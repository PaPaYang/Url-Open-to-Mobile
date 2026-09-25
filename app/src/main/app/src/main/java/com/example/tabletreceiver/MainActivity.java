package com.example.tabletreceiver;

import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import fi.iki.elonen.NanoHTTPD;
import java.io.IOException;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private WebServer server;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. 화면에 IP 및 상태를 표시할 뷰 생성
        TextView textView = new TextView(this);
        textView.setTextSize(20);
        textView.setPadding(50, 50, 50, 50);
        setContentView(textView);

        String ipAddress = getWifiIpAddress();
        textView.setText("태블릿 수신 서버 동작 중...\n\n태블릿 IP 주소:\n" + ipAddress + "\n\n포트: 8080");

        // 2. 안드로이드 메인 스레드 튕김 방지를 위해 별도 스레드에서 서버 실행
        new Thread(() -> {
            try {
                server = new WebServer(8080);
                server.start();
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> textView.setText("서버 실행 실패: " + e.getMessage()));
            }
        }).start();
    }

    private String getWifiIpAddress() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int ip = wifiInfo.getIpAddress();
            if (ip == 0) return "Wi-Fi에 연결되어 있지 않음";
            return String.format(Locale.getDefault(), "%d.%d.%d.%d",
                    (ip & 0xff),
                    (ip >> 8 & 0xff),
                    (ip >> 16 & 0xff),
                    (ip >> 24 & 0xff));
        }
        return "IP를 불러올 수 없음";
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
                var params = session.getParameters();
                if (params.containsKey("url") && !params.get("url").isEmpty()) {
                    String url = params.get("url").get(0);

                    // 웹 브라우저 열기
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);

                    return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "OK");
                }
            }
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found");
        }
    }
}
