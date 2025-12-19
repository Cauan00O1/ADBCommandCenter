package com.joaomgcd.adbcommandcenter.adb.tasker.ui


sealed interface TaskerPluginConfigEvent {

    object SaveAndFinish : TaskerPluginConfigEvent
    object CancelAndFinish : TaskerPluginConfigEvent
}