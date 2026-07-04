package com.asc.selfcontrol

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.asc.selfcontrol.data.Prefs
import java.util.Calendar

class AppLockAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return // ignore ourselves

        val rules = Prefs.getRules(this).filter { it.packageName == pkg }
        if (rules.isEmpty()) return

        val now = Calendar.getInstance()
        val nowMinute = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        val activeRule = rules.firstOrNull { it.isLockedAt(nowMinute) } ?: return

        // Already unlocked recently? let it through.
        if (Prefs.isInGrace(this, pkg)) return

        // Kick the user back to the home screen, then show the lock screen on top.
        performGlobalAction(GLOBAL_ACTION_HOME)

        val lockIntent = Intent(this, LockOverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(LockOverlayActivity.EXTRA_PACKAGE_NAME, activeRule.packageName)
            putExtra(LockOverlayActivity.EXTRA_LABEL, activeRule.label)
        }
        startActivity(lockIntent)
    }

    override fun onInterrupt() {
        // no-op
    }
}
