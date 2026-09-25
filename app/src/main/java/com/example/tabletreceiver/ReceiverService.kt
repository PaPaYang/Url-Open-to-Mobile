package com.example.tabletreceiver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket

class ReceiverService : Service() {

    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        
        // 백그라운드 절전 방지 (WakeLock & WifiLock)
        acquireLocks()
        
        startForegroundServiceNotification()
        startServer()
    }

    private fun acquireLocks() {
        // CPU 슬립 방지
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "TabletReceiver::WakeLock"
        )
        wakeLock?.acquire(10 * 60 * 1000L /* 10분 후 자동해제 예방용 재갱신 구조 */)

        // 화면 켜진 상태가 아니어도 Wi-Fi 연결 유지
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiLock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.DEX) {
            wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "TabletReceiver::WifiLock")
        } else {
            wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "TabletReceiver::WifiLock")
        }
        wifiLock?.acquire()
    }

    private fun startForegroundServiceNotification() {
        val channelId = "tablet_receiver_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Tablet Receiver Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Tablet Receiver 실행 중")
            .setContentText("백그라운드에서 수신 대기 중 (포트 8080)")
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    private fun startServer() {
        isRunning = true
        Thread {
            try {
                serverSocket = ServerSocket(8080)
                while (isRunning) {
                    val socket = serverSocket?.accept() ?: break
                    handleClient(socket)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun handleClient(socket: Socket) {
        Thread {
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val requestLine = reader.readLine()

                if (requestLine != null) {
                    val url = parseUrlFromRequest(requestLine)
                    if (!url.isNullOrEmpty()) {
                        openBrowser(url)
                    }
                }

                val output = socket.getOutputStream()
                val response = "HTTP/1.1 200 OK\r\nAccess-Control-Allow-Origin: *\r\nContent-Type: text/plain; charset=UTF-8\r\n\r\nOK"
                output.write(response.toByteArray())
                output.flush()
                socket.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun parseUrlFromRequest(requestLine: String): String? {
        val parts = requestLine.split(" ")
        if (parts.size >= 2) {
            val path = parts[1]
            if (path.contains("url=")) {
                val rawUrl = path.substringAfter("url=")
                return Uri.decode(rawUrl)
            } else if (path.startsWith("http://") || path.startsWith("https://")) {
                return path
            }
        }
        return null
    }

    private fun openBrowser(url: String) {
        val targetUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "https://$url"
        } else {
            url
        }

        // 화면이 꺼져 있을 때 화면을 깨우고 앱을 전면에 띄우는 Intent Flag 설정
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
            if (wifiLock?.isHeld == true) wifiLock?.release()
            serverSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
