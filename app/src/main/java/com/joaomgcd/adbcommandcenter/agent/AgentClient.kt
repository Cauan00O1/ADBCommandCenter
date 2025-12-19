//package com.joaomgcd.adbcommandcenter.agent
//
//import android.util.Log
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//import java.io.BufferedReader
//import java.io.InputStreamReader
//import java.io.PrintWriter
//import java.net.Socket
//import javax.inject.Inject
//import javax.inject.Singleton
//
//private const val AGENT_PORT = 52999 // Arbitrary high port
//
//private const val TAG = "AgentClient"
//private fun String.debug() = Log.d(TAG, this)
//private fun String.info() = Log.i(TAG, this)
//private fun String.warn() = Log.w(TAG, this)
//private fun String.error(tr: Throwable? = null) = Log.e(TAG, this, tr)
//
//@Singleton
//class AgentClient @Inject constructor() {
//
//    suspend fun isAgentAlive(): Boolean = withContext(Dispatchers.IO) {
//        try {
//            val response = sendCommandInternal("AGENT_PING")
//            response.contains("PONG")
//        } catch (e: Exception) {
//            "Agent is not alive".error(e)
//            false
//        }
//    }
//
//    suspend fun runCommand(command: String): List<String> = withContext(Dispatchers.IO) {
//        sendCommandInternal(command)
//    }
//
//    private fun sendCommandInternal(command: String): List<String> {
//        "Sending command to ShellAgent: $command".debug()
//        val result = mutableListOf<String>()
//
//        try {
//            Socket("127.0.0.1", AGENT_PORT).use { socket ->
//                val output = PrintWriter(socket.getOutputStream(), true)
//                val input = BufferedReader(InputStreamReader(socket.getInputStream()))
//
//                output.println(command)
//
//                var line: String?
//                while (input.readLine().also { line = it } != null) {
//                    result.add(line!!)
//                }
//            }
//        } catch (e: Exception) {
//            "Couldn't send command".error(e)
//        }
//        return result
//    }
//}