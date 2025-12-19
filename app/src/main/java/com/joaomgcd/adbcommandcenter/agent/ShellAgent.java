//package com.joaomgcd.adbcommandcenter.agent;
//
//import java.io.BufferedReader;
//import java.io.FileOutputStream;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.io.OutputStream;
//import java.io.PrintStream;
//import java.io.PrintWriter;
//import java.net.InetAddress;
//import java.net.ServerSocket;
//import java.net.Socket;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//public class ShellAgent {
//    private static final String SOCKET_IP = "127.0.0.1";
//    private static final int AGENT_PORT = 52999;
//    private static final String LOG_PATH = "/data/local/tmp/agent_debug.log";
//
//    public static void main(String[] args) {

//        setupLogging();
//        System.out.println("Agent: Booting... UID: " + android.os.Process.myUid());
//
//        ExecutorService executor = Executors.newCachedThreadPool();
//
//        try (ServerSocket serverSocket = new ServerSocket(AGENT_PORT, 50, InetAddress.getByName(SOCKET_IP))) {
//            System.out.println("Agent: Listening on " + SOCKET_IP + ":" + AGENT_PORT);
//
//            while (true) {
//                try {
//                    final Socket clientSocket = serverSocket.accept();
//                    executor.submit(() -> handleClient(clientSocket));
//                } catch (Exception e) {
//                    System.err.println("Agent: Accept error: " + e.getMessage());
//                    break;
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            System.err.println("Agent: Fatal error: " + e.getMessage());
//        }
//    }
//
//    private static void setupLogging() {
//        try {
//            FileOutputStream fos = new FileOutputStream(LOG_PATH, false); // Overwrite mode
//            PrintStream ps = new PrintStream(fos, true);
//            System.setOut(ps);
//            System.setErr(ps);
//        } catch (Exception ignored) {

//        }
//    }
//

//    private static void handleClient(Socket socket) {
//        try (
//                Socket s = socket;
//                InputStream in = s.getInputStream();
//                OutputStream out = s.getOutputStream();
//                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
//                PrintWriter writer = new PrintWriter(out, true)
//        ) {
//            String command = reader.readLine();
//            if (command == null) return;
//
//            if ("AGENT_PING".equals(command)) {
//                writer.println("PONG");
//                return;
//            }
//            if ("AGENT_EXIT".equals(command)) {
//                writer.println("BYE");
//                System.exit(0);
//                return;
//            }
//
//            try {
//                Process process = Runtime.getRuntime().exec(command);
//                BufferedReader stdOut = new BufferedReader(new InputStreamReader(process.getInputStream()));
//                String line;
//                while ((line = stdOut.readLine()) != null) {
//                    writer.println(line);
//                }
//                BufferedReader stdErr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
//                while ((line = stdErr.readLine()) != null) {
//                    writer.println("ERR: " + line);
//                }
//                process.waitFor();
//                writer.println("EXIT_CODE: " + process.exitValue());
//            } catch (Exception e) {
//                writer.println("ERR: " + e.getMessage());
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//}