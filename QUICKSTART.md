# ⚡ COBRA CARDS - QUICK START

## 🚀 تشغيل في 5 دقائق

### الخطوة 1️⃣: على السيرفر (Linux)

```bash
# انقل المشروع
cd CobraCards/backend/

# ثبّت المتطلبات
pip install -r requirements.txt

# عدّل الإعدادات
cp server.env.example server.env
nano server.env  # أضف BOT_TOKEN و CHAT_ID

# شغّل
python bot.py
```

**يجب تشوف:**
```
✅ البوت شغال! API على البورت 5000
```

---

### الخطوة 2️⃣: على الهاتف (Android)

#### خطوة مهمة جداً:

```
افتح: app/src/main/java/com/cobra/cards/api/RetrofitClient.kt
غيّر السطر:
private const val BASE_URL = "http://YOUR_SERVER_IP:5000/"

مثال:
private const val BASE_URL = "http://192.168.1.100:5000/"
```

#### Build APK:

```
AndroidIDE:
1. Open Project → اختر CobraCards
2. Build → Build Release APK
3. كمّل الإعدادات
4. APK جاهز!
```

---

### الخطوة 3️⃣: اختبار

```bash
# من الهاتف
اختبر تسجيل دخول:
Username: admin
Password: admin123
```

**لو شفت Dashboard = نجح! ✅**

---

## 🐳 تشغيل مع Docker (أسهل)

```bash
cd CobraCards/

# عدّل .env
cp .env.example .env
nano .env

# شغّل
docker-compose up -d

# تحقق
docker logs cobra-cards-bot -f
```

---

## 📱 المزامنة الفورية

| العملية | التطبيق | البوت | الوقت |
|---------|---------|-------|-------|
| تسجيل دخول | ✅ | ✅ | فوراً |
| شحن | ✅ | ✅ | فوراً |
| تعديل رصيد | ✅ | ✅ | < 30 ثانية |
| حظر مستخدم | ✅ | ✅ | فوراً |

---

## 🔒 الحماية الكاملة

- ✅ No screenshots allowed
- ✅ No copy/paste
- ✅ Code obfuscated (ProGuard)
- ✅ Rate limiting (10 req/min)
- ✅ JWT tokens
- ✅ Audit logging

---

## 📁 الملفات المهمة

```
CobraCards/
├── bot.py                  ← البوت الرئيسي
├── server.env              ← الإعدادات (عدّله!)
├── requirements.txt        ← المتطلبات
├── Dockerfile              ← للـ Docker
├── docker-compose.yml      ← لتشغيل سريع
├── DEPLOYMENT.md           ← شرح التشغيل الكامل
├── SECURITY.md             ← شرح الحماية
├── TROUBLESHOOTING.md      ← استكشاف الأخطاء
└── app/                    ← التطبيق الأندرويد
    └── src/main/java/com/cobra/cards/
        └── api/RetrofitClient.kt  ← عدّل IP هنا!
```

---

## 🆘 المشاكل الشائعة

### لا يتصل بالسيرفر؟
```
1. تأكد من IP صحيح في RetrofitClient.kt
2. افتح البورت: sudo ufw allow 5000
3. اختبر: curl http://YOUR_IP:5000/health
```

### البوت لا يشتغل؟
```
1. pip install python-telegram-bot==20.7
2. أضف BOT_TOKEN و CHAT_ID في server.env
3. python bot.py
```

### تطبيق يُغلق؟
```
1. ./gradlew clean
2. Build APK تاني
3. لو لسه نفس المشكلة اقرأ TROUBLESHOOTING.md
```

---

## 📞 الدعم

اقرأ:
1. **DEPLOYMENT.md** - شرح التشغيل الكامل
2. **SECURITY.md** - شرح الحماية
3. **TROUBLESHOOTING.md** - حل المشاكل

---

**Nagy Dev** 🐍
