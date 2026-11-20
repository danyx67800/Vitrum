package com.vitrum.tv.ota

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.widget.Toast
import com.vitrum.tv.R

class InstallResultReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            val confirmation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_INTENT) as? Intent
            }
            if (confirmation != null) {
                confirmation.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(confirmation) }
            }
            return
        }
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        context.getSharedPreferences("vitrum_ota", Context.MODE_PRIVATE).edit()
            .putInt(KEY_INSTALL_STATUS, status)
            .putString(KEY_INSTALL_MESSAGE, message)
            .apply()
        val text = when (status) {
            PackageInstaller.STATUS_SUCCESS -> R.string.install_success
            PackageInstaller.STATUS_FAILURE_ABORTED -> R.string.install_cancelled
            else -> R.string.install_failed_generic
        }
        Toast.makeText(context, context.getString(text), Toast.LENGTH_LONG).show()
    }

    companion object {
        const val EXTRA_SESSION_ID = "session_id"
        const val KEY_INSTALL_STATUS = "last_install_status"
        const val KEY_INSTALL_MESSAGE = "last_install_message"
    }
}
