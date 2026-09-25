package com.example.tabletreceiver;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
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

        TextView textView = new TextView(this);
        textView.setTextSize(18);
        textView.setPadding(60, 60, 60, 60);
        setContentView(textView);

        // 백그라운드 포그라운드 서비스 실행
        Intent serviceIntent = new Intent(this, SafeNetworkService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        String ipAddress = getIPAddress();
        textView.setText("Tablet Receiver 서비스가 백그라운드에서 실행되었습니다.\n\n이 앱을 닫아도 백그라운드에서 수신 가능합니다.\n\n태블릿 IP 주소:\n" + ipAddress + "\n\n포트: 8080");
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
}
