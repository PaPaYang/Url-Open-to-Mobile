package com.example.safeapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.io.OutputStream
import java.net.Socket
import java.util.concurrent.Executors

class SafeNetworkService : Service() {

    private val executor = Executors.newSingleThreadExecutor()
    private val CHANNEL_ID = "safe_service_channel"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 1. Android 16 포그라운드 서비스 크래시 방지: startForeground 필수 즉시 호출
        try {
            val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("안전한 네트워크 서비스")
                .setContentText("동작 중입니다...")
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

            startForeground(1001, notification)
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
            return START_NOT_STICKY
        }

        // 2. 비동기 쓰레드로 네트워킹 작업 수행 (NetworkOnMainThreadException 방지)
        executor.execute {
            runSocketClient()
        }

        return START_STICKY
    }

    private fun runSocketClient() {
        var socket: Socket? = null
        try {
            // 테스트용 IP 및 Port (실제 서버 설정으로 변경)
            socket = Socket("127.0.0.1", 8080)
            val output: OutputStream = socket.getOutputStream()
            output.write("Hello Android 16".toByteArray())
            output.flush()
        } catch (e: Exception) {
            // SocketTimeoutException, IOException 등 세부 예외 포착
            e.printStackTrace()
        } finally {
            try {
                socket?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Safe Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
                val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}
