package com.example.tabletreceiver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket

class ReceiverService : Service() {

    private var serverSocket: ServerSocket? = null
    private var isRunning = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceNotification()
        startServer()
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
            .setContentText("모바일 수신 대기 중 (포트 8080)")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
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
                    // HTTP GET /?url=https://... 또는 원문 URL 추출
                    val url = parseUrlFromRequest(requestLine)
                    if (!url.isNullOrEmpty()) {
                        openBrowser(url)
                    }
                }

                // HTTP 응답 반환
                val output = socket.getOutputStream()
                val response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain; charset=UTF-8\r\n\r\nOK"
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

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
