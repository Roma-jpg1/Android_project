package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.IBinder
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import kotlin.coroutines.resume

class MyService : Service() {

    private val LOG_TAG = "BG_SERVICE"

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private var workerStarted = false

    companion object {
        const val ACTION_BG_UPDATE = "BackGroundUpdate"
        private const val CHANNEL_ID = "bg_service_channel"
        private const val NOTIFICATION_ID = 1001
        private const val LOOP_DELAY_MS = 4000L
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val notification = buildNotification("Сервис работает в фоне")

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        )

        Log.d(LOG_TAG, "Foreground service started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (workerStarted) {
            Log.d(LOG_TAG, "Worker already started")
            return START_STICKY
        }

        workerStarted = true

        serviceScope.launch {
            while (isActive) {
                try {
                    if (!checkLocationPermissions()) {
                        sendStatus("Нет location permission")
                        delay(LOOP_DELAY_MS)
                        continue
                    }

                    if (!isLocationEnabled()) {
                        sendStatus("Геолокация выключена")
                        delay(LOOP_DELAY_MS)
                        continue
                    }

                    val location = getLocationOnce()
                    val signalDbm = getSignalStrengthDbm()
                    val cellInfoText = getAllCellInfo()

                    if (location != null) {
                        val json = JSONObject().apply {
                            put("lat", location.latitude)
                            put("lon", location.longitude)
                            put("alt", location.altitude)
                            put("time", location.time)
                            put("signalDbm", signalDbm)
                            put("cellInfo", cellInfoText)
                        }

                        sendJsonToSocket(json.toString())

                        sendUpdateToActivity(
                            lat = location.latitude,
                            lon = location.longitude,
                            alt = location.altitude,
                            signalDbm = signalDbm,
                            rawCellInfo = cellInfoText
                        )
                    } else {
                        sendStatus("Локация не получена")
                    }
                } catch (e: SecurityException) {
                    Log.e(LOG_TAG, "SecurityException в сервисе", e)
                    sendStatus("SecurityException: ${e.message}")
                } catch (e: Exception) {
                    Log.e(LOG_TAG, "Ошибка в сервисе", e)
                    sendStatus("Ошибка: ${e.message}")
                }

                delay(LOOP_DELAY_MS)
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        workerStarted = false
        serviceJob.cancel()
        Log.d(LOG_TAG, "Service destroyed")
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(LOG_TAG, "Task removed")
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Background Service",
            NotificationManager.IMPORTANCE_LOW
        )
        channel.description = "Передача геоданных и сигнала в фоне"

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MyApplication")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
    }

    private suspend fun getLocationOnce(): Location? {
        if (!checkLocationPermissions()) return null
        if (!isLocationEnabled()) return null

        return suspendCancellableCoroutine { cont ->
            val fusedClient = LocationServices.getFusedLocationProviderClient(this)

            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (cont.isActive) {
                        cont.resume(location)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(LOG_TAG, "Ошибка получения локации", e)
                    if (cont.isActive) {
                        cont.resume(null)
                    }
                }
        }
    }

    private fun checkLocationPermissions(): Boolean {
        val fineGranted = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineGranted || coarseGranted
    }

    private fun isLocationEnabled(): Boolean {
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    private fun getAllCellInfo(): String {
        if (!checkLocationPermissions()) {
            return "allCellInfo:<no permission>"
        }

        return try {
            val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val list = tm.allCellInfo

            if (list.isNullOrEmpty()) {
                "allCellInfo:<empty>"
            } else {
                list.joinToString("\n---\n") { it.toString() }
            }
        } catch (e: SecurityException) {
            "allCellInfo:<security error> ${e.message}"
        } catch (e: Exception) {
            "allCellInfo:<error> ${e.message}"
        }
    }

    @SuppressLint("MissingPermission")
    private fun getSignalStrengthDbm(): Int {
        if (!checkLocationPermissions()) {
            return Int.MIN_VALUE
        }

        return try {
            val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val list = tm.allCellInfo ?: return Int.MIN_VALUE

            val firstRegistered = list.firstOrNull { it.isRegistered } ?: list.firstOrNull()

            when (firstRegistered) {
                is android.telephony.CellInfoLte -> firstRegistered.cellSignalStrength.dbm
                is android.telephony.CellInfoNr -> firstRegistered.cellSignalStrength.dbm
                is android.telephony.CellInfoGsm -> firstRegistered.cellSignalStrength.dbm
                is android.telephony.CellInfoWcdma -> firstRegistered.cellSignalStrength.dbm
                is android.telephony.CellInfoCdma -> firstRegistered.cellSignalStrength.dbm
                else -> Int.MIN_VALUE
            }
        } catch (e: SecurityException) {
            Log.e(LOG_TAG, "SecurityException чтения сигнала", e)
            Int.MIN_VALUE
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Ошибка чтения сигнала", e)
            Int.MIN_VALUE
        }
    }

    private fun sendJsonToSocket(jsonText: String) {
        var context: ZContext? = null
        var socket: ZMQ.Socket? = null

        try {
            context = ZContext()
            socket = context.createSocket(SocketType.REQ)

            socket.receiveTimeOut = 3000
            socket.sendTimeOut = 3000

            socket.connect("tcp://10.0.2.2:5656")
            socket.send(jsonText.toByteArray(ZMQ.CHARSET), 0)

            val reply = socket.recv(0)
            if (reply != null) {
                Log.d(LOG_TAG, "Ответ сервера: ${String(reply, ZMQ.CHARSET)}")
            } else {
                Log.d(LOG_TAG, "Сервер не ответил")
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Ошибка отправки сокетом", e)
        } finally {
            socket?.close()
            context?.close()
        }
    }

    private fun sendStatus(msg: String) {
        val intent = Intent(ACTION_BG_UPDATE)
        intent.putExtra("type", "status")
        intent.putExtra("status", msg)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun sendUpdateToActivity(
        lat: Double,
        lon: Double,
        alt: Double,
        signalDbm: Int,
        rawCellInfo: String
    ) {
        val intent = Intent(ACTION_BG_UPDATE)
        intent.putExtra("type", "data")
        intent.putExtra("lat", lat)
        intent.putExtra("lon", lon)
        intent.putExtra("alt", alt)
        intent.putExtra("signalDbm", signalDbm)
        intent.putExtra("rawCellInfo", rawCellInfo)
        intent.putExtra("timestamp", System.currentTimeMillis())
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }
}