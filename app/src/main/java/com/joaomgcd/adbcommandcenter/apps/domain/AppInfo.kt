package com.joaomgcd.adbcommandcenter.apps.domain

import android.graphics.drawable.Drawable

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable
)