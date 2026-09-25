package com.example.tabletreceiver

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.net.NetworkInterface
import java.util.Collections

class MainActivity : AppCompatActivity() { // <- 클래스 시작 확인

    override fun onCreate(savedInstanceState: Bundle?) { // protected -> override fun 사용
        super.onCreate(savedInstanceState)

        val serviceIntent = Intent(this, YourService::class.java) // 실제 Service 클래스명으로 변경
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val titleView = TextView(this).apply {
            text = "Tablet Receiver"
        }
        layout.addView(titleView)

        val card = LinearLayout(this)
        val ipAddress = getLocalIpAddress()

        val statusLabel = TextView(this)
        card.addView(statusLabel)

        val ipLabel = TextView(this)
        card.addView(ipLabel)

        val ipValue = TextView(this).apply {
            text = ipAddress
        }
        card.addView(ipValue)

        val portLabel = TextView(this)
        card.addView(portLabel)

        layout.addView(card)

        val guideView = TextView(this)
        layout.addView(guideView)

        setContentView(layout)
    }

    private fun getLocalIpAddress(): String {
        try {
            // Java Enumeration을 Kotlin List로 변환하여 순회
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress) {
                        val ip = addr.hostAddress
                        if (ip != null && !ip.contains(":")) { // IPv4 체크
                            return ip
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "127.0.0.1"
    }
} // <- 클래스 닫는 괄호 확인
