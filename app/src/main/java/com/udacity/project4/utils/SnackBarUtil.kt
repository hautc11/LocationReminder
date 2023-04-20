package com.udacity.project4.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.View
import androidx.core.content.ContextCompat.startActivity
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R

fun showSnackBarDirectToSettings(view: View, context: Context, message: String) {
    Snackbar.make(
        view,
        message,
        Snackbar.LENGTH_INDEFINITE
    )
        .setAction(R.string.settings) {
            val intent = Intent().apply {
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(context, intent, null)
        }
        .show()
}