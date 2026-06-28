# 🚀 COBRA CARDS - DEPLOYMENT GUIDE

## الجزء الأول: تشغيل البوت على السيرفر

### 1️⃣ تحضير السيرفر (Linux Ubuntu/Debian)

```bash
# تحديث النظام
sudo apt update && sudo apt upgrade -y

# تثبيت Python 3.9+
sudo apt install python3 python3-pip python3-venv -y

# التحقق
python3 --version
pip3 --version
```

### 2️⃣ نسخ المشروع وتثبيت المتطلبات

```bash
# انقل مجلد backend إلى السيرفر
scp -r backend/ user@your_server_ip:/home/user/cobra-bot/

# اتصل بالسيرفر
ssh user@your_server_ip

# انتقل للمجلد
cd /home/user/cobra-bot/

# عمل Virtual Environment
python3 -m venv venv
source venv/bin/activate

# تثبيت المتطلبات
pip install -r requirements.txt
```

### 3️⃣ إعداد الـ Configuration

```bash
# نسخ ملف الإعدادات
cp server.env.example server.env

# عدّل البيانات
nano server.env
```

**محتوى `server.env`:**
```env
BOT_TOKEN=YOUR_BOT_TOKEN_HERE
CHAT_ID=YOUR_TELEGRAM_ID
API_PORT=5000
ADMIN_USERNAME=admin
ADMIN_PASSWORD=secure_password_here
DATABASE_PATH=/home/user/cobra-bot/bot_data.db
LOG_PATH=/home/user/cobra-bot/logs/
```

### 4️⃣ اختبار التشغيل

```bash
# تشغيل مباشر
python bot.py

# يجب تشوف الرسالة:
# ✅ البوت شغال! API على البورت 5000
```

---

## الجزء الثاني: تشغيل دائم مع Systemd

### 1️⃣ إنشاء Systemd Service

```bash
sudo nano /etc/systemd/system/cobra-bot.service
```

**انسخ هذا المحتوى:**
```ini
[Unit]
Description=Cobra Cards Bot Service
After=network.target

[Service]
Type=simple
User=user
WorkingDirectory=/home/user/cobra-bot
Environment="PATH=/home/user/cobra-bot/venv/bin"
ExecStart=/home/user/cobra-bot/venv/bin/python /home/user/cobra-bot/bot.py
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

### 2️⃣ تفعيل الـ Service

```bash
# تحديث systemd
sudo systemctl daemon-reload

# تشغيل الخدمة
sudo systemctl start cobra-bot

# تشغيل تلقائي عند البدء
sudo systemctl enable cobra-bot

# التحقق من الحالة
sudo systemctl status cobra-bot

# عرض السجل
sudo journalctl -u cobra-bot -f
```

---

## الجزء الثالث: Docker Support (اختياري)

### 1️⃣ إنشاء Dockerfile

```bash
cat > /home/user/cobra-bot/Dockerfile << 'EOF'
FROM python:3.9-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

ENV BOT_TOKEN=""
ENV CHAT_ID=""
ENV API_PORT=5000

EXPOSE 5000

CMD ["python", "bot.py"]
EOF
```

### 2️⃣ بناء وتشغيل Docker

```bash
# بناء الصورة
docker build -t cobra-bot:latest .

# تشغيل الحاوية
docker run -d \
  -e BOT_TOKEN="your_token" \
  -e CHAT_ID="your_id" \
  -e API_PORT="5000" \
  -p 5000:5000 \
  -v cobra_data:/app \
  --name cobra-bot \
  cobra-bot:latest
```

---

## الجزء الرابع: تأمين الاتصال (HTTPS)

### 1️⃣ استخدام Nginx كـ Reverse Proxy

```bash
sudo apt install nginx -y

# إنشاء ملف الإعدادات
sudo nano /etc/nginx/sites-available/cobra-bot
```

**محتوى الإعدادات:**
```nginx
server {
    listen 443 ssl http2;
    server_name your_domain.com;

    ssl_certificate /etc/letsencrypt/live/your_domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your_domain.com/privkey.pem;

    location / {
        proxy_pass http://localhost:5000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}

server {
    listen 80;
    server_name your_domain.com;
    return 301 https://$server_name$request_uri;
}
```

### 2️⃣ تفعيل الموقع

```bash
# إنشاء رابط رمزي
sudo ln -s /etc/nginx/sites-available/cobra-bot /etc/nginx/sites-enabled/

# اختبار الإعدادات
sudo nginx -t

# إعادة تشغيل Nginx
sudo systemctl restart nginx
```

### 3️⃣ إصدار SSL Certificate مع Let's Encrypt

```bash
sudo apt install certbot python3-certbot-nginx -y

sudo certbot certonly --nginx -d your_domain.com
```

---

## الجزء الخامس: Backup التلقائي

### 1️⃣ إنشاء Script للـ Backup

```bash
cat > /home/user/cobra-bot/backup.sh << 'EOF'
#!/bin/bash

BACKUP_DIR="/home/user/cobra-bot/backups"
DB_FILE="/home/user/cobra-bot/bot_data.db"
DATE=$(date +%Y%m%d_%H%M%S)

mkdir -p $BACKUP_DIR

# نسخ احتياطي من قاعدة البيانات
cp $DB_FILE $BACKUP_DIR/bot_data_$DATE.db

# حذف الـ Backups القديمة (أكثر من 30 يوم)
find $BACKUP_DIR -name "bot_data_*.db" -mtime +30 -delete

echo "✅ Backup completed: $BACKUP_DIR/bot_data_$DATE.db"
EOF

chmod +x /home/user/cobra-bot/backup.sh
```

### 2️⃣ جدولة الـ Backup مع Cron

```bash
# فتح crontab
crontab -e

# أضف هذا السطر (كل يوم الساعة 2 صباحاً)
0 2 * * * /home/user/cobra-bot/backup.sh
```

---

## الجزء السادس: المراقبة والـ Logging

### 1️⃣ مجلد السجلات

```bash
mkdir -p /home/user/cobra-bot/logs
chmod 755 /home/user/cobra-bot/logs
```

### 2️⃣ عرض السجلات

```bash
# آخر 100 سطر
tail -100 /home/user/cobra-bot/logs/bot.log

# مراقبة مباشرة
tail -f /home/user/cobra-bot/logs/bot.log

# البحث عن الأخطاء
grep "ERROR" /home/user/cobra-bot/logs/bot.log
```

---

## الجزء السابع: تحديث IP في التطبيق

### ⚠️ **خطوة حرجة**

قبل بناء APK، عدّل هذا الملف:

```
app/src/main/java/com/cobra/cards/api/RetrofitClient.kt
```

```kotlin
private const val BASE_URL = "http://YOUR_SERVER_IP:5000/"
// أو إذا كان عندك Domain + HTTPS:
// private const val BASE_URL = "https://your_domain.com/"
```

**أمثلة:**
```kotlin
// محلي على نفس الشبكة:
private const val BASE_URL = "http://192.168.1.100:5000/"

// VPS:
private const val BASE_URL = "http://12.34.56.78:5000/"

// مع Domain وSSL:
private const val BASE_URL = "https://cobra.example.com/"
```

---

## الجزء الثامن: بناء APK النهائي

### 1️⃣ في AndroidIDE

```
1. افتح CobraCards في AndroidIDE
2. تأكد من تعديل IP/Domain
3. Build → Build Release APK
4. الملف سيكون في:
   app/build/outputs/apk/release/app-release.apk
```

### 2️⃣ توقيع APK (اختياري)

```bash
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
  -keystore ~/.android/debug.keystore \
  app-release.apk alias_name
```

---

## قائمة الفحص (Checklist)

### Backend:
- ✅ تثبيت Python وجميع المتطلبات
- ✅ إنشاء `.env` وملء البيانات الصحيحة
- ✅ اختبار البوت محلياً
- ✅ إعداد Systemd Service للتشغيل الدائم
- ✅ إعداد Backup التلقائي
- ✅ إعداد SSL/HTTPS (اختياري لكن موصى به)

### Android:
- ✅ تعديل IP السيرفر في RetrofitClient.kt
- ✅ اختبار الاتصال بالـ API
- ✅ تفعيل الحماية (ProGuard, FLAG_SECURE)
- ✅ بناء APK Release
- ✅ اختبار على هاتف فعلي

### الاختبار:
- ✅ تسجيل دخول من التطبيق
- ✅ الشحن والعمليات
- ✅ السجل والملف الشخصي
- ✅ لوحة الأدمن من البوت
- ✅ التزامن بين البوت والتطبيق

---

## استكشاف الأخطاء

### البوت لا يتصل بـ API

```bash
# تحقق من البورت
sudo lsof -i :5000

# اختبر الاتصال
curl http://localhost:5000/api/auth/login
```

### التطبيق لا يتصل بالسيرفر

```
1. تحقق من IP الصحيح في RetrofitClient.kt
2. تأكد من فتح البورت على الفائرويل:
   sudo ufw allow 5000
3. اختبر من الهاتف:
   curl http://server_ip:5000/api/auth/login
```

### Database Error

```bash
# احذف database القديم واعادة إنشاؤه
rm bot_data.db
python bot.py
# سيعيد إنشاء database تلقائياً
```

---

**Nagy Dev** 🐍
