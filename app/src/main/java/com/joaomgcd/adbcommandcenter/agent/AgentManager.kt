//package com.joaomgcd.adbcommandcenter.agent
//
//import android.content.Context
//import android.util.Log
//import com.joaomgcd.adbcommandcenter.adb.connection.domain.RunActiveAdbShellCommandUseCase
//import dagger.hilt.android.qualifiers.ApplicationContext
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//import javax.inject.Inject
//import javax.inject.Singleton
//
//private const val TAG = "AgentManager"
//private fun String.debug() = Log.d(TAG, this)
//private fun String.warn() = Log.w(TAG, this)
//
//@Singleton
//class AgentManager @Inject constructor(
//    @param:ApplicationContext private val context: Context,
//    private val runShellCommand: RunActiveAdbShellCommandUseCase,
//    private val agentClient: AgentClient
//) {
//
//    suspend fun startAgent(): Result<Unit> = withContext(Dispatchers.IO) {
//        if (agentClient.isAgentAlive()) {
//            "Agent is already running.".debug()
//            return@withContext Result.success(Unit)
//        }
//        "Bootstrapping Agent via ADB...".debug()
//

//        val className = "com.joaomgcd.adbcommandcenter.agent.ShellAgent"
//        try {
//            Class.forName(className)
//        } catch (e: ClassNotFoundException) {
//            return@withContext Result.failure(Exception("ShellAgent class is missing from the APK. Perform a full Clean/Reinstall."))
//        }
//
//        val appInfo = context.applicationInfo
//        val apkPaths = mutableListOf<String>()
//        apkPaths.add(appInfo.sourceDir) // Base APK
//        appInfo.splitSourceDirs?.let { apkPaths.addAll(it) } // Split APKs
//
//        val classPath = apkPaths.joinToString(":")
//
//        val cmd = "export CLASSPATH=$classPath; nohup app_process /system/bin $className < /dev/null > /dev/null 2>&1 &"
//
//        runShellCommand(cmd)
//
//        Thread.sleep(1500)
//
//        if (agentClient.isAgentAlive()) {
//            "Agent bootstrapped successfully!".debug()
//            return@withContext Result.success(Unit)
//        }
//
//        "Agent failed to connect. Running diagnostic start...".warn()
//
//        val diagCmd = "export CLASSPATH=$classPath; app_process /system/bin $className"
//        val diagnosticResult = runShellCommand(diagCmd)
//
//        val errorMsg = diagnosticResult.exceptionOrNull()?.message
//            ?: diagnosticResult.getOrNull()
//            ?: "Unknown error. Ensure you have performed a FULL INSTALL (Uninstall -> Clean -> Run)."
//
//        return@withContext Result.failure(Exception("Agent failed to start.\nDiagnostic Output: $errorMsg"))
//    }
//
//    suspend fun stopAgent() {
//        try {
//            agentClient.runCommand("AGENT_EXIT")
//        } catch (e: Exception) {
//        }
//    }
//}