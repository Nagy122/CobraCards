package com.cobra.cards.ui.profile

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cobra.cards.repository.AppRepository
import com.cobra.cards.utils.SessionManager
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private val repository = AppRepository()
    private lateinit var tvContent: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔒 تفعيل الحماية
        SecurityManager.disableScreenshots(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF1A1A1A.toInt())
        }

        val titleBar = LinearLayout(this).apply {
            setBackgroundColor(0xFFE60000.toInt())
            setPadding(30, 40, 30, 40)
        }
        titleBar.addView(TextView(this).apply {
            text = "👤 الملف الشخصي"
            textSize = 20f
            setTextColor(0xFFFFFFFF.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })

        tvContent = TextView(this).apply {
            text = "⏳ جاري التحميل..."
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(30, 40, 30, 40)
        }

        root.addView(titleBar)
        root.addView(tvContent)
        setContentView(root)

        loadProfile()
    }

    private fun loadProfile() {
        lifecycleScope.launch {
            val result = repository.getProfile()
            result.onSuccess { p ->
                val statusText = if (p.blocked) "🚫 محظور" else "✅ نشط"
                tvContent.text = """
👤 اسم المستخدم: ${p.username}
💰 الرصيد الحالي: ${p.balance} وحدة
📊 إجمالي العمليات: ${p.totalRecharges}
✅ العمليات الناجحة: ${p.successRecharges}
❌ العمليات الفاشلة: ${p.totalRecharges - p.successRecharges}
🔰 الحالة: $statusText
                """.trimIndent()
            }
            result.onFailure { e ->
                // Fallback to local data
                tvContent.text = """
👤 اسم المستخدم: ${SessionManager.getUsername()}
💰 الرصيد الحالي: ${SessionManager.getBalance()} وحدة
🔰 الحالة: ✅ نشط
                """.trimIndent()
            }
        }
    }
}
