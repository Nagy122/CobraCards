# 🐍 COBRA CARDS - Complete System

**تطبيق أندرويد + بوت تيليجرام كامل مع حماية عالية جداً**

## ⚡ البدء السريع (5 دقائق)

اقرأ: **[QUICKSTART.md](QUICKSTART.md)**

---

## 📚 التوثيق الكامل

| الملف | الموضوع |
|-------|---------|
| **[QUICKSTART.md](QUICKSTART.md)** | البدء السريع |
| **[DEPLOYMENT.md](DEPLOYMENT.md)** | التشغيل الكامل |
| **[SECURITY.md](SECURITY.md)** | الحماية والأمان |
| **[TROUBLESHOOTING.md](TROUBLESHOOTING.md)** | استكشاف الأخطاء |

---

## 🎯 المزايا

### 📱 التطبيق
- ✅ تسجيل دخول آمن
- ✅ شحن فودافون كاش
- ✅ سجل العمليات
- ✅ الملف الشخصي
- ✅ لوحة إدارة (للأدمن)
- ✅ مزامنة فورية مع البوت

### 🤖 البوت
- ✅ إدارة المستخدمين
- ✅ تعديل الأرصدة
- ✅ حظر/رفع الحظر
- ✅ عرض السجلات
- ✅ مزامنة فورية مع التطبيق
- ✅ إشعارات تلقائية

### 🔒 الحماية
- ✅ منع اسكرين شوت
- ✅ كشف Rooting
- ✅ كشف Debugger
- ✅ ProGuard Obfuscation
- ✅ JWT Tokens
- ✅ Rate Limiting
- ✅ Audit Logging
- ✅ Encrypted Data

---

## 🏗️ المعمارية

```
┌─────────────┐
│   Android   │  ← التطبيق
│  App        │
└──────┬──────┘
       │ HTTP/HTTPS
       ▼
┌─────────────────────┐
│  Flask API          │  ← الـ API
│  (5000)             │
└──────┬──────────────┘
       │ SQLite3
       ▼
┌─────────────────────┐
│  Database           │  ← قاعدة البيانات
│  (bot_data.db)      │
└─────────────────────┘

┌─────────────┐
│ Telegram    │  ← البوت
│ Bot         │
└──────┬──────┘
       │
       └──→ API (نفس الـ Database)
```

---

## 📋 المتطلبات

### Backend
- Python 3.9+
- pip
- Linux/macOS/Windows

### Android
- AndroidIDE (على الهاتف)
- Gradle 7.5+
- Android SDK 33+

---

## 🚀 البدء

### Option 1: المثبت التقليدي

```bash
# 1. السيرفر
cd backend/
pip install -r requirements.txt
cp server.env.example server.env
nano server.env  # أضف BOT_TOKEN و CHAT_ID
python bot.py

# 2. الهاتف
# انقل CobraCards إلى الهاتف
# افتح في AndroidIDE
# عدّل IP في RetrofitClient.kt
# Build Release APK
```

### Option 2: Docker (الأسهل)

```bash
docker-compose up -d
```

---

## 🔧 الإعدادات

### server.env

```env
BOT_TOKEN=YOUR_TOKEN_FROM_BOTFATHER
CHAT_ID=YOUR_TELEGRAM_ID
API_PORT=5000
ADMIN_USERNAME=admin
ADMIN_PASSWORD=change_me
DATABASE_PATH=./bot_data.db
LOG_PATH=./logs/
```

### RetrofitClient.kt

```kotlin
private const val BASE_URL = "http://YOUR_SERVER_IP:5000/"
```

---

## 📊 قاعدة البيانات

### الجداول

1. **users** - المستخدمين
   ```
   id, username, password, balance, token, blocked, role
   ```

2. **recharge_log** - سجل الشحن
   ```
   id, auth_id, user_id, product_id, receiver, pin, success, message
   ```

3. **notifications** - الإشعارات
   ```
   id, user_id, title, body, read, created_at
   ```

4. **audit_log** - سجل العمليات
   ```
   id, user_id, action, details, ip_address, created_at
   ```

---

## 🔐 الأمان

### على مستوى التطبيق:
- FLAG_SECURE (منع اسكرين شوت)
- Obfuscation مع ProGuard
- Minification و Shrinking
- منع Copy/Paste
- كشف Rooting والمحاكيات

### على مستوى السيرفر:
- JWT Tokens
- Rate Limiting
- Audit Logging
- Password Hashing
- HTTPS Support

---

## 🧪 الاختبار

### اختبر API

```bash
# Health check
curl http://localhost:5000/health

# Login
curl -X POST http://localhost:5000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

### اختبر التطبيق

```
1. Login: admin / admin123
2. اختبر Recharge
3. عدّل الرصيد من البوت
4. تحقق من التزامن
```

---

## 📈 الـ Logging

### عرض السجلات

```bash
# البوت
tail -f logs/bot.log

# التطبيق
adb logcat | grep cobra
```

---

## 🔄 المزامنة الفورية

| عملية | البوت | التطبيق | السرعة |
|--------|------|---------|--------|
| Login | ✅ | ✅ | فوراً |
| Recharge | ✅ | ✅ | فوراً |
| Balance Edit | ✅ | ✅ | < 30s |
| User Block | ✅ | ✅ | فوراً |
| Notification | ✅ | ✅ | فوراً |

---

## 📱 شاشات التطبيق

### Login Screen
- دخول آمن
- كشف الحسابات المحظورة

### Dashboard
- عرض الرصيد
- شحن فودافون كاش
- لوحة أدمن

### History
- آخر 50 عملية
- حالة كل عملية

### Profile
- بيانات الحساب
- الإحصائيات

---

## 🐛 استكشاف الأخطاء

```
اقرأ: TROUBLESHOOTING.md
```

---

## 🎓 المزيد من المعلومات

- **DEPLOYMENT.md** - تثبيت الإنتاج
- **SECURITY.md** - تفاصيل الحماية
- **TROUBLESHOOTING.md** - حل المشاكل
- **QUICKSTART.md** - البدء السريع

---

## 📞 الدعم

للمساعدة:
1. اقرأ التوثيق ذات الصلة
2. تحقق من السجلات (logs)
3. استخدم `sqlite3 bot_data.db` للتحقق من الـ data

---

## 🎯 المستقبل

- [ ] WhatsApp Integration
- [ ] SMS Notifications
- [ ] Advanced Analytics
- [ ] 2FA Authentication
- [ ] Admin Dashboard Web
- [ ] Multi-language Support

---

**Made with ❤️ by Nagy Dev** 🐍

`Version 1.0 - Production Ready`

