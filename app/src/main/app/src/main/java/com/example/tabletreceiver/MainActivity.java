package com.example.tabletreceiver;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import fi.iki.elonen.NanoHTTPD;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {

    private WebServer server;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        textView = new TextView(this);
        textView.setTextSize(20);
        textView.setPadding(60, 60, 60, 60);
        setContentView(textView);

        String ipAddress = getIPAddress();
        textView.setText("수신 서버 실행 중...\n\n기기 IP 주소:\n" + ipAddress + "\n\n포트: 8080");

        // 메인 스레드 멈춤 방지
        new Thread(() -> {
            try {
                server = new WebServer(8080);
                server.start();
            } catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() -> 
                    textView.setText("서버 실행 실패:\n" + e.getMessage())
                );
            }
        }).start();
    }

    // Wi-Fi 및 모바일/핫스팟 IP 모두 안전하게 감지
    private String getIPAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        if (sAddr != null && sAddr.indexOf(':') < 0) { // IPv4만 추출
                            return sAddr;
                        }
                    }
                }
            }
        } catch (Exception ignored) { }
        return "IP 주소를 찾을 수 없음 (Wi-Fi 연결 확인)";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (server != null) {
            server.stop();
        }
    }

    private class WebServer extends NanoHTTPD {
        public WebServer(int port) {
            super(port);
        }

        @Override
        public Response serve(IHTTPSession session) {
            if ("/open".equals(session.getUri())) {
                Map<String, List<String>> params = session.getParameters();
                if (params != null && params.containsKey("url")) {
                    List<String> urls = params.get("url");
                    if (urls != null && !urls.isEmpty()) {
                        String url = urls.get(0);
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "OK");
                    }
                }
            }
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found");
        }
    }
}
