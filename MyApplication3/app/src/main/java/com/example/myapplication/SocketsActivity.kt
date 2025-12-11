package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresPermission
import com.example.myapplication.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.json.JSONObject
import org.zeromq.ZContext
import org.zeromq.SocketType
import org.zeromq.ZMQ
import java.util.Date

class SocketsActivity : AppCompatActivity() {

    private val LOG_TAG = "SOCKETS_ACTIVITY"

    private lateinit var tvSockets: TextView
    private lateinit var tvMessages: TextView
    private lateinit var tvLat: TextView
    private lateinit var tvLon: TextView
    private lateinit var tvAlt: TextView
    private lateinit var tvCurt: TextView

    private lateinit var handler: Handler
    private lateinit var fusedClient: FusedLocationProviderClient

    private var LastLat: Double? = null
    private var LastLon: Double? = null
    private var LastTime: Long = 0

    companion object {
        const val PERMISSION_REQUEST = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sockets2)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        handler = Handler(Looper.getMainLooper())
        fusedClient = LocationServices.getFusedLocationProviderClient(this)

        tvSockets = findViewById(R.id.tvSockets)
        tvMessages = findViewById(R.id.tvMessages)


    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onResume() {
        super.onResume()
        getCurrentLocation()
    }

    // ------------------ ЛОКАЦИЯ ---------------------
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun getCurrentLocation() {

        if (!checkPermissions()) {
            requestPermissions()
            return
        }

        if (!isLocationEnabled()) {
            Toast.makeText(this, "Включи геолокацию", Toast.LENGTH_SHORT).show()
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            return
        }

        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location == null) {
                    Toast.makeText(this, "Нет сигнала GPS", Toast.LENGTH_SHORT).show()
                } else {
                    updateUI(location)
                    sendLocationJSON(location)
                }
            }
    }

    private fun updateUI(loc: Location) {
        if (shouldSave(loc.latitude, loc.longitude)) {
            LastLat = loc.latitude
            LastLon = loc.longitude
            LastTime = System.currentTimeMillis()
        }
    }

    private fun checkPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            PERMISSION_REQUEST
        )
    }

    private fun isLocationEnabled(): Boolean {
        val m = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return m.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || m.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun shouldSave(lat: Double, lon: Double): Boolean {
        val now = System.currentTimeMillis()
        return LastLat == null ||
                LastLon == null ||
                lat != LastLat ||
                lon != LastLon ||
                (now - LastTime) >= 5 * 60 * 1000
    }

    private fun sendLocationJSON(loc: Location) {

        val json = JSONObject().apply {
            put("lat", loc.latitude)
            put("lon", loc.longitude)
            put("alt", loc.altitude)
            put("time", loc.time.toString())
        }

        val jsonText = json.toString()

        Thread {
            try {
                val context = ZMQ.context(1)
                val socket = ZContext().createSocket(SocketType.REQ)

                socket.connect("tcp://192.168.242.55:9560")

                socket.send(jsonText.toByteArray(ZMQ.CHARSET), 0)

                val replyBytes = socket.recv(0)
                val reply = String(replyBytes, ZMQ.CHARSET)

                handler.post {
                    tvMessages.append("Ответ сервера: $reply\n")
                }

                socket.close()
                context.close()

            } catch (e: Exception) {
                Log.e(LOG_TAG, "Ошибка отправки JSON", e)
            }
        }.start()
    }
}
