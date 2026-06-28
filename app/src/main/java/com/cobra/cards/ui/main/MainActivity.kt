package com.cobra.cards.ui.main

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cobra.cards.databinding.ActivityMainBinding
import com.cobra.cards.repository.AppRepository
import com.cobra.cards.ui.history.HistoryActivity
import com.cobra.cards.ui.login.LoginActivity
import com.cobra.cards.ui.profile.ProfileActivity
import com.cobra.cards.utils.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val repository = AppRepository()

    private val products = listOf(
        "فكة 10 وحدات"   to "Fakka_10_Unite",
        "فكة 20 وحدات"   to "Fakka_20_Unite",
        "فكة 50 وحدات"   to "Fakka_50_Unite",
        "فكة 100 وحدات"  to "Fakka_100_Unite"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔒 تفعيل الحماية
        SecurityManager.disableScreenshots(this)

        if (!SessionManager.isLoggedIn()) {
            logout(); return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSpinner()
        setupButtons()
        refreshBalance()
        startAutoRefresh()
    }

    private fun setupSpinner() {
        val names = products.map { it.first }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerProduct.adapter = adapter
    }

    private fun setupButtons() {
        // زر الشحن
        binding.btnCharge.setOnClickListener { startRecharge() }

        // تحديث الرصيد
        binding.btnRefresh.setOnClickListener { refreshBalance() }

        // السجل
        binding.btnHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        // الملف الشخصي
        binding.btnProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        // تسجيل الخروج
        binding.btnLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("تسجيل الخروج")
                .setMessage("هل تريد الخروج؟")
                .setPositiveButton("نعم") { _, _ -> logout() }
                .setNegativeButton("لا", null)
                .show()
        }

        // لوحة الأدمن - تظهر فقط للأدمن
        if (SessionManager.isAdmin()) {
            binding.btnAdmin.visibility = View.VISIBLE
            binding.btnAdmin.setOnClickListener { showAdminPanel() }
        } else {
            binding.btnAdmin.visibility = View.GONE
        }
    }

    private fun refreshBalance() {
        binding.tvBalance.text = "💰 جاري التحديث..."
        lifecycleScope.launch {
            val result = repository.getBalance()
            result.onSuccess { response ->
                if (response.blocked) {
                    showBlockedDialog()
                } else {
                    SessionManager.updateBalance(response.balance)
                    binding.tvBalance.text = "💰 رصيدك: ${response.balance} وحدة"
                    binding.tvUsername.text = "👤 ${SessionManager.getUsername()}"
                }
            }
            result.onFailure {
                binding.tvBalance.text = "💰 رصيدك: ${SessionManager.getBalance()} وحدة"
                binding.tvUsername.text = "👤 ${SessionManager.getUsername()}"
            }
        }
    }

    private fun startRecharge() {
        val receiver = binding.etReceiver.text.toString().trim()
        val pin = binding.etPin.text.toString().trim()
        val selectedIndex = binding.spinnerProduct.selectedItemPosition
        val productId = products[selectedIndex].second

        if (!receiver.startsWith("01") || receiver.length != 11 || !receiver.all { it.isDigit() }) {
            showStatus("❌ رقم غير صحيح (01XXXXXXXXX)", isError = true); return
        }
        if (pin.length != 6 || !pin.all { it.isDigit() }) {
            showStatus("❌ الرقم السري 6 أرقام", isError = true); return
        }

        setChargeLoading(true)
        showStatus("⏳ جاري التحقق من الرصيد...", isError = false)

        lifecycleScope.launch {
            val authResult = repository.authorize(productId, receiver, pin)

            authResult.onSuccess { authResponse ->
                if (authResponse.success) {
                    val authId = authResponse.authId ?: ""
                    val newBalance = authResponse.newBalance ?: 0
                    SessionManager.updateBalance(newBalance)
                    binding.tvBalance.text = "💰 رصيدك: $newBalance وحدة"

                    // إبلاغ السيرفر بالنجاح
                    repository.report(authId, true, "✅ تم بنجاح من التطبيق")

                    showStatus("✅ تم إرسال الشحن!\n📱 المستلم: $receiver\n💰 رصيدك الجديد: $newBalance", isError = false)
                    binding.etReceiver.text.clear()
                    binding.etPin.text.clear()
                } else {
                    showStatus("❌ ${authResponse.message ?: "رفض السيرفر الطلب"}", isError = true)
                }
            }

            authResult.onFailure { e ->
                showStatus("❌ ${e.message}", isError = true)
            }

            setChargeLoading(false)
        }
    }

    private fun showAdminPanel() {
        val options = arrayOf(
            "📋 عرض المستخدمين",
            "➕ إضافة مستخدم",
            "✏️ تعديل رصيد",
            "🗑 حذف مستخدم",
            "🚫 حظر/رفع حظر"
        )
        AlertDialog.Builder(this)
            .setTitle("👑 لوحة الأدمن")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showAllUsers()
                    1 -> showAddUserDialog()
                    2 -> showEditBalanceDialog()
                    3 -> showDeleteUserDialog()
                    4 -> showBlockUserDialog()
                }
            }
            .show()
    }

    private fun showAllUsers() {
        lifecycleScope.launch {
            val result = repository.getUsers()
            result.onSuccess { response ->
                if (response.success) {
                    val msg = if (response.users.isEmpty()) "لا يوجد مستخدمين"
                    else response.users.joinToString("\n") { u ->
                        val status = if (u.blocked) "🚫" else "✅"
                        "$status ${u.username} | 💰 ${u.balance}"
                    }
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("📋 المستخدمين")
                        .setMessage(msg)
                        .setPositiveButton("حسناً", null)
                        .show()
                }
            }
            result.onFailure { e ->
                toast("❌ ${e.message}")
            }
        }
    }

    private fun showAddUserDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 20)
        }
        val etUser = EditText(this).apply { hint = "اسم المستخدم" }
        val etPass = EditText(this).apply { hint = "كلمة السر" }
        val etBal  = EditText(this).apply { hint = "الرصيد"; inputType = android.text.InputType.TYPE_CLASS_NUMBER }
        layout.addView(etUser); layout.addView(etPass); layout.addView(etBal)

        AlertDialog.Builder(this)
            .setTitle("➕ إضافة مستخدم")
            .setView(layout)
            .setPositiveButton("إضافة") { _, _ ->
                val u = etUser.text.toString().trim()
                val p = etPass.text.toString().trim()
                val b = etBal.text.toString().toIntOrNull() ?: 0
                if (u.isNotEmpty() && p.isNotEmpty()) {
                    lifecycleScope.launch {
                        val r = repository.createUser(u, p, b)
                        r.onSuccess { if (it.success) toast("✅ تم إضافة $u") else toast("❌ ${it.message}") }
                        r.onFailure { toast("❌ ${it.message}") }
                    }
                } else toast("أدخل جميع البيانات")
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun showEditBalanceDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 20)
        }
        val etUser = EditText(this).apply { hint = "اسم المستخدم" }
        val etBal  = EditText(this).apply { hint = "الرصيد الجديد"; inputType = android.text.InputType.TYPE_CLASS_NUMBER }
        layout.addView(etUser); layout.addView(etBal)

        AlertDialog.Builder(this)
            .setTitle("✏️ تعديل رصيد")
            .setView(layout)
            .setPositiveButton("تعديل") { _, _ ->
                val u = etUser.text.toString().trim()
                val b = etBal.text.toString().toIntOrNull() ?: 0
                if (u.isNotEmpty()) {
                    lifecycleScope.launch {
                        val r = repository.editBalance(u, b)
                        r.onSuccess { if (it.success) toast("✅ تم تعديل رصيد $u إلى $b") else toast("❌ ${it.message}") }
                        r.onFailure { toast("❌ ${it.message}") }
                    }
                } else toast("أدخل اسم المستخدم")
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun showDeleteUserDialog() {
        val etUser = EditText(this).apply { hint = "اسم المستخدم" }
        AlertDialog.Builder(this)
            .setTitle("🗑 حذف مستخدم")
            .setView(etUser)
            .setPositiveButton("حذف") { _, _ ->
                val u = etUser.text.toString().trim()
                if (u.isNotEmpty()) {
                    AlertDialog.Builder(this)
                        .setTitle("تأكيد الحذف")
                        .setMessage("هل تريد حذف $u؟")
                        .setPositiveButton("نعم") { _, _ ->
                            lifecycleScope.launch {
                                val r = repository.deleteUser(u)
                                r.onSuccess { if (it.success) toast("✅ تم حذف $u") else toast("❌ ${it.message}") }
                                r.onFailure { toast("❌ ${it.message}") }
                            }
                        }
                        .setNegativeButton("لا", null).show()
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun showBlockUserDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 20)
        }
        val etUser = EditText(this).apply { hint = "اسم المستخدم" }
        layout.addView(etUser)

        AlertDialog.Builder(this)
            .setTitle("🚫 حظر / رفع حظر")
            .setView(layout)
            .setPositiveButton("حظر") { _, _ ->
                val u = etUser.text.toString().trim()
                if (u.isNotEmpty()) {
                    lifecycleScope.launch {
                        val r = repository.blockUser(u, true)
                        r.onSuccess { toast(if (it.success) "🚫 تم حظر $u" else "❌ ${it.message}") }
                        r.onFailure { toast("❌ ${it.message}") }
                    }
                }
            }
            .setNeutralButton("رفع الحظر") { _, _ ->
                val u = etUser.text.toString().trim()
                if (u.isNotEmpty()) {
                    lifecycleScope.launch {
                        val r = repository.blockUser(u, false)
                        r.onSuccess { toast(if (it.success) "✅ تم رفع حظر $u" else "❌ ${it.message}") }
                        r.onFailure { toast("❌ ${it.message}") }
                    }
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun showBlockedDialog() {
        AlertDialog.Builder(this)
            .setTitle("🚫 حساب محظور")
            .setMessage("تم حظر حسابك. تواصل مع الإدارة.")
            .setCancelable(false)
            .setPositiveButton("خروج") { _, _ -> logout() }
            .show()
    }

    // Auto-refresh balance every 30 seconds
    private fun startAutoRefresh() {
        lifecycleScope.launch {
            while (true) {
                delay(30_000)
                if (!isFinishing) refreshBalance()
            }
        }
    }

    private fun setChargeLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnCharge.isEnabled = !loading
    }

    private fun showStatus(msg: String, isError: Boolean) {
        binding.tvStatus.text = msg
        binding.tvStatus.setTextColor(
            if (isError) 0xFFFF4444.toInt() else 0xFF00FF88.toInt()
        )
    }

    private fun logout() {
        SessionManager.clearSession()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
