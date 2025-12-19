package com.joaomgcd.adbcommandcenter.adb.install.app.domain


data class InstallableFileInfo(
    val fileName: String,
    val filePath: String,
    val fileSizeInBytes: Long
)