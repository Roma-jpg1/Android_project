package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import zmq.socket.Sockets

class MenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        val btnCalc = findViewById<Button>(R.id.btnCalc)
        val btnExit = findViewById<Button>(R.id.btnExit)
        val btnPlayer = findViewById<Button>(R.id.btnPlayer)
        val btnLocation = findViewById<Button>(R.id.btnLocation)
        val btnSockets = findViewById<Button>(R.id.btnSockets)

        btnCalc.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        btnPlayer.setOnClickListener {
            val intent1 = Intent(this, PlayerActivity::class.java)
            startActivity(intent1)
        }

        btnLocation.setOnClickListener {
            val intent1 = Intent(this, LocationActivity::class.java)
            startActivity(intent1)
        }

        btnSockets.setOnClickListener {
            val intent1 = Intent(this, SocketsActivity::class.java)
            startActivity(intent1)
        }

        btnExit.setOnClickListener {
            finishAffinity()
        }
    }
}
