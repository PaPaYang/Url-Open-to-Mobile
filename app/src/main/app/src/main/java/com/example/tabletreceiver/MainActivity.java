package com.example.tabletreceiver;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.widget.TextView;
import fi.iki.elonen.NanoHTTPD;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {

    private WebServer server;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView textView = new TextView(this);
        textView.setTextSize(22);
        textView.setPadding(60, 60, 60, 60);
        setContentView(textView);

        String ipAddress = getWifiIpAddress();
        textView.setText("Tablet Receiver 실행 중\n\n태블릿 IP 주소:\n" + ipAddress + "\n\n포트: 8080");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    server = new WebServer(8080);
                    server.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private String getWifiIpAddress() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int ip = wifiInfo.getIpAddress();
            if (ip == 0) return "Wi-Fi 미연결";
            return String.format("%d.%d.%d.%d",
                    (ip & 0xff),
                    (ip >> 8 & 0xff),
                    (ip >> 16 & 0xff),
                    (ip >> 24 & 0xff));
        }
        return "IP 불러오기 실패";
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
