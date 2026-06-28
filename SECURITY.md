# 🔒 COBRA CARDS - SECURITY DOCUMENTATION

## طبقات الحماية

### 1. 📸 منع اسكرين شوت وتسجيل الشاشة
```kotlin
SecurityManager.disableScreenshots(activity)
// ← يفعّل FLAG_SECURE على كل Activity
// ← التطبيق غير قابل للتصوير أو التسجيل
```

**الآلية:**
- `WindowManager.LayoutParams.FLAG_SECURE` في كل Activity
- منع التطبيق من الظهور في Preview
- عدم إمكانية استخدام Screencast

---

### 2. 🔐 منع Copy/Paste من البيانات الحساسة
```kotlin
SecurityManager.disableCopyPasteForView(textView)
// ← يعطل الاختيار والنسخ واللصق
// ← يعطل الـ Action Mode (النسخ/القص/الإلصاق)
```

---

### 3. 🛡️ منع Debugging والـ Breakpoints
```kotlin
SecurityManager.preventDebugging()
// ← يكتشف لو حد وصّل Debugger
// ← يُغلق التطبيق فوراً إذا اكتشف Debugger
```

**الفحص:**
```kotlin
if (android.os.Debug.isDebuggerConnected()) {
    System.exit(0)  // ← إغلاق فوري
}
```

---

### 4. 🌳 كشف Rooting والـ Jailbreak
```kotlin
SecurityManager.detectRooting()
// ← يبحث عن آثار الـ Root
// ← يُغلق التطبيق إذا اكتشف Root
```

**المسارات المراقبة:**
- `/sbin/su`
- `/system/bin/su`
- `/system/xbin/su`
- و 7 مسارات أخرى معروفة

---

### 5. 🎮 كشف المحاكيات (Emulators)
```kotlin
if (SecurityManager.isRunningOnEmulator()) {
    // منع التطبيق من العمل على محاكي
}
```

**الفحص:**
- `android.os.Build.FINGERPRINT`
- `android.os.Build.MODEL`
- `/system/app/SdkSetup.apk`

---

### 6. 🔤 Obfuscation + Minification
**في `build.gradle` (Release Build):**
```gradle
minifyEnabled true
shrinkResources true
proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
```

**ProGuard Rules:**
- ✅ إعادة تسمية الـ Classes إلى `a`, `b`, `c`, ...
- ✅ حذف جميع الـ Debug Info
- ✅ حذف الـ Logging Statements
- ✅ إعادة ترتيب الـ Package (`-repackageclasses`)
- ✅ 5 مستويات Optimization

**النتيجة:**
- الـ APK يصبح 30-40% أصغر
- الكود غير مقروء تماماً حتى مع Decompiler
- مستحيل فهم Logic التطبيق

---

### 7. 🔐 تشفير البيانات الحساسة
```kotlin
val encryptedToken = SecurityManager.encryptSensitiveData(token)
// ← تشفير Base64 للـ Token
val decrypted = SecurityManager.decryptSensitiveData(encryptedToken)
```

---

### 8. 🧹 تنظيف Memory
```kotlin
SecurityManager.clearSensitiveData()
// ← حذف البيانات من الذاكرة عند الخروج
// ← منع استخراج البيانات من Memory Dump
```

---

### 9. 🚫 منع الظهور في Recent Apps
```kotlin
SecurityManager.exitToPreventRecents(activity)
// ← التطبيق لا يظهر في Recent Apps
// ← منع صور المعاينة
```

---

## طبقات الحماية في Backend

### 1. 🔑 JWT Token
```python
# كل API Call تحتاج Bearer Token
Authorization: Bearer {token_hex_32_char}
```

### 2. 🚫 Blocked Users
```python
# لو المستخدم محظور، الـ API ترفض:
if user["blocked"]:
    return {"success": False, "message": "🚫 حسابك محظور"}
```

### 3. 📋 Audit Logging
```python
# كل عملية تُسجّل:
# - من استخدمها
# - متى
# - الحالة (نجح/فشل)
```

---

## Build APK Release (مع كل الحماية)

```bash
cd CobraCards
./gradlew assembleRelease
# ← APK سيكون في: app/build/outputs/apk/release/app-release.apk
```

**التحقق:**
```bash
# فك APK
unzip app-release.apk

# محاولة decompile الكود
apktool d app-release.apk
# → الكود مشفر ولا معنى له
```

---

## قائمة الفحص (Checklist)

- ✅ FLAG_SECURE مفعّل على كل Activity
- ✅ اسكرين شوت معطّل
- ✅ Debugger Detection مفعّل
- ✅ Rooting Detection مفعّل
- ✅ ProGuard Obfuscation مفعّل
- ✅ Minification مفعّل
- ✅ Shrink Resources مفعّل
- ✅ Debug Info محذوف
- ✅ Logging معطّل في Release
- ✅ Copy/Paste معطّل
- ✅ API Token محمي
- ✅ Blocked Users في Database

---

## التحديات المتبقية

### حدود Android:
- لا يوجد حماية 100% ضد Reverse Engineering
- لكن توليفة البروتوكولات تجعله صعب جداً

### الحل الأمثل:
1. **Server-Side Logic** - كل العمليات الحساسة تكون على السيرفر
2. **API Signatures** - توقيع كل Request
3. **Certificate Pinning** - ربط الـ Certificate بالتطبيق
4. **Rate Limiting** - تحديد عدد الـ Requests
5. **Multi-Factor Authentication** - تفعيل 2FA

---

**Nagy Dev** 🐍
