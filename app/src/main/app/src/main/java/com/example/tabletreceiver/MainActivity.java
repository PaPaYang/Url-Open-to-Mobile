package com.example.tabletreceiver;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

public class MainActivity extends Activity {

    private ServerSocket serverSocket;
    private Thread serverThread;
    private volatile boolean isRunning = false;
    private TextView textView;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 순수 자바 코드로 UI 생성 (XML 리소스 로딩 오류 사전 차단)
        textView = new TextView(this);
        textView.setTextSize(18f);
        textView.setPadding(60, 60, 60, 60);
        setContentView(textView);

        String ipAddress = getLocalIpAddress();
        textView.setText("Tablet Receiver 실행 중\n\n기기 IP 주소:\n" + ipAddress + "\n\n포트: 8080");

        startHttpServer();
    }

    private void startHttpServer() {
        isRunning = true;
        serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(8080);
                while (isRunning && !Thread.currentThread().isInterrupted()) {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(() -> handleClientRequest(clientSocket)).start();
                }
            } catch (Exception e) {
                if (isRunning) {
                    final String errorMsg = e.getMessage();
                    mainHandler.post(() -> {
                        if (textView != null) {
                            textView.setText("서버 실행 오류:\n" + errorMsg);
                        }
                    });
                }
            }
        });
        serverThread.start();
    }

    private void handleClientRequest(Socket socket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             OutputStream out = socket.getOutputStream()) {

            String requestLine = reader.readLine();
            if (requestLine != null && requestLine.startsWith("GET")) {
                int urlIndex = requestLine.indexOf("url=");
                if (urlIndex != -1) {
                    String rawUrl = requestLine.substring(urlIndex + 4);
                    int endSpace = rawUrl.indexOf(' ');
                    if (endSpace != -1) {
                        rawUrl = rawUrl.substring(0, endSpace);
                    }

                    final String targetUrl = URLDecoder.decode(rawUrl, "UTF-8");

                    mainHandler.post(() -> {
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
            }

            String httpResponse = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/plain; charset=utf-8\r\n" +
                    "Access-Control-Allow-Origin: *\r\n" +
                    "Content-Length: 2\r\n\r\n" +
                    "OK";
            out.write(httpResponse.getBytes(StandardCharsets.UTF_8));
            out.flush();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (Exception ignored) {}
        }
    }

    private String getLocalIpAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String ip = addr.getHostAddress();
                        if (ip != null && ip.indexOf(':') < 0) {
                            return ip;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return "Wi-Fi 연결을 확인해주세요";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isRunning = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (Exception ignored) {}
        }
        if (serverThread != null) {
            serverThread.interrupt();
        }
    }
}
