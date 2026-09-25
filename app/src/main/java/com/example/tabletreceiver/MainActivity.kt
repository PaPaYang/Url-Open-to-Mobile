package com.example.tabletreceiver

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.net.NetworkInterface
import java.util.Collections

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 메인 루트 레이아웃 설정
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F5F7FA"))
            setPadding(dpToPx(24), dpToPx(32), dpToPx(24), dpToPx(32))
        }

        // 상단 타이틀
        val titleView = TextView(this).apply {
            text = "Tablet Receiver"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1A1D20"))
            setPadding(0, 0, 0, dpToPx(24))
        }
        rootLayout.addView(titleView)

        // 카드 컨테이너 레이아웃
        val cardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dpToPx(16).toFloat()
                setStroke(dpToPx(1), Color.parseColor("#E2E8F0"))
            }
        }

        // 서버 상태 항목
        cardLayout.addView(createLabelTextView("서버 상태"))
        val statusValue = TextView(this).apply {
            text = "● 실행 중 (Listening)"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#10B981"))
            setPadding(0, dpToPx(4), 0, dpToPx(16))
        }
        cardLayout.addView(statusValue)

        // IP 주소 항목
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

        // 포트 번호 항목
        cardLayout.addView(createLabelTextView("포트 번호"))
        val portValue = TextView(this).apply {
            text = "8080"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            setPadding(0, dpToPx(4), 0, 0)
        }
        cardLayout.addView(portValue)

        rootLayout.addView(cardLayout)

        // 하단 안내 메시지
        val guideView = TextView(this).apply {
            text = "모바일 앱에서 위 IP 주소와 포트 번호를 입력하여 연결하세요."
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
