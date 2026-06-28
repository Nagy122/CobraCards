package com.cobra.cards.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cobra.cards.databinding.ActivityLoginBinding
import com.cobra.cards.repository.AppRepository
import com.cobra.cards.ui.main.MainActivity
import com.cobra.cards.utils.SessionManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val repository = AppRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔒 تفعيل الحماية
        SecurityManager.disableScreenshots(this)

        // إذا كان مسجلاً دخوله بالفعل، اذهب مباشرة للرئيسية
        if (SessionManager.isLoggedIn() && SessionManager.getRawToken().isNotEmpty()) {
            goToMain()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener { attemptLogin() }
    }

    private fun attemptLogin() {
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (username.isEmpty()) {
            binding.tvLoginStatus.text = "❌ أدخل اسم المستخدم"
            binding.tvLoginStatus.setTextColor(0xFFFF4444.toInt())
            return
        }
        if (password.isEmpty()) {
            binding.tvLoginStatus.text = "❌ أدخل كلمة السر"
            binding.tvLoginStatus.setTextColor(0xFFFF4444.toInt())
            return
        }

        setLoading(true)
        binding.tvLoginStatus.text = "⏳ جاري التحقق..."
        binding.tvLoginStatus.setTextColor(0xFFFFFFFF.toInt())

        lifecycleScope.launch {
            val result = repository.login(username, password)
            setLoading(false)

            result.onSuccess { response ->
                if (response.success && response.token != null) {
                    val user = response.user
                    SessionManager.saveSession(
                        token = response.token,
                        username = user?.username ?: username,
                        balance = user?.balance ?: 0,
                        role = user?.role ?: "user"
                    )
                    if (user?.blocked == true) {
                        showError("🚫 حسابك محظور. تواصل مع الإدارة.")
                        SessionManager.clearSession()
                    } else {
                        showSuccess("✅ تم الدخول بنجاح!")
                        goToMain()
                    }
                } else {
                    showError("❌ ${response.message ?: "يوزر أو باسورد غلط"}")
                }
            }

            result.onFailure { e ->
                showError("❌ ${e.message}")
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.loginProgressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
        binding.etUsername.isEnabled = !loading
        binding.etPassword.isEnabled = !loading
    }

    private fun showError(msg: String) {
        binding.tvLoginStatus.text = msg
        binding.tvLoginStatus.setTextColor(0xFFFF4444.toInt())
    }

    private fun showSuccess(msg: String) {
        binding.tvLoginStatus.text = msg
        binding.tvLoginStatus.setTextColor(0xFF00FF88.toInt())
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
