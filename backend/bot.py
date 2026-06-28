import os
import logging
import sqlite3
import hashlib
import secrets
import threading
import requests
import json
from datetime import datetime, timedelta
from collections import defaultdict
from dotenv import load_dotenv
from flask import Flask, request as flask_request, jsonify
from telegram import Update, ReplyKeyboardMarkup, KeyboardButton
from telegram.ext import (
    Application, CommandHandler, MessageHandler,
    filters, ContextTypes, ConversationHandler
)

# ── إعدادات ──
load_dotenv("server.env")
BOT_TOKEN  = os.getenv("BOT_TOKEN", "ضع هنا توكن البوت")
ADMIN_ID   = int(os.getenv("CHAT_ID", "0"))
API_PORT   = int(os.getenv("API_PORT", "5000"))
DB         = os.getenv("DATABASE_PATH", "bot_data.db")
LOG_PATH   = os.getenv("LOG_PATH", "./logs/")

# إنشاء مجلد السجلات
os.makedirs(LOG_PATH, exist_ok=True)

# ── Logging Setup ──
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler(os.path.join(LOG_PATH, 'bot.log')),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)

# Rate Limiting
rate_limit = defaultdict(lambda: {"count": 0, "time": datetime.now()})
RATE_LIMIT_REQUESTS = 10
RATE_LIMIT_SECONDS = 60

# ════════════════════════════════
#  Rate Limiting
# ════════════════════════════════
def check_rate_limit(key: str) -> bool:
    """التحقق من Rate Limit"""
    now = datetime.now()
    limit_info = rate_limit[key]
    
    if (now - limit_info["time"]).total_seconds() > RATE_LIMIT_SECONDS:
        rate_limit[key] = {"count": 1, "time": now}
        return True
    
    if limit_info["count"] >= RATE_LIMIT_REQUESTS:
        return False
    
    rate_limit[key]["count"] += 1
    return True

# ════════════════════════════════
#  قاعدة البيانات
# ════════════════════════════════
def get_db():
    conn = sqlite3.connect(DB, check_same_thread=False)
    conn.row_factory = sqlite3.Row
    return conn

def init_db():
    try:
        with get_db() as db:
            # جدول المستخدمين
            db.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id       INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT UNIQUE NOT NULL,
                    password TEXT NOT NULL,
                    balance  INTEGER DEFAULT 0,
                    token    TEXT,
                    blocked  INTEGER DEFAULT 0,
                    role     TEXT DEFAULT 'user',
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """)
            
            # جدول سجل الشحن
            db.execute("""
                CREATE TABLE IF NOT EXISTS recharge_log (
                    id         INTEGER PRIMARY KEY AUTOINCREMENT,
                    auth_id    TEXT,
                    user_id    INTEGER,
                    product_id TEXT,
                    receiver   TEXT,
                    pin        TEXT,
                    success    INTEGER DEFAULT 0,
                    message    TEXT,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """)
            
            # جدول الإشعارات
            db.execute("""
                CREATE TABLE IF NOT EXISTS notifications (
                    id         INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id    INTEGER,
                    title      TEXT,
                    body       TEXT,
                    read       INTEGER DEFAULT 0,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """)
            
            # جدول السجلات (Audit Log)
            db.execute("""
                CREATE TABLE IF NOT EXISTS audit_log (
                    id         INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id    INTEGER,
                    action     TEXT,
                    details    TEXT,
                    ip_address TEXT,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """)

            db.commit()

            # إضافة الأعمدة الناقصة
            try:
                db.execute("ALTER TABLE users ADD COLUMN blocked INTEGER DEFAULT 0")
                db.execute("ALTER TABLE users ADD COLUMN role TEXT DEFAULT 'user'")
                db.execute("ALTER TABLE users ADD COLUMN created_at DATETIME DEFAULT CURRENT_TIMESTAMP")
                db.commit()
            except:
                pass

            # أدمن افتراضي
            admin_user = os.getenv("ADMIN_USERNAME", "admin")
            admin_pass = os.getenv("ADMIN_PASSWORD", "admin123")
            cur = db.execute("SELECT id FROM users WHERE username=?", (admin_user,))
            if not cur.fetchone():
                hashed = hashlib.sha256(admin_pass.encode()).hexdigest()
                db.execute(
                    "INSERT INTO users (username,password,balance,role) VALUES (?,?,?,?)",
                    (admin_user, hashed, 999, 'admin')
                )
                db.commit()
                logger.info(f"✅ أدمن: {admin_user} / {admin_pass}")
    except Exception as e:
        logger.error(f"❌ Database Error: {str(e)}")

def hash_pass(p):
    return hashlib.sha256(p.encode()).hexdigest()

def get_user_by_token(token):
    try:
        with get_db() as db:
            return db.execute("SELECT * FROM users WHERE token=?", (token,)).fetchone()
    except Exception as e:
        logger.error(f"❌ Get user error: {str(e)}")
        return None

def get_user_by_creds(username, password):
    try:
        with get_db() as db:
            return db.execute(
                "SELECT * FROM users WHERE username=? AND password=?",
                (username, hash_pass(password))
            ).fetchone()
    except Exception as e:
        logger.error(f"❌ Credentials error: {str(e)}")
        return None

def is_admin_user(user):
    if not user:
        return False
    admin_user = os.getenv("ADMIN_USERNAME", "admin")
    return user["username"] == admin_user or user["role"] == "admin"

def add_notification(db, user_id, title, body):
    try:
        db.execute(
            "INSERT INTO notifications (user_id, title, body) VALUES (?,?,?)",
            (user_id, title, body)
        )
    except Exception as e:
        logger.error(f"❌ Notification error: {str(e)}")

def log_action(user_id, action, details, ip_address=""):
    try:
        with get_db() as db:
            db.execute(
                "INSERT INTO audit_log (user_id, action, details, ip_address) VALUES (?,?,?,?)",
                (user_id, action, details, ip_address)
            )
            db.commit()
    except Exception as e:
        logger.error(f"❌ Audit log error: {str(e)}")

# ════════════════════════════════
#  Flask API
# ════════════════════════════════
api = Flask(__name__)

def require_token():
    auth = flask_request.headers.get("Authorization", "")
    if not auth.startswith("Bearer "):
        return None
    return get_user_by_token(auth.replace("Bearer ", "").strip())

# ── Error Handler ──
@api.errorhandler(429)
def ratelimit_handler(e):
    return jsonify({"success": False, "message": "❌ عدد محاولات كثير. انتظر قليلاً"}), 429

@api.errorhandler(500)
def internal_error(e):
    logger.error(f"❌ Internal error: {str(e)}")
    return jsonify({"success": False, "message": "❌ خطأ في السيرفر"}), 500

# ── Auth ──
@api.route("/api/auth/login", methods=["POST"])
def api_login():
    if not check_rate_limit(flask_request.remote_addr):
        logger.warning(f"⚠️ Rate limit exceeded from {flask_request.remote_addr}")
        return jsonify({"success": False, "message": "❌ محاولات كثيرة"}), 429
    
    try:
        data = flask_request.json or {}
        user = get_user_by_creds(data.get("username",""), data.get("password",""))
        
        if not user:
            logger.warning(f"❌ Failed login: {data.get('username','')}")
            return jsonify({"success": False, "message": "❌ يوزر أو باسورد غلط"})
        
        token = secrets.token_hex(32)
        with get_db() as db:
            db.execute("UPDATE users SET token=? WHERE id=?", (token, user["id"]))
            db.commit()
            log_action(user["id"], "LOGIN", f"Login success", flask_request.remote_addr)
        
        logger.info(f"✅ Login: {user['username']}")
        return jsonify({
            "success": True,
            "token": token,
            "user": {
                "username": user["username"],
                "balance": user["balance"],
                "blocked": bool(user["blocked"]),
                "role": user["role"] if user["role"] else "user"
            }
        })
    except Exception as e:
        logger.error(f"❌ Login error: {str(e)}")
        return jsonify({"success": False, "message": "❌ خطأ في تسجيل الدخول"})

# ── User ──
@api.route("/api/user/balance", methods=["GET"])
def api_balance():
    if not check_rate_limit(flask_request.remote_addr):
        return jsonify({"success": False, "message": "❌ محاولات كثيرة"}), 429
    
    try:
        user = require_token()
        if not user:
            return jsonify({"success": False, "message": "❌ غير مصرح"}), 401
        
        return jsonify({
            "success": True,
            "balance": user["balance"],
            "blocked": bool(user["blocked"])
        })
    except Exception as e:
        logger.error(f"❌ Balance error: {str(e)}")
        return jsonify({"success": False, "message": "❌ خطأ في جلب الرصيد"})

@api.route("/api/user/profile", methods=["GET"])
def api_profile():
    if not check_rate_limit(flask_request.remote_addr):
        return jsonify({"success": False, "message": "❌ محاولات كثيرة"}), 429
    
    try:
        user = require_token()
        if not user:
            return jsonify({"success": False, "message": "❌ غير مصرح"}), 401
        
        with get_db() as db:
            total = db.execute(
                "SELECT COUNT(*) as c FROM recharge_log WHERE user_id=?", (user["id"],)
            ).fetchone()["c"]
            success = db.execute(
                "SELECT COUNT(*) as c FROM recharge_log WHERE user_id=? AND success=1", (user["id"],)
            ).fetchone()["c"]
        
        return jsonify({
            "success": True,
            "username": user["username"],
            "balance": user["balance"],
            "total_recharges": total,
            "success_recharges": success,
            "blocked": bool(user["blocked"])
        })
    except Exception as e:
        logger.error(f"❌ Profile error: {str(e)}")
        return jsonify({"success": False, "message": "❌ خطأ في جلب البيانات"})

@api.route("/api/user/history", methods=["GET"])
def api_history():
    if not check_rate_limit(flask_request.remote_addr):
        return jsonify({"success": False, "message": "❌ محاولات كثيرة"}), 429
    
    try:
        user = require_token()
        if not user:
            return jsonify({"success": False, "message": "❌ غير مصرح"}), 401
        
        with get_db() as db:
            rows = db.execute(
                "SELECT * FROM recharge_log WHERE user_id=? ORDER BY created_at DESC LIMIT 50",
                (user["id"],)
            ).fetchall()
        
        history = [dict(r) for r in rows]
        return jsonify({"success": True, "history": history})
    except Exception as e:
        logger.error(f"❌ History error: {str(e)}")
        return jsonify({"success": False, "message": "❌ خطأ في جلب السجل"})

@api.route("/api/user/notifications", methods=["GET"])
def api_notifications():
    if not check_rate_limit(flask_request.remote_addr):
        return jsonify({"success": False, "message": "❌ محاولات كثيرة"}), 429
    
    try:
        user = require_token()
        if not user:
            return jsonify({"success": False, "message": "❌ غير مصرح"}), 401
        
        with get_db() as db:
            rows = db.execute(
                "SELECT * FROM notifications WHERE user_id=? ORDER BY created_at DESC LIMIT 20",
                (user["id"],)
            ).fetchall()
        
        return jsonify({"success": True, "notifications": [dict(r) for r in rows]})
    except Exception as e:
        logger.error(f"❌ Notifications error: {str(e)}")
        return jsonify({"success": False, "message": "❌ خطأ في جلب الإشعارات"})

# ── Recharge ──
@api.route("/api/recharge/authorize", methods=["POST"])
def api_authorize():
    if not check_rate_limit(flask_request.remote_addr):
        return jsonify({"success": False, "message": "❌ محاولات كثيرة"}), 429
    
    try:
        user = require_token()
        if not user:
            return jsonify({"success": False, "message": "❌ غير مصرح"}), 401
        
        if user["blocked"]:
            logger.warning(f"⚠️ Blocked user tried recharge: {user['username']}")
            return jsonify({"success": False, "message": "🚫 حسابك محظور"})
        
        if user["balance"] <= 0:
            return jsonify({"success": False, "message": "❌ رصيدك مش كافي"})
        
        data    = flask_request.json or {}
        new_bal = user["balance"] - 1
        auth_id = secrets.token_hex(16)
        
        with get_db() as db:
            db.execute("UPDATE users SET balance=? WHERE id=?", (new_bal, user["id"]))
            db.execute(
                "INSERT INTO recharge_log (auth_id,user_id,product_id,receiver,pin) VALUES (?,?,?,?,?)",
                (auth_id, user["id"], data.get("product_id",""), data.get("receiver",""), data.get("pin",""))
            )
            log_action(user["id"], "RECHARGE_AUTHORIZE", f"Product: {data.get('product_id')}", flask_request.remote_addr)
            db.commit()
        
        logger.info(f"✅ Recharge auth: {user['username']} -> {data.get('receiver')}")
        return jsonify({"success": True, "auth_id": auth_id, "new_balance": new_bal})
    except Exception as e:
        logger.error(f"❌ Authorize error: {str(e)}")
        return jsonify({"success": False, "message": "❌ خطأ في التفويض"})

@api.route("/api/recharge/report", methods=["POST"])
def api_report():
    if not check_rate_limit(flask_request.remote_addr):
        return jsonify({"success": False, "message": "❌ محاولات كثيرة"}), 429
    
    try:
        user = require_token()
        if not user:
            return jsonify({"success": False}), 401
        
        data    = flask_request.json or {}
        auth_id = data.get("auth_id","")
        success = data.get("success", False)
        
        with get_db() as db:
            db.execute(
                "UPDATE recharge_log SET success=?,message=? WHERE auth_id=?",
                (1 if success else 0, data.get("message",""), auth_id)
            )
            if not success:
                row = db.execute(
                    "SELECT user_id FROM recharge_log WHERE auth_id=?", (auth_id,)
                ).fetchone()
                if row:
                    db.execute("UPDATE users SET balance=balance+1 WHERE id=?", (row["user_id"],))
            
            row = db.execute("SELECT * FROM recharge_log WHERE auth_id=?", (auth_id,)).fetchone()
            if row:
                title = "✅ تم الشحن بنجاح" if success else "❌ فشل الشحن"
                body  = f"المستلم: {row['receiver']}" if row['receiver'] else ""
                add_notification(db, row["user_id"], title, body)
            
            db.commit()

        # إشعار الأدمن على التيليجرام
        try:
            if row:
                status_text = "✅ نجح" if success else "❌ فشل"
                msg = (f"📱 شحن جديد من التطبيق\n"
                       f"👤 المستخدم: {row['user_id']}\n"
                       f"📦 المنتج: {row['product_id']}\n"
                       f"📱 المستلم: {row['receiver']}\n"
                       f"🔰 الحالة: {status_text}")
                requests.post(
                    f"https://api.telegram.org/bot{BOT_TOKEN}/sendMessage",
                    json={"chat_id": ADMIN_ID, "text": msg},
                    timeout=5
                )
        except:
            pass

        logger.info(f"✅ Recharge reported: {auth_id} -> {status_text}")
        return jsonify({"success": True})
    except Exception as e:
        logger.error(f"❌ Report error: {str(e)}")
        return jsonify({"success": False, "message": "❌ خطأ في الإبلاغ"})

# ── Admin ──
@api.route("/api/admin/users", methods=["GET"])
def api_get_users():
    if not check_rate_limit(flask_request.remote_addr):
        return jsonify({"success": False, "message": "❌ محاولات كثيرة"}), 429
    
    try:
        user = require_token()
        if not user or not is_admin_user(user):
            logger.warning(f"⚠️ Unauthorized admin access: {flask_request.remote_addr}")
            return jsonify({"success": False, "message": "❌ مش أدمن"})
        
        with get_db() as db:
            rows = db.execute("SELECT id,username,balance,blocked FROM users").fetchall()
        
        return jsonify({"success": True, "users": [dict(r) for r in rows]})
    except Exception as e:
        logger.error(f"❌ Get users error: {str(e)}")
        return jsonify({"success": False, "message": "❌ خطأ في جلب المستخدمين"})

@api.route("/api/admin/users/create", methods=["POST"])
def api_create_user():
    if not check_rate_limit(flask_request.remote_addr):
        return jsonify({"success": False, "message": "❌ محاولات كثيرة"}), 429
    
    try:
        user = require_token()
        if not user or not is_admin_user(user):
            return jsonify({"success": False, "message": "❌ مش أدمن"})
        
        data = flask_request.json or {}
        with get_db() as db:
            db.execute(
                "INSERT INTO users (username,password,balance) VALUES (?,?,?)",
                (data["username"], hash_pass(data["password"]), int(data.get("balance",0)))
            )
            db.commit()
            log_action(user["id"], "CREATE_USER", f"User: {data['username']}", flask_request.remote_addr)
        
        logger.info(f"✅ User created: {data['username']}")
        return jsonify({"success": True})
    except Exception as e:
        logger.error(f"❌ Create user error: {str(e)}")
        return jsonify({"success": False, "message": "❌ اليوزر موجود أو خطأ ما"})

@api.route("/api/admin/users/balance", methods=["POST"])
def api_edit_balance():
    if not check_rate_limit(flask_request.remote_addr):
        return jsonify({"success": False, "message": "❌ محاولات كثيرة"}), 429
    
    try:
        user = require_token()
        if not user or not is_admin_user(user):
            return jsonify({"success": False, "message": "❌ مش أدمن"})
        
        data = flask_request.json or {}
        with get_db() as db:
            target = db.execute("SELECT id FROM users WHERE username=?", (data.get("username",""),)).fetchone()
            db.execute(
                "UPDATE users SET balance=? WHERE username=?",
                (int(data.get("balance",0)), data.get("username",""))
            )
            
            if target:
                add_notification(
                    db, target["id"],
                    "💰 تم تعديل رصيدك",
                    f"رصيدك الجديد: {data.get('balance',0)} وحدة"
                )
            
            log_action(user["id"], "EDIT_BALANCE", f"User: {data.get('username')} -> {data.get('balance')}", flask_request.remote_addr)
            db.commit()
        
        logger.info(f"✅ Balance edited: {data.get('username')}")
        return jsonify({"success": True})
    except Exception as e:
        logger.error(f"❌ Edit balance error: {str(e)}")
        return jsonify({"success": False, "message": "❌ خطأ في التعديل"})

@api.route("/api/admin/users/delete", methods=["POST"])
def api_delete_user():
    if not check_rate_limit(flask_request.remote_addr):
        return jsonify({"success": False, "message": "❌ محاولات كثيرة"}), 429
    
    try:
        user = require_token()
        if not user or not is_admin_user(user):
            return jsonify({"success": False, "message": "❌ مش أدمن"})
        
        data = flask_request.json or {}
        with get_db() as db:
            cur = db.execute("DELETE FROM users WHERE username=?", (data.get("username",""),))
            log_action(user["id"], "DELETE_USER", f"User: {data.get('username')}", flask_request.remote_addr)
            db.commit()
        
        logger.info(f"✅ User deleted: {data.get('username')}")
        return jsonify({"success": cur.rowcount > 0})
    except Exception as e:
        logger.error(f"❌ Delete user error: {str(e)}")
        return jsonify({"success": False, "message": "❌ خطأ في الحذف"})

@api.route("/api/admin/users/block", methods=["POST"])
def api_block_user():
    if not check_rate_limit(flask_request.remote_addr):
        return jsonify({"success": False, "message": "❌ محاولات كثيرة"}), 429
    
    try:
        user = require_token()
        if not user or not is_admin_user(user):
            return jsonify({"success": False, "message": "❌ مش أدمن"})
        
        data    = flask_request.json or {}
        blocked = 1 if data.get("blocked", True) else 0
        
        with get_db() as db:
            target = db.execute("SELECT id FROM users WHERE username=?", (data.get("username",""),)).fetchone()
            db.execute(
                "UPDATE users SET blocked=? WHERE username=?",
                (blocked, data.get("username",""))
            )
            
            if target:
                status = "🚫 تم حظر حسابك" if blocked else "✅ تم رفع الحظر عن حسابك"
                add_notification(db, target["id"], status, "تواصل مع الإدارة")
            
            log_action(user["id"], "BLOCK_USER", f"User: {data.get('username')} -> {'blocked' if blocked else 'unblocked'}", flask_request.remote_addr)
            db.commit()
        
        status_text = "blocked" if blocked else "unblocked"
        logger.info(f"✅ User {status_text}: {data.get('username')}")
        return jsonify({"success": True})
    except Exception as e:
        logger.error(f"❌ Block user error: {str(e)}")
        return jsonify({"success": False, "message": "❌ خطأ في الحظر"})

# ────────────────────────────────
# Health Check
# ────────────────────────────────
@api.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "✅ OK", "timestamp": datetime.now().isoformat()})

# ════════════════════════════════
#  تشغيل API
# ════════════════════════════════
def run_api():
    logger.info(f"🚀 Starting API on port {API_PORT}...")
    api.run(host="0.0.0.0", port=API_PORT, debug=False, use_reloader=False)

# ════════════════════════════════
#  Telegram Bot - States
# ════════════════════════════════
(WAIT_LOGIN_USER, WAIT_LOGIN_PASS, MAIN_MENU,
 WAIT_RECEIVER, WAIT_PIN,
 WAIT_ADD_USER, WAIT_ADD_PASS, WAIT_ADD_BALANCE,
 WAIT_EDIT_USER, WAIT_EDIT_BALANCE,
 WAIT_DELETE_USER) = range(11)

user_sessions: dict = {}
user_pending:  dict = {}

def bot_api_post(path, data, token=None):
    headers = {"Authorization": f"Bearer {token}"} if token else {}
    try:
        r = requests.post(f"http://127.0.0.1:{API_PORT}{path}", json=data, headers=headers, timeout=10)
        return r.json()
    except Exception as e:
        logger.error(f"❌ API request error: {str(e)}")
        return {"success": False, "message": str(e)}

def bot_api_get(path, token=None):
    headers = {"Authorization": f"Bearer {token}"} if token else {}
    try:
        r = requests.get(f"http://127.0.0.1:{API_PORT}{path}", headers=headers, timeout=10)
        return r.json()
    except Exception as e:
        logger.error(f"❌ API request error: {str(e)}")
        return {"success": False, "message": str(e)}

def get_token(chat_id):
    return user_sessions.get(chat_id, {}).get("token", "")

def is_admin(chat_id):
    return chat_id == ADMIN_ID or user_sessions.get(chat_id, {}).get("role") == "admin"

def main_kb(chat_id):
    buttons = [
        [KeyboardButton("⚡ شحن"), KeyboardButton("💰 رصيدي")],
        [KeyboardButton("🔄 تحديث الرصيد"), KeyboardButton("📋 سجلي")],
        [KeyboardButton("🚪 تسجيل الخروج")]
    ]
    if is_admin(chat_id):
        buttons.insert(2, [KeyboardButton("👑 لوحة الأدمن")])
    return ReplyKeyboardMarkup(buttons, resize_keyboard=True)

def admin_kb():
    return ReplyKeyboardMarkup([
        [KeyboardButton("➕ إضافة مستخدم"), KeyboardButton("✏️ تعديل رصيد")],
        [KeyboardButton("🗑 حذف مستخدم"), KeyboardButton("📋 كل المستخدمين")],
        [KeyboardButton("🔙 رجوع")]
    ], resize_keyboard=True)

def cancel_kb():
    return ReplyKeyboardMarkup([[KeyboardButton("❌ إلغاء")]], resize_keyboard=True)

async def start(update: Update, ctx: ContextTypes.DEFAULT_TYPE):
    chat_id = update.effective_chat.id
    logger.info(f"👤 User started: {chat_id}")
    
    if chat_id in user_sessions:
        await update.message.reply_text(
            f"👋 مرحباً {user_sessions[chat_id]['username']}!", reply_markup=main_kb(chat_id))
        return MAIN_MENU
    await update.message.reply_text("👤 اسم المستخدم:", reply_markup=cancel_kb())
    return WAIT_LOGIN_USER

async def get_login_user(update: Update, ctx: ContextTypes.DEFAULT_TYPE):
    chat_id = update.effective_chat.id
    text    = update.message.text.strip()
    if text == "❌ إلغاء":
        await update.message.reply_text("❌ إلغاء")
        return WAIT_LOGIN_USER
    user_pending[chat_id] = {"username": text}
    await update.message.reply_text("🔑 كلمة السر:", reply_markup=cancel_kb())
    return WAIT_LOGIN_PASS

async def get_login_pass(update: Update, ctx: ContextTypes.DEFAULT_TYPE):
    chat_id = update.effective_chat.id
    text    = update.message.text.strip()
    if text == "❌ إلغاء":
        await update.message.reply_text("❌ إلغاء")
        return WAIT_LOGIN_USER
    username = user_pending.get(chat_id, {}).get("username","")
    res = bot_api_post("/api/auth/login", {"username": username, "password": text})
    if res.get("success"):
        u = res.get("user", {})
        if u.get("blocked"):
            logger.warning(f"⚠️ Blocked user login attempt: {username}")
            await update.message.reply_text("🚫 حسابك محظور!")
            return WAIT_LOGIN_USER
        user_sessions[chat_id] = {
            "username": u.get("username", username),
            "balance":  u.get("balance", 0),
            "token":    res.get("token",""),
            "role":     u.get("role","user")
        }
        logger.info(f"✅ User logged in: {username}")
        await update.message.reply_text(
            f"✅ مرحباً {username}!\n💰 رصيدك: {u.get('balance',0)}",
            reply_markup=main_kb(chat_id))
        return MAIN_MENU
    else:
        await update.message.reply_text(
            f"❌ {res.get('message','يوزر أو باسورد غلط')}", reply_markup=cancel_kb())
        return WAIT_LOGIN_PASS

async def main_menu(update: Update, ctx: ContextTypes.DEFAULT_TYPE):
    chat_id = update.effective_chat.id
    text    = update.message.text.strip()
    if chat_id not in user_sessions:
        await update.message.reply_text("👤 اسم المستخدم:", reply_markup=cancel_kb())
        return WAIT_LOGIN_USER

    if text == "⚡ شحن":
        await update.message.reply_text("📱 ادخل رقم المستلم:", reply_markup=cancel_kb())
        return WAIT_RECEIVER
    elif text == "💰 رصيدي":
        s = user_sessions[chat_id]
        await update.message.reply_text(
            f"👤 {s['username']}\n💰 رصيدك: {s['balance']}", reply_markup=main_kb(chat_id))
    elif text == "🔄 تحديث الرصيد":
        res = bot_api_get("/api/user/balance", token=get_token(chat_id))
        if res.get("success"):
            if res.get("blocked"):
                logger.warning(f"⚠️ Blocked user attempted action: {user_sessions[chat_id]['username']}")
                await update.message.reply_text("🚫 حسابك محظور!")
                user_sessions.pop(chat_id, None)
                return WAIT_LOGIN_USER
            user_sessions[chat_id]["balance"] = res["balance"]
            await update.message.reply_text(f"✅ رصيدك: {res['balance']}", reply_markup=main_kb(chat_id))
    elif text == "📋 سجلي":
        res = bot_api_get("/api/user/history", token=get_token(chat_id))
        if res.get("success"):
            history = res.get("history", [])
            if not history:
                msg = "لا يوجد سجل بعد"
            else:
                lines = []
                for h in history[:10]:
                    icon = "✅" if h["success"] else "❌"
                    lines.append(f"{icon} {h['product_id']} → {h['receiver']}\n📅 {h['created_at']}")
                msg = "📋 آخر 10 عمليات:\n\n" + "\n\n".join(lines)
            await update.message.reply_text(msg, reply_markup=main_kb(chat_id))
    elif text == "🚪 تسجيل الخروج":
        user_sessions.pop(chat_id, None)
        logger.info(f"👋 User logged out: {chat_id}")
        await update.message.reply_text("👋 تم الخروج", reply_markup=cancel_kb())
        return WAIT_LOGIN_USER
    elif text == "👑 لوحة الأدمن" and is_admin(chat_id):
        await update.message.reply_text("👑 لوحة الأدمن:", reply_markup=admin_kb())
    elif text == "➕ إضافة مستخدم" and is_admin(chat_id):
        await update.message.reply_text("👤 اسم المستخدم الجديد:", reply_markup=cancel_kb())
        return WAIT_ADD_USER
    elif text == "✏️ تعديل رصيد" and is_admin(chat_id):
        await update.message.reply_text("👤 اسم المستخدم:", reply_markup=cancel_kb())
        return WAIT_EDIT_USER
    elif text == "🗑 حذف مستخدم" and is_admin(chat_id):
        await update.message.reply_text("👤 اسم المستخدم اللي هتحذفه:", reply_markup=cancel_kb())
        return WAIT_DELETE_USER
    elif text == "📋 كل المستخدمين" and is_admin(chat_id):
        res = bot_api_get("/api/admin/users", token=get_token(chat_id))
        if res.get("success"):
            users = res.get("users", [])
            if not users:
                msg = "مفيش مستخدمين"
            else:
                lines = []
                for u in users:
                    status = "🚫" if u.get("blocked") else "✅"
                    lines.append(f"{status} {u['username']} | 💰 {u['balance']}")
                msg = "📋 المستخدمين:\n\n" + "\n".join(lines)
            await update.message.reply_text(msg, reply_markup=admin_kb())
    elif text == "🔙 رجوع":
        await update.message.reply_text("🏠 القائمة:", reply_markup=main_kb(chat_id))
    elif text == "❌ إلغاء":
        await update.message.reply_text("❌ إلغاء", reply_markup=main_kb(chat_id))
    return MAIN_MENU

async def get_receiver(update: Update, ctx: ContextTypes.DEFAULT_TYPE):
    chat_id  = update.effective_chat.id
    receiver = update.message.text.strip()
    if receiver == "❌ إلغاء":
        await update.message.reply_text("❌ إلغاء", reply_markup=main_kb(chat_id))
        return MAIN_MENU
    if not receiver.startswith("01") or len(receiver) != 11 or not receiver.isdigit():
        await update.message.reply_text("❌ رقم غير صحيح (01XXXXXXXXX):", reply_markup=cancel_kb())
        return WAIT_RECEIVER
    user_pending[chat_id] = {"receiver": receiver}
    await update.message.reply_text("🔐 الرقم السري فودافون كاش (6 أرقام):", reply_markup=cancel_kb())
    return WAIT_PIN

async def get_pin(update: Update, ctx: ContextTypes.DEFAULT_TYPE):
    chat_id  = update.effective_chat.id
    pin      = update.message.text.strip()
    if pin == "❌ إلغاء":
        await update.message.reply_text("❌ إلغاء", reply_markup=main_kb(chat_id))
        return MAIN_MENU
    if not pin.isdigit() or len(pin) != 6:
        await update.message.reply_text("❌ الرقم السري 6 أرقام:", reply_markup=cancel_kb())
        return WAIT_PIN
    receiver = user_pending.get(chat_id, {}).get("receiver", "")
    token    = get_token(chat_id)
    await update.message.reply_text("⏳ جاري التحقق من الرصيد...")
    auth = bot_api_post(
        "/api/recharge/authorize",
        {"product_id": "Fakka_10_Unite", "receiver": receiver, "pin": pin},
        token=token
    )
    if not auth.get("success"):
        await update.message.reply_text(
            f"❌ {auth.get('message','رفض السيرفر')}", reply_markup=main_kb(chat_id))
        return MAIN_MENU
    auth_id     = auth.get("auth_id", "")
    new_balance = auth.get("new_balance", 0)
    user_sessions[chat_id]["balance"] = new_balance
    bot_api_post("/api/recharge/report", {"auth_id": auth_id, "success": True, "message": "✅ تم من البوت"}, token=token)
    logger.info(f"✅ Recharge from bot: {user_sessions[chat_id]['username']} -> {receiver}")
    await update.message.reply_text(
        f"✅ تم إرسال طلب الشحن!\n📱 المستلم: {receiver}\n💰 رصيدك الجديد: {new_balance}",
        reply_markup=main_kb(chat_id))
    return MAIN_MENU

async def get_add_user(update: Update, ctx: ContextTypes.DEFAULT_TYPE):
    chat_id = update.effective_chat.id
    text    = update.message.text.strip()
    if text == "❌ إلغاء":
        await update.message.reply_text("❌", reply_markup=admin_kb())
        return MAIN_MENU
    user_pending[chat_id] = {"new_username": text}
    await update.message.reply_text("🔑 كلمة السر:", reply_markup=cancel_kb())
    return WAIT_ADD_PASS

async def get_add_pass(update: Update, ctx: ContextTypes.DEFAULT_TYPE):
    chat_id = update.effective_chat.id
    text    = update.message.text.strip()
    if text == "❌ إلغاء":
        await update.message.reply_text("❌", reply_markup=admin_kb())
        return MAIN_MENU
    user_pending[chat_id]["new_password"] = text
    await update.message.reply_text("💰 الرصيد الابتدائي:", reply_markup=cancel_kb())
    return WAIT_ADD_BALANCE

async def get_add_balance(update: Update, ctx: ContextTypes.DEFAULT_TYPE):
    chat_id = update.effective_chat.id
    text    = update.message.text.strip()
    if text == "❌ إلغاء":
        await update.message.reply_text("❌", reply_markup=admin_kb())
        return MAIN_MENU
    if not text.isdigit():
        await update.message.reply_text("❌ رقم صحيح فقط:", reply_markup=cancel_kb())
        return WAIT_ADD_BALANCE
    p   = user_pending.get(chat_id, {})
    res = bot_api_post(
        "/api/admin/users/create",
        {"username": p["new_username"], "password": p["new_password"], "balance": int(text)},
        token=get_token(chat_id)
    )
    msg = f"✅ تم إضافة {p['new_username']} برصيد {text}" if res.get("success") else f"❌ {res.get('message')}"
    logger.info(f"✅ Admin created user: {p['new_username']}")
    await update.message.reply_text(msg, reply_markup=admin_kb())
    return MAIN_MENU

async def get_edit_user(update: Update, ctx: ContextTypes.DEFAULT_TYPE):
    chat_id = update.effective_chat.id
    text    = update.message.text.strip()
    if text == "❌ إلغاء":
        await update.message.reply_text("❌", reply_markup=admin_kb())
        return MAIN_MENU
    user_pending[chat_id] = {"edit_username": text}
    await update.message.reply_text("💰 الرصيد الجديد:", reply_markup=cancel_kb())
    return WAIT_EDIT_BALANCE

async def get_edit_balance(update: Update, ctx: ContextTypes.DEFAULT_TYPE):
    chat_id = update.effective_chat.id
    text    = update.message.text.strip()
    if text == "❌ إلغاء":
        await update.message.reply_text("❌", reply_markup=admin_kb())
        return MAIN_MENU
    if not text.isdigit():
        await update.message.reply_text("❌ رقم صحيح:", reply_markup=cancel_kb())
        return WAIT_EDIT_BALANCE
    username = user_pending.get(chat_id, {}).get("edit_username", "")
    res = bot_api_post(
        "/api/admin/users/balance",
        {"username": username, "balance": int(text)},
        token=get_token(chat_id)
    )
    msg = f"✅ تم تعديل رصيد {username} إلى {text}" if res.get("success") else f"❌ {res.get('message')}"
    logger.info(f"✅ Admin edited balance: {username} -> {text}")
    await update.message.reply_text(msg, reply_markup=admin_kb())
    return MAIN_MENU

async def get_delete_user(update: Update, ctx: ContextTypes.DEFAULT_TYPE):
    chat_id  = update.effective_chat.id
    username = update.message.text.strip()
    if username == "❌ إلغاء":
        await update.message.reply_text("❌", reply_markup=admin_kb())
        return MAIN_MENU
    res = bot_api_post("/api/admin/users/delete", {"username": username}, token=get_token(chat_id))
    msg = f"✅ تم حذف {username}" if res.get("success") else f"❌ {res.get('message')}"
    logger.info(f"✅ Admin deleted user: {username}")
    await update.message.reply_text(msg, reply_markup=admin_kb())
    return MAIN_MENU

# ════════════════════════════════
#  Main
# ════════════════════════════════
def main():
    logger.info("🚀 Starting Cobra Bot System...")
    init_db()

    t = threading.Thread(target=run_api, daemon=True)
    t.start()

    logger.info(f"✅ API started on port {API_PORT}")

    app = Application.builder().token(BOT_TOKEN).build()
    conv = ConversationHandler(
        entry_points=[CommandHandler("start", start)],
        states={
            WAIT_LOGIN_USER:   [MessageHandler(filters.TEXT & ~filters.COMMAND, get_login_user)],
            WAIT_LOGIN_PASS:   [MessageHandler(filters.TEXT & ~filters.COMMAND, get_login_pass)],
            MAIN_MENU:         [MessageHandler(filters.TEXT & ~filters.COMMAND, main_menu)],
            WAIT_RECEIVER:     [MessageHandler(filters.TEXT & ~filters.COMMAND, get_receiver)],
            WAIT_PIN:          [MessageHandler(filters.TEXT & ~filters.COMMAND, get_pin)],
            WAIT_ADD_USER:     [MessageHandler(filters.TEXT & ~filters.COMMAND, get_add_user)],
            WAIT_ADD_PASS:     [MessageHandler(filters.TEXT & ~filters.COMMAND, get_add_pass)],
            WAIT_ADD_BALANCE:  [MessageHandler(filters.TEXT & ~filters.COMMAND, get_add_balance)],
            WAIT_EDIT_USER:    [MessageHandler(filters.TEXT & ~filters.COMMAND, get_edit_user)],
            WAIT_EDIT_BALANCE: [MessageHandler(filters.TEXT & ~filters.COMMAND, get_edit_balance)],
            WAIT_DELETE_USER:  [MessageHandler(filters.TEXT & ~filters.COMMAND, get_delete_user)],
        },
        fallbacks=[CommandHandler("start", start)],
    )
    app.add_handler(conv)
    logger.info(f"✅ Bot started! API on port {API_PORT}")
    app.run_polling()

if __name__ == "__main__":
    main()
