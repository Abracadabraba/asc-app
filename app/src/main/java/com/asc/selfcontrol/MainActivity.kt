package com.asc.selfcontrol

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.accessibilityservice.AccessibilityServiceInfo
import android.text.TextUtils
import android.view.accessibility.AccessibilityManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.asc.selfcontrol.data.Prefs
import com.asc.selfcontrol.data.Rule
import com.asc.selfcontrol.util.PasswordUtil

data class AppInfo(val label: String, val packageName: String) {
    override fun toString() = label
}

class MainActivity : AppCompatActivity() {

    private lateinit var spinnerApps: Spinner
    private lateinit var timePickerStart: TimePicker
    private lateinit var timePickerEnd: TimePicker
    private lateinit var listRules: ListView
    private lateinit var tvAccessibilityStatus: TextView
    private lateinit var btnEnableAccessibility: Button

    private lateinit var layoutSetPassword: LinearLayout
    private lateinit var etNewPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnSetPassword: Button
    private lateinit var tvPasswordStatus: TextView
    private lateinit var btnResetPassword: Button

    private var appList: List<AppInfo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        spinnerApps = findViewById(R.id.spinnerApps)
        timePickerStart = findViewById(R.id.timePickerStart)
        timePickerEnd = findViewById(R.id.timePickerEnd)
        listRules = findViewById(R.id.listRules)
        tvAccessibilityStatus = findViewById(R.id.tvAccessibilityStatus)
        btnEnableAccessibility = findViewById(R.id.btnEnableAccessibility)

        layoutSetPassword = findViewById(R.id.layoutSetPassword)
        etNewPassword = findViewById(R.id.etNewPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnSetPassword = findViewById(R.id.btnSetPassword)
        tvPasswordStatus = findViewById(R.id.tvPasswordStatus)
        btnResetPassword = findViewById(R.id.btnResetPassword)

        timePickerStart.setIs24HourView(true)
        timePickerEnd.setIs24HourView(true)

        loadAppList()
        refreshRuleList()
        refreshPasswordUi()

        btnEnableAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        findViewById<Button>(R.id.btnAutostart).setOnClickListener { openAutostartSettings() }
        findViewById<Button>(R.id.btnBatteryOptimization).setOnClickListener { openBatteryOptimizationSettings() }
        findViewById<Button>(R.id.btnAppInfo).setOnClickListener { openAppInfoSettings() }

        btnSetPassword.setOnClickListener { onSetPasswordClicked() }
        btnResetPassword.setOnClickListener { onResetPasswordClicked() }

        findViewById<Button>(R.id.btnAddRule).setOnClickListener { onAddRuleClicked() }

        listRules.setOnItemLongClickListener { _, _, position, _ ->
            Prefs.removeRuleAt(this, position)
            refreshRuleList()
            Toast.makeText(this, "已删除该规则", Toast.LENGTH_SHORT).show()
            true
        }
    }

    override fun onResume() {
        super.onResume()
        refreshAccessibilityStatus()
        refreshPasswordUi()
    }

    private fun loadAppList() {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos = pm.queryIntentActivities(intent, 0)
        appList = resolveInfos
            .map { AppInfo(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
            .filter { it.packageName != packageName }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, appList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerApps.adapter = adapter
    }

    private fun refreshRuleList() {
        val rules = Prefs.getRules(this)
        val display = rules.map { r ->
            "${r.label}  ${fmt(r.startMinute)} - ${fmt(r.endMinute)}"
        }
        listRules.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, display)
    }

    private fun fmt(minutesOfDay: Int): String {
        val h = minutesOfDay / 60
        val m = minutesOfDay % 60
        return String.format("%02d:%02d", h, m)
    }

    private fun onAddRuleClicked() {
        val selected = spinnerApps.selectedItem as? AppInfo
        if (selected == null) {
            Toast.makeText(this, "请选择一个应用", Toast.LENGTH_SHORT).show()
            return
        }
        val startMinute = timePickerStart.hour * 60 + timePickerStart.minute
        val endMinute = timePickerEnd.hour * 60 + timePickerEnd.minute
        if (startMinute == endMinute) {
            Toast.makeText(this, "开始和结束时间不能相同", Toast.LENGTH_SHORT).show()
            return
        }
        Prefs.addRule(this, Rule(selected.packageName, selected.label, startMinute, endMinute))
        refreshRuleList()
        Toast.makeText(this, "已添加规则", Toast.LENGTH_SHORT).show()
    }

    private fun onSetPasswordClicked() {
        val p1 = etNewPassword.text.toString()
        val p2 = etConfirmPassword.text.toString()
        if (p1.length < 4) {
            Toast.makeText(this, "密码至少 4 位", Toast.LENGTH_SHORT).show()
            return
        }
        if (p1 != p2) {
            Toast.makeText(this, "两次输入的密码不一致", Toast.LENGTH_SHORT).show()
            return
        }
        val salt = PasswordUtil.generateSalt()
        val hash = PasswordUtil.hash(p1, salt)
        Prefs.setPassword(this, salt, hash)
        etNewPassword.text.clear()
        etConfirmPassword.text.clear()
        Toast.makeText(
            this,
            "密码已设置。请立刻把密码写在纸上或交给你信任的人保管——App 本身不会再显示这个密码。",
            Toast.LENGTH_LONG
        ).show()
        refreshPasswordUi()
    }

    private fun onResetPasswordClicked() {
        if (!Prefs.canResetPassword(this)) {
            Toast.makeText(this, "冷静期尚未结束，暂时无法重置", Toast.LENGTH_SHORT).show()
            return
        }
        Prefs.resetPassword(this)
        Toast.makeText(this, "密码已重置，请设置新密码", Toast.LENGTH_LONG).show()
        refreshPasswordUi()
    }

    private fun refreshPasswordUi() {
        val isSet = Prefs.isPasswordSet(this)
        layoutSetPassword.visibility = if (isSet) android.view.View.GONE else android.view.View.VISIBLE
        tvPasswordStatus.visibility = if (isSet) android.view.View.VISIBLE else android.view.View.GONE
        tvPasswordStatus.text = if (isSet) "密码状态：已设置（无法查看明文）" else ""

        val canReset = Prefs.canResetPassword(this)
        val requested = Prefs.getForgotRequestedAt(this) != 0L
        btnResetPassword.visibility = if (isSet && canReset) android.view.View.VISIBLE else android.view.View.GONE

        if (isSet && requested && !canReset) {
            val remainMs = Prefs.COOLDOWN_MILLIS - (System.currentTimeMillis() - Prefs.getForgotRequestedAt(this))
            val remainHours = (remainMs / (60 * 60 * 1000)).coerceAtLeast(0)
            tvPasswordStatus.text = "密码状态：已设置。你申请了忘记密码，还需等待约 $remainHours 小时才能重置。"
        }
    }

    private fun refreshAccessibilityStatus() {
        val enabled = isAccessibilityServiceEnabled()
        tvAccessibilityStatus.text = if (enabled) {
            "无障碍权限：已开启 ✓"
        } else {
            "无障碍权限：未开启，规则不会生效，请点击下方按钮开启"
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        val target = "$packageName/${AppLockAccessibilityService::class.java.name}"
        // Fallback string-based check via Settings, more reliable across OEMs
        val enabledStr = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        return enabledStr.split(":").any { it.equals(target, ignoreCase = true) } ||
            enabledServices.any { it.id.equals(target, ignoreCase = true) }
    }

    private fun openAutostartSettings() {
        // MIUI / HyperOS keep autostart management in Security app, but the exact
        // component name has changed across versions, so we try a few known ones
        // and fall back to the generic App Info page if none resolve.
        val candidates = listOf(
            Intent().apply {
                component = android.content.ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            },
            Intent().apply {
                component = android.content.ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.securitycenter.Main"
                )
            },
            Intent("miui.intent.action.OP_AUTO_START").apply {
                addCategory(Intent.CATEGORY_DEFAULT)
            }
        )
        for (i in candidates) {
            try {
                startActivity(i)
                Toast.makeText(
                    this,
                    "请在列表中找到 ASC，把自启动开关打开",
                    Toast.LENGTH_LONG
                ).show()
                return
            } catch (e: Exception) {
                // try next candidate
            }
        }
        Toast.makeText(this, "未能自动跳转，请手动到「设置 → 应用设置 → 应用管理 → ASC → 自启动」开启", Toast.LENGTH_LONG).show()
        openAppInfoSettings()
    }

    private fun openBatteryOptimizationSettings() {
        try {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATIONS_SETTINGS))
            Toast.makeText(
                this,
                "找到 ASC，把耗电策略设置为「无限制」",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Toast.makeText(this, "未能自动跳转，请手动到「设置 → 电池与性能 → 应用配置 → ASC」设为无限制", Toast.LENGTH_LONG).show()
            openAppInfoSettings()
        }
    }

    private fun openAppInfoSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开应用详情页", Toast.LENGTH_SHORT).show()
        }
    }
}
