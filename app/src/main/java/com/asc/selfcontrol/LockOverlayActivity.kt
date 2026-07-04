package com.asc.selfcontrol

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.asc.selfcontrol.data.Prefs
import com.asc.selfcontrol.util.PasswordUtil

class LockOverlayActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_LABEL = "extra_label"
    }

    private lateinit var pkg: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock)

        pkg = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        val label = intent.getStringExtra(EXTRA_LABEL) ?: pkg

        val tvAppLabel = findViewById<TextView>(R.id.tvAppLabel)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val tvError = findViewById<TextView>(R.id.tvError)
        val btnUnlock = findViewById<Button>(R.id.btnUnlock)
        val tvForgot = findViewById<TextView>(R.id.tvForgot)
        val tvCancel = findViewById<TextView>(R.id.tvCancel)

        tvAppLabel.text = "$label 当前处于限制时段"

        fun refreshCooldownUi() {
            if (Prefs.isInCooldown(this)) {
                val remainMin = ((Prefs.getCooldownUntil(this) - System.currentTimeMillis()) / 60000).coerceAtLeast(0)
                tvError.text = "输入错误次数过多，已锁定，请等待约 $remainMin 分钟"
                btnUnlock.isEnabled = false
            } else {
                btnUnlock.isEnabled = true
            }
        }
        refreshCooldownUi()

        btnUnlock.setOnClickListener {
            refreshCooldownUi()
            if (Prefs.isInCooldown(this)) return@setOnClickListener

            val input = etPassword.text.toString()
            if (!Prefs.isPasswordSet(this)) {
                Toast.makeText(this, "尚未设置密码，请先在 ASC 主界面设置", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val ok = PasswordUtil.verify(input, Prefs.getSalt(this), Prefs.getHash(this))
            if (ok) {
                Prefs.clearFailedAttempts(this)
                Prefs.grantGrace(this, pkg)
                Toast.makeText(this, "解锁成功，${Prefs.UNLOCK_GRACE_MILLIS / 60000} 分钟内可正常使用", Toast.LENGTH_LONG).show()
                finish()
            } else {
                Prefs.registerFailedAttempt(this)
                val left = (Prefs.MAX_ATTEMPTS - Prefs.getFailedAttempts(this))
                tvError.text = if (Prefs.isInCooldown(this)) {
                    "密码错误次数过多，已锁定 24 小时"
                } else {
                    "密码错误，还剩 $left 次尝试机会"
                }
                refreshCooldownUi()
                etPassword.text.clear()
            }
        }

        tvForgot.setOnClickListener {
            Prefs.requestForgotPassword(this)
            Toast.makeText(
                this,
                "已记录忘记密码请求。请等待 24 小时后，到 ASC 主界面重置密码。",
                Toast.LENGTH_LONG
            ).show()
        }

        tvCancel.setOnClickListener {
            goHome()
        }
    }

    private fun goHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(homeIntent)
        finish()
    }

    // Prevent leaving the lock screen via back button; user must go Home
    // or successfully unlock. This is a soft deterrent, not a hard block,
    // since a normal Activity cannot fully disable the system Home button
    // without device-owner/kiosk mode privileges.
    override fun onBackPressed() {
        goHome()
    }
}
