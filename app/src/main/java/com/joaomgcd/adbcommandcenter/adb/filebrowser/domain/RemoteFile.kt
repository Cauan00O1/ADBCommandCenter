package com.joaomgcd.adbcommandcenter.adb.filebrowser.domain

data class RemoteFile(
    val name: String,
    val path: String,
    val isDirectory: Boolean
)