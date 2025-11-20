package com.example.myapplication
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplication.R
import android.location.Location
import android.location.LocationManager
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.myapplication.MainActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.json.JSONObject
import java.io.File

class LocationActivity : AppCompatActivity() {


    private lateinit var handler: android.os.Handler

    private lateinit var runnable: Runnable


    private var LastLat: Double? = null
    private var LastLon: Double? = null
    private var LastTime: Long =0


    val value: Int = 0
    val LOG_TAG: String = "LOCATION_ACTIVITY"
    private lateinit var bBackToMain: Button

    companion object {
        private const val PERMISSION_REQUEST_ACCESS_LOCATION= 100
    }

    private lateinit var myFusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var tvLat: TextView
    private lateinit var tvLon: TextView
    private lateinit var tvAlt: TextView
    private lateinit var tvCurt: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_location)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        bBackToMain = findViewById<Button>(R.id.back_to_main)

        myFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        tvLat = findViewById(R.id.tv_lat) as TextView
        tvLon = findViewById(R.id.tv_lon) as TextView
        tvAlt = findViewById(R.id.tv_Alt) as TextView
        tvCurt =findViewById(R.id.tv_Curt) as TextView

    }

    override fun onResume() {
        super.onResume()

        bBackToMain.setOnClickListener({
            val backToMain = Intent(this, MenuActivity::class.java)
            startActivity(backToMain)
        })

        val handler = android.os.Handler(mainLooper)
        val runnable = object : Runnable {
            override fun run() {
                getCurrentLocation()
                handler.postDelayed(this, 10000) //10
            }
        }
        handler.post(runnable)
        val timeHandler = android.os.Handler(mainLooper)
        val timeRunnable = object : Runnable {
            override fun run() {
                val currentTime = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                    .format(java.util.Date())
                tvCurt.text = currentTime
                timeHandler.postDelayed(this, 1000) // 1
            }
        }
        timeHandler.post(timeRunnable)
    }

    private fun getCurrentLocation(){

        if(checkPermissions()){
            if(isLocationEnabled()){
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissions()
                    return
                }
                myFusedLocationProviderClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnCompleteListener(this){ task->
                    val location: Location?=task.result
                    if(location == null){
                        Toast.makeText(applicationContext, "problems with signal", Toast.LENGTH_SHORT).show()
                    } else {
                        tvLat.setText(location.latitude.toString())
                        tvLon.setText(location.longitude.toString())
                        tvAlt.setText(location.altitude.toString())

                        val currentTime = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                        tvCurt.text = currentTime

                        if (doSave(location.latitude, location.longitude)){
                            apptojson(location.latitude, location.longitude, location.altitude, currentTime)
                        }
                    }
                }

            } else{
                // open settings to enable location
                Toast.makeText(applicationContext, "Enable location in settings", Toast.LENGTH_SHORT).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            Log.w(LOG_TAG, "location permission is not allowed");
            tvLat.setText("Permission is not granted")
            tvLon.setText("Permission is not granted")
            requestPermissions()
        }

    }

    private fun requestPermissions() {
        Log.w(LOG_TAG, "requestPermissions()");
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_REQUEST_ACCESS_LOCATION
        )
    }

    private fun checkPermissions(): Boolean{
        if( ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED )
        {
            return true
        } else {
            return false
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == PERMISSION_REQUEST_ACCESS_LOCATION)
        {
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(applicationContext, "Permission granted", Toast.LENGTH_SHORT).show()
                getCurrentLocation()
            } else {
                Toast.makeText(applicationContext, "Denied by user", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isLocationEnabled(): Boolean{
        val locationManager:LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

    }

    private fun doSave(currentLat: Double, currentLon: Double): Boolean{
        val now = System.currentTimeMillis()
        if (LastLat==null || LastLon==null || currentLat!=LastLat || currentLon!=LastLon || (now -LastTime)>=1000*60*5){
            return true}
        else{
            return false
        }
        }


    private fun updlast(lat: Double, lon: Double) {
        LastLat=lat
        LastLon=lon
        LastTime=System.currentTimeMillis()
    }

    private fun apptojson(lat: Double, lon: Double, alt: Double, time: String){
        val fname = "loc.json"
        val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_DOWNLOADS
        )
        val file = File(downloadDir, fname)

        if (!file.exists()) {
            val initialJson = """{ "locations": [] }"""
            file.writeText(initialJson)
        }

        val jsonText = file.readText()
        val jsonObject = JSONObject(jsonText)
        val locationsArray = jsonObject.getJSONArray("locations")

        val entry = JSONObject()
        entry.put("lat", lat)
        entry.put("lon", lon)
        entry.put("alt", alt)
        entry.put("time", time)

        locationsArray.put(entry)

        file.writeText(jsonObject.toString())

        updlast(lat, lon)


    }

}