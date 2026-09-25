package com.example.tabletreceiver

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.net.NetworkInterface
import java.util.Collections

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 수신 백그라운드 서비스 시작
        val serviceIntent = Intent(this, ReceiverService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        // 메인 루트 레이아웃
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F5F7FA"))
            setPadding(dpToPx(24), dpToPx(32), dpToPx(24), dpToPx(32))
        }

        // 타이틀
        val titleView = TextView(this).apply {
            text = "Tablet Receiver"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1A1D20"))
            setPadding(0, 0, 0, dpToPx(24))
        }
        rootLayout.addView(titleView)

        // 카드 컨테이너
        val cardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dpToPx(16).toFloat()
                setStroke(dpToPx(1), Color.parseColor("#E2E8F0"))
            }
        }

        cardLayout.addView(createLabelTextView("서버 상태"))
        val statusValue = TextView(this).apply {
            text = "● 실행 중 (Listening)"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#10B981"))
            setPadding(0, dpToPx(4), 0, dpToPx(16))
        }
        cardLayout.addView(statusValue)

        cardLayout.addView(createLabelTextView("IP 주소"))
        val ipAddress = getLocalIpAddress()
        val ipValue = TextView(this).apply {
            text = ipAddress
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
            setTextColor(Color.parseColor("#2563EB"))
            setPadding(0, dpToPx(4), 0, dpToPx(16))
        }
        cardLayout.addView(ipValue)

        cardLayout.addView(createLabelTextView("포트 번호"))
        val portValue = TextView(this).apply {
            text = "8080"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            setPadding(0, dpToPx(4), 0, dpToPx(16))
        }
        cardLayout.addView(portValue)

        // 테스트 동작 확인용 버튼
        val testButton = Button(this).apply {
            text = "태블릿 테스트 (Google 열기)"
            setBackgroundColor(Color.parseColor("#2563EB"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
                startActivity(intent)
            }
        }
        cardLayout.addView(testButton)

        rootLayout.addView(cardLayout)

        val guideView = TextView(this).apply {
            text = "모바일/PC 브라우저에서 아래 주소로 접속해 테스트하세요:\nhttp://$ipAddress:8080/?url=https://www.google.com"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(Color.parseColor("#64748B"))
            gravity = Gravity.CENTER
            setPadding(0, dpToPx(24), 0, 0)
        }
        rootLayout.addView(guideView)

        setContentView(rootLayout)
    }

    private fun createLabelTextView(labelText: String): TextView {
        return TextView(this).apply {
            text = labelText
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(Color.parseColor("#64748B"))
        }
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress) {
                        val ip = addr.hostAddress
                        if (ip != null && !ip.contains(":")) {
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

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
