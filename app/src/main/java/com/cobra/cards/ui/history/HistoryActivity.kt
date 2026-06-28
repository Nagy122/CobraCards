package com.cobra.cards.ui.history

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cobra.cards.model.HistoryItem
import com.cobra.cards.repository.AppRepository
import kotlinx.coroutines.launch

class HistoryActivity : AppCompatActivity() {

    private val repository = AppRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔒 تفعيل الحماية
        SecurityManager.disableScreenshots(this)

        // Build UI programmatically - no XML needed
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF1A1A1A.toInt())
        }

        val titleBar = LinearLayout(this).apply {
            setBackgroundColor(0xFFE60000.toInt())
            setPadding(30, 40, 30, 40)
        }
        val tvTitle = TextView(this).apply {
            text = "📋 سجل العمليات"
            textSize = 20f
            setTextColor(0xFFFFFFFF.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        titleBar.addView(tvTitle)

        val scroll = ScrollView(this)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
        }

        val tvLoading = TextView(this).apply {
            text = "⏳ جاري التحميل..."
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = android.view.Gravity.CENTER
            setPadding(0, 50, 0, 0)
        }
        container.addView(tvLoading)
        scroll.addView(container)

        root.addView(titleBar)
        root.addView(scroll)
        setContentView(root)

        loadHistory(container, tvLoading)
    }

    private fun loadHistory(container: LinearLayout, tvLoading: TextView) {
        lifecycleScope.launch {
            val result = repository.getHistory()

            result.onSuccess { response ->
                container.removeView(tvLoading)
                if (response.history.isEmpty()) {
                    addTextRow(container, "لا يوجد سجل بعد", 0xFFAAAAAA.toInt())
                } else {
                    response.history.forEach { item ->
                        addHistoryCard(container, item)
                    }
                }
            }

            result.onFailure { e ->
                tvLoading.text = "❌ ${e.message}"
                tvLoading.setTextColor(0xFFFF4444.toInt())
            }
        }
    }

    private fun addHistoryCard(container: LinearLayout, item: HistoryItem) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF2A2A2A.toInt())
            setPadding(20, 16, 20, 16)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 12)
            layoutParams = params
        }

        val statusIcon = if (item.success == 1) "✅" else "❌"
        val statusColor = if (item.success == 1) 0xFF00FF88.toInt() else 0xFFFF4444.toInt()

        addCardRow(card, "$statusIcon ${item.productId}", statusColor, 15f, true)
        addCardRow(card, "📱 المستلم: ${item.receiver}", 0xFFFFFFFF.toInt(), 14f)
        addCardRow(card, "📅 ${item.createdAt}", 0xFFAAAAAA.toInt(), 12f)
        if (!item.message.isNullOrEmpty()) {
            addCardRow(card, "💬 ${item.message}", 0xFFCCCCCC.toInt(), 12f)
        }

        container.addView(card)
    }

    private fun addCardRow(parent: LinearLayout, text: String, color: Int, size: Float = 14f, bold: Boolean = false) {
        parent.addView(TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            if (bold) typeface = android.graphics.Typeface.DEFAULT_BOLD
            val p = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            p.setMargins(0, 4, 0, 4)
            layoutParams = p
        })
    }

    private fun addTextRow(parent: LinearLayout, text: String, color: Int) {
        parent.addView(TextView(this).apply {
            this.text = text
            textSize = 16f
            setTextColor(color)
            gravity = android.view.Gravity.CENTER
            setPadding(0, 30, 0, 0)
        })
    }
}
