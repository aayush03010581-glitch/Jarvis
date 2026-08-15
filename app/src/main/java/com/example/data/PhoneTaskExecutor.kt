package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings

object PhoneTaskExecutor {
    fun executeTask(context: Context, command: String): Boolean {
        val lower = command.lowercase().trim()
        val intent: Intent? = when {
            lower.contains("camera") || lower.contains("photo") || lower.contains("picture") -> {
                Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            }
            lower.contains("browser") || lower.contains("google") || lower.contains("internet") || lower.contains("web") -> {
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
            }
            lower.contains("settings") || lower.contains("setting") -> {
                Intent(Settings.ACTION_SETTINGS)
            }
            lower.contains("map") || lower.contains("navigation") || lower.contains("gps") -> {
                Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=current+location"))
            }
            lower.contains("dial") || lower.contains("phone") || lower.contains("call") -> {
                Intent(Intent.ACTION_DIAL)
            }
            lower.contains("gallery") || lower.contains("photos") -> {
                Intent(Intent.ACTION_VIEW, Uri.parse("content://media/internal/images/media"))
            }
            lower.contains("calculator") -> {
                Intent().apply {
                    action = Intent.ACTION_MAIN
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    setPackage("com.android.calculator2")
                }
            }
            lower.contains("wifi") -> {
                Intent(Settings.ACTION_WIFI_SETTINGS)
            }
            lower.contains("bluetooth") -> {
                Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            }
            else -> null
        }

        return if (intent != null) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } catch (e: Exception) {
                try {
                    val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(fallback)
                    true
                } catch (ex: Exception) {
                    false
                }
            }
        } else {
            if (lower.startsWith("open ") || lower.startsWith("launch ")) {
                val appName = lower.removePrefix("open ").removePrefix("launch ").trim()
                try {
                    val pm = context.packageManager
                    val launchIntent = pm.getLaunchIntentForPackage(appName)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                        true
                    } else {
                        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$appName+app")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(webIntent)
                        true
                    }
                } catch (e: Exception) {
                    false
                }
            } else {
                false
            }
        }
    }
}
