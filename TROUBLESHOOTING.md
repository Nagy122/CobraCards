# 🔧 COBRA CARDS - TROUBLESHOOTING GUIDE

## 1️⃣ مشاكل Backend

### ❌ خطأ: "ModuleNotFoundError: No module named 'telegram'"

```bash
# الحل:
pip install python-telegram-bot==20.7
```

### ❌ خطأ: "BOT_TOKEN invalid" 

```bash
# تحقق من:
1. أن BOT_TOKEN صحيح من @BotFather
2. أنك نسخته بالظبط بدون مسافات
3. عدم وجود أحرف خاصة

# اختبر:
curl -X POST https://api.telegram.org/botYOUR_TOKEN/getMe
```

### ❌ خطأ: "Address already in use :5000"

```bash
# البورت 5000 مستخدم بالفعل
# حل 1: استخدم بورت مختلف
API_PORT=5001 python bot.py

# حل 2: اقتل العملية القديمة
sudo lsof -i :5000
sudo kill -9 <PID>
```

### ❌ خطأ: "Database is locked"

```bash
# قاعدة البيانات معطلة
# الحل:
rm bot_data.db
python bot.py
# سيعيد إنشاء database جديد
```

### ❌ خطأ: "sqlite3.IntegrityError: UNIQUE constraint failed"

```bash
# اليوزر موجود بالفعل
# الحل من البوت:
# استخدم username مختلف
```

---

## 2️⃣ مشاكل Android

### ❌ تطبيق لا يتصل بالسيرفر

**السبب الأول - IP خاطئ:**
```kotlin
// افتح: app/src/main/java/com/cobra/cards/api/RetrofitClient.kt
// تأكد من:
private const val BASE_URL = "http://192.168.1.100:5000/"
```

**اختبر:**
```bash
# من الهاتف
adb shell
curl http://192.168.1.100:5000/health
# يجب ترى: {"status":"✅ OK"}
```

**السبب الثاني - Firewall:**
```bash
# على السيرفر
sudo ufw status
sudo ufw allow 5000
```

**السبب الثالث - الشبكة:**
```
1. تأكد أن الهاتف والسيرفر على نفس الشبكة
2. أو استخدم VPS IP (عام)
3. أو استخدم Domain + SSL
```

### ❌ تطبيق يُغلق عند الفتح

**السبب الأول - Missing SecurityManager:**
```kotlin
// تأكد من وجود:
com/cobra/cards/utils/SecurityManager.kt
```

**السبب الثاني - Gradle sync failed:**
```bash
# افتح Terminal في AndroidIDE:
./gradlew clean
./gradlew sync
```

### ❌ "تسجيل دخول فاشل مع كل محاولة"

```
1. تحقق من اسم المستخدم والباسورد صحيح
2. تأكد من أدمن موجود في database:
   
   على السيرفر:
   sqlite3 bot_data.db
   SELECT * FROM users;
```

### ❌ "طلب الشحن ما يشتغل"

```
1. تأكد من رصيدك > 0
2. تأكد من رقم المستلم 01XXXXXXXXX (11 رقم)
3. تأكد من PIN 6 أرقام
4. تحقق من لوحة تحكم البوت - هل الطلب وصل؟
```

### ❌ "لا يوجد سجل"

```
1. جرّب شحن جديد أولاً
2. انتظر 30 ثانية للـ auto-refresh
3. انقر تحديث الرصيد
```

---

## 3️⃣ مشاكل التزامن Real-time

### ❌ تعديل رصيد من البوت لا ينعكس على التطبيق

```
1. اضغط "تحديث الرصيد"
2. أو انتظر 30 ثانية (auto-refresh)
3. تأكد من تسجيل دخولك في التطبيق
```

### ❌ حظر المستخدم لا يُقفل الدخول

```
1. سجّل الخروج وأدخل تاني
2. لو لسه نفس المشكلة تحقق من database:
   
   sqlite3 bot_data.db
   SELECT * FROM users WHERE username='xxx';
   # يجب تشوف blocked=1
```

---

## 4️⃣ مشاكل الحماية

### ❌ "التطبيق يسمح باسكرين شوت"

```
1. تأكد من تفعيل SecurityManager
2. اعد بناء APK بـ Release mode
3. تحقق من وجود FLAG_SECURE في كل Activity
```

### ❌ "Debugger يتصل بالتطبيق"

```
1. ينبغي التطبيق يُغلق فوراً
2. تأكد من: debuggable = false في build.gradle
```

### ❌ "ProGuard ما اشتغل"

```
1. افتح app/build.gradle
2. تأكد من:
   minifyEnabled true
   shrinkResources true
3. اعد بناء APK
```

---

## 5️⃣ مشاكل الـ Logs

### 📋 عرض سجلات البوت

```bash
# في الوقت الفعلي:
tail -f logs/bot.log

# آخر 100 سطر:
tail -100 logs/bot.log

# البحث عن أخطاء:
grep "ERROR" logs/bot.log

# الأخطاء الأخيرة:
grep "ERROR" logs/bot.log | tail -20
```

### 📋 عرض سجلات التطبيق

```bash
# في الهاتف:
adb logcat | grep "cobra"

# عرض آخر 100 سطر:
adb logcat -d | grep "cobra" | tail -100
```

---

## 6️⃣ مشاكل الـ Database

### 🗄️ إصلاح database

```bash
# فحص السلامة:
sqlite3 bot_data.db "PRAGMA integrity_check;"

# إصلاح:
sqlite3 bot_data.db "PRAGMA optimize;"

# عرض الجداول:
sqlite3 bot_data.db ".tables"

# عرض المستخدمين:
sqlite3 bot_data.db "SELECT * FROM users;"

# حذف مستخدم:
sqlite3 bot_data.db "DELETE FROM users WHERE username='xxx';"

# تحديث الرصيد:
sqlite3 bot_data.db "UPDATE users SET balance=100 WHERE username='admin';"
```

---

## 7️⃣ Performance

### ⚡ التطبيق بطيء

```
1. قلل عدد السجلات:
   SELECT COUNT(*) FROM recharge_log;
   # لو أكتر من 100,000 احذف القديم

2. أضف Index:
   sqlite3 bot_data.db "CREATE INDEX idx_user_id ON recharge_log(user_id);"

3. استخدم ProGuard (R8) للـ APK
```

### ⚡ API بطيء

```
1. تحقق من استخدام CPU والـ Memory:
   top -b

2. قلل عدد الـ requests المتزامنة

3. أضف caching في RetrofitClient.kt
```

---

## 8️⃣ الـ Rate Limiting

### ⚠️ رسالة: "محاولات كثيرة"

```
هذا طبيعي - حماية ضد الـ Spam:
- 10 requests / 60 seconds per IP
- انتظر دقيقة وحاول تاني
```

**لو أنت الأدمن:**
```python
# غيّر في bot.py:
RATE_LIMIT_REQUESTS = 10
RATE_LIMIT_SECONDS = 60
```

---

## 9️⃣ Backup و Recovery

### 💾 عمل Backup يدوي

```bash
# قبل أي تعديل كبير:
cp bot_data.db bot_data.db.backup

# استرجاع:
cp bot_data.db.backup bot_data.db
```

### 💾 إصلاح database تالف

```bash
# تصدير البيانات:
sqlite3 bot_data.db ".dump" > backup.sql

# حذف الـ database:
rm bot_data.db

# إعادة إنشاء:
python bot.py

# استيراد البيانات:
sqlite3 bot_data.db < backup.sql
```

---

## 🔟 قائمة الفحص (Debugging)

قبل الإبلاغ عن مشكلة، تحقق من:

- ✅ Python 3.9+ مثبت
- ✅ كل المتطلبات مثبتة: `pip list | grep -E 'telegram|flask|requests'`
- ✅ BOT_TOKEN صحيح وفعّال
- ✅ CHAT_ID صحيح
- ✅ البورت 5000 مفتوح (أو البورت اللي اخترته)
- ✅ base_url صحيح في التطبيق
- ✅ الهاتف والسيرفر على نفس الشبكة
- ✅ Firewall مسموح للبورت
- ✅ database موجود أو يُنشأ تلقائياً
- ✅ أدمن موجود (username: admin)
- ✅ No debugger connected
- ✅ ProGuard enabled في Release APK

---

**لو في مشكلة بتقول:**

```
1. رقم الخطأ (Error Code/Message)
2. الخطوات اللي تسبب المشكلة
3. الـ Log اللي فيها الخطأ (من logs/bot.log)
4. نتيجة: sqlite3 bot_data.db ".schema"
```

**Nagy Dev** 🐍
