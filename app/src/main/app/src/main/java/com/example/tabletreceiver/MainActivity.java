package com.example.tabletreceiver;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ServerSocket serverSocket;
    private boolean isRunning = false;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        textView = new TextView(this);
        textView.setTextSize(20);
        textView.setPadding(60, 60, 60, 60);
        setContentView(textView);

        String ipAddress = getIPAddress();
        textView.setText("Tablet Receiver 실행 중\n\n기기 IP 주소:\n" + ipAddress + "\n\n포트: 8080");

        startServer();
    }

    private void startServer() {
        isRunning = true;
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(8080);
                while (isRunning) {
                    Socket socket = serverSocket.accept();
                    handleClient(socket);
                }
            } catch (Exception e) {
                if (isRunning) {
                    new Handler(Looper.getMainLooper()).post(() ->
                        textView.setText("서버 실행 오류:\n" + e.getMessage())
                    );
                }
            }
        }).start();
    }

    private void handleClient(Socket socket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             OutputStream out = socket.getOutputStream()) {

            String line = reader.readLine();
            if (line != null && line.startsWith("GET /open")) {
                int urlStart = line.indexOf("url=");
                if (urlStart != -1) {
                    String urlParam = line.substring(urlStart + 4);
                    int spaceIndex = urlParam.indexOf(" ");
                    if (spaceIndex != -1) {
                        urlParam = urlParam.substring(0, spaceIndex);
                    }
                    String targetUrl = URLDecoder.decode(urlParam, "UTF-8");

                    // URL 열기 실행
                    new Handler(Looper.getMainLooper()).post(() -> {
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

            // HTTP Response
            String response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: 2\r\n\r\nOK";
            out.write(response.getBytes("UTF-8"));
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { socket.close(); } catch (Exception ignored) {}
        }
    }

    private String getIPAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        if (sAddr != null && sAddr.indexOf(':') < 0) {
                            return sAddr;
                        }
                    }
                }
            }
        } catch (Exception ignored) { }
        return "Wi-Fi 연결 확인 필요";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isRunning = false;
        if (serverSocket != null) {
            try { serverSocket.close(); } catch (Exception ignored) {}
        }
    }
}
