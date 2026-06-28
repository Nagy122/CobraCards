package com.cobra.cards

import android.app.Application
import android.os.StrictMode
import com.cobra.cards.utils.SessionManager
import com.cobra.cards.utils.SecurityManager

class CobraApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SessionManager.init(this)
        
        // تفعيل الحماية من البداية
        SecurityManager.initSecurity(this)
    }
}
