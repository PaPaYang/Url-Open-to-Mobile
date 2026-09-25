package com.example.tabletreceiver;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 백그라운드 서비스 시작
        Intent serviceIntent = new Intent(this, SafeNetworkService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        // 레이아웃 구성
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 80, 60, 80);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.setBackgroundColor(Color.parseColor("#F5F7FA"));

        TextView titleView = new TextView(this);
        titleView.setText("Tablet Receiver");
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setTextColor(Color.parseColor("#1A1F36"));
        titleView.setPadding(0, 0, 0, 40);
        layout.addView(titleView);

        // IP 표시 카드 레이아웃
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(50, 50, 50, 50);
        card.setBackgroundColor(Color.WHITE);

        String ipAddress = getLocalIpAddress();

        TextView statusLabel = new TextView(this);
        statusLabel.setText("🟢 백그라운드 수신 중 (자동 실행 됨)");
        statusLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        statusLabel.setTextColor(Color.parseColor("#2E7D32"));
        statusLabel.setTypeface(null, Typeface.BOLD);
        card.addView(statusLabel);

        TextView ipLabel = new TextView(this);
        ipLabel.setText("\n태블릿 내부 IP 주소:");
        ipLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        ipLabel.setTextColor(Color.parseColor("#6B7280"));
        card.addView(ipLabel);

        TextView ipValue = new TextView(this);
        ipValue.setText(ipAddress);
        ipValue.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        ipValue.setTypeface(null, Typeface.BOLD);
        ipValue.setTextColor(Color.parseColor("#2563EB"));
        card.addView(ipValue);

        TextView portLabel = new TextView(this);
        portLabel.setText("\n포트: 8080");
        portLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        portLabel.setTextColor(Color.parseColor("#4B5563"));
        card.addView(portLabel);

        layout.addView(card);

        TextView guideView = new TextView(this);
        guideView.setText("\n\n크롬 확장프로그램 옵션에서 위 IP 주소를 입력해 주세요.\n태블릿을 재부팅해도 서비스가 자동 실행됩니다.");
        guideView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        guideView.setTextColor(Color.parseColor("#6B7280"));
        guideView.setGravity(Gravity.CENTER);
        layout.addView(guideView);

        setContentView(layout);
    }

    private String getLocalIpAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String ip = addr.getHostAddress();
                        if (ip != null && ip.indexOf(':') < 0) { // IPv4만 추출
                            return ip;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return "Wi-Fi 연결을 확인해주세요";
    }
}
