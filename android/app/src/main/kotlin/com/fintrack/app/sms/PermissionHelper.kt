package com.fintrack.app.sms

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat

/**
 * Helps the user grant SMS access on phones that block it for apps installed outside a store
 * (Android "restricted settings", Xiaomi HyperOS/MIUI, etc.), and keep the receiver alive.
 */
object PermissionHelper {
    val SMS_PERMISSIONS = arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)

    val isXiaomi: Boolean
        get() = Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true) ||
            Build.BRAND.lowercase() in setOf("xiaomi", "redmi", "poco")

    /** True when asking again would show the system dialog; false once the user/OS has blocked it. */
    fun canAskAgain(activity: Activity): Boolean =
        SMS_PERMISSIONS.any { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }

    /** Opens the screen where SMS can be allowed (Xiaomi's permission editor when available). */
    fun openSmsSettings(context: Context) {
        if (isXiaomi && tryStart(context, Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity")
                putExtra("extra_pkgname", context.packageName)
            })
        ) return
        openAppInfo(context)
    }

    /** Standard App info page: holds the ⋮ > "Allow restricted settings" menu. */
    fun openAppInfo(context: Context) {
        tryStart(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
    }

    /** Xiaomi kills background receivers unless Autostart is on; elsewhere this opens App info. */
    fun openAutostartSettings(context: Context) {
        if (isXiaomi && tryStart(context, Intent().setComponent(
                ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
            ))
        ) return
        openAppInfo(context)
    }

    private fun tryStart(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
