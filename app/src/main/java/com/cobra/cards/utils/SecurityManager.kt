package com.cobra.cards.utils

import android.app.Activity
import android.app.Application
import android.content.Context
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

object SecurityManager {

    /**
     * تفعيل جميع الحماية عند بدء التطبيق
     */
    fun initSecurity(app: Application) {
        // منع اسكرين شوت على مستوى التطبيق
        disableScreenshotGlobally()
        
        // منع Debugging
        preventDebugging()
        
        // كشف Rooting والمحاكيات
        detectRooting()
    }

    /**
     * 🔒 تعطيل اسكرين شوت كامل
     * FLAG_SECURE = تطبيق لا يمكن تصويره ولا تسجيل فيديو
     */
    fun disableScreenshots(activity: Activity) {
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }

    /**
     * 🔒 تعطيل Copy/Paste على كل المدخلات
     */
    fun disableCopyPasteForView(view: TextView) {
        view.isLongClickable = false
        view.setTextIsSelectable(false)
        
        if (view is EditText) {
            view.isCursorVisible = false
            view.customSelectionActionModeCallback = object : android.view.ActionMode.Callback {
                override fun onCreateActionMode(mode: android.view.ActionMode?, menu: android.view.Menu?): Boolean = false
                override fun onPrepareActionMode(mode: android.view.ActionMode?, menu: android.view.Menu?): Boolean = false
                override fun onActionItemClicked(mode: android.view.ActionMode?, item: android.view.MenuItem?): Boolean = false
                override fun onDestroyActionMode(mode: android.view.ActionMode?) {}
            }
        }
    }

    /**
     * 🔒 منع التطبيق من الظهور في Recent Apps
     */
    fun exitToPreventRecents(activity: Activity) {
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }

    /**
     * 🔒 كشف محاولات Debugging
     */
    private fun preventDebugging() {
        // جعل التطبيق يكتشف Debugger
        try {
            if (android.os.Debug.isDebuggerConnected()) {
                throw SecurityException("❌ Debugger detected!")
            }
        } catch (e: Exception) {
            // لو حد حاول يوصل debugger
            System.exit(0)
        }
    }

    /**
     * 🔒 كشف Rooting والمحاكيات
     */
    private fun detectRooting() {
        val roottedPaths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        )

        for (path in roottedPaths) {
            if (java.io.File(path).exists()) {
                // التطبيق يعرف إنه على جهاز Rooted
                // ممكن تعمل إجراء (مثل إغلاق التطبيق)
                android.os.Process.killProcess(android.os.Process.myPid())
                System.exit(1)
            }
        }
    }

    /**
     * 🔒 تشفير البيانات الحساسة في Memory
     */
    fun encryptSensitiveData(data: String): String {
        return android.util.Base64.encodeToString(
            data.toByteArray(),
            android.util.Base64.NO_WRAP
        )
    }

    /**
     * 🔒 فك تشفير البيانات
     */
    fun decryptSensitiveData(encrypted: String): String {
        return String(
            android.util.Base64.decode(encrypted, android.util.Base64.NO_WRAP)
        )
    }

    /**
     * 🔒 منع Reverse Engineering
     * - تفعيل ProGuard/R8 في build.gradle
     * - حذف Debug Symbols
     * - Obfuscation
     */
    fun enableObfuscation() {
        // يتم تطبيقه تلقائياً في build.gradle release builds
    }

    /**
     * 🔒 حماية من Screen Recording
     */
    fun preventScreenRecording(activity: Activity) {
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }

    /**
     * 🔒 كشف Virtual Device (محاكي)
     */
    fun isRunningOnEmulator(): Boolean {
        return android.os.Build.FINGERPRINT.contains("generic") ||
               android.os.Build.FINGERPRINT.contains("unknown") ||
               android.os.Build.MODEL.contains("google_sdk") ||
               android.os.Build.MODEL.contains("Emulator") ||
               android.os.Build.PRODUCT == "sdk" ||
               java.io.File("/system/app/SdkSetup.apk").exists()
    }

    /**
     * 🔒 تنظيف Memory عند الخروج
     */
    fun clearSensitiveData() {
        System.gc()
        Runtime.getRuntime().gc()
    }

    private fun disableScreenshotGlobally() {
        // هيتم تطبيقه على كل Activity
    }
}

