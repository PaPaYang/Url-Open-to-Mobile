package com.example.tabletreceiver

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
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder

class SafeNetworkService : Service() {

    private var serverSocket: ServerSocket? = null
    @Volatile
    private var isRunning = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, "TABLET_RECEIVER_CHANNEL")
            .setContentTitle("Tablet Receiver")
            .setContentText("백그라운드에서 페이지 수신 대기 중...")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .build()

        startForeground(1, notification)

        if (!isRunning) {
            isRunning = true
            startServer()
        }

        return START_STICKY
    }

    private fun startServer() {
        Thread {
            try {
                serverSocket = ServerSocket(8080)
                while (isRunning) {
                    val socket = serverSocket?.accept()
                    socket?.let { handleSocket(it) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun handleSocket(socket: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), "UTF-8"))
            val out: OutputStream = socket.getOutputStream()
            val requestLine = reader.readLine()

            if (requestLine != null && requestLine.contains("/open")) {
                val urlStart = requestLine.indexOf("url=")
                if (urlStart != -1) {
                    var targetUrl = requestLine.substring(urlStart + 4)
                    val spaceIndex = targetUrl.indexOf(" ")
                    if (spaceIndex != -1) {
                        targetUrl = targetUrl.substring(0, spaceIndex)
                    }
                    val decodedUrl = URLDecoder.decode(targetUrl, "UTF-8")

                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(decodedUrl)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                }
            }

            val response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nConnection: close\r\n\r\nOK"
            out.write(response.toByteArray(Charsets.UTF_8))
            out.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try { socket.close() } catch (e: Exception) {}
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "TABLET_RECEIVER_CHANNEL",
                "Tablet Receiver",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        try { serverSocket?.close() } catch (e: Exception) {}
    }
}
