package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.*
import android.os.*
import android.provider.MediaStore
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File

class PlayerActivity : AppCompatActivity() {

    private lateinit var p: MediaPlayer
    private val list = mutableListOf<File>()
    private var i = 0

    private lateinit var bar: SeekBar
    private lateinit var cur: TextView
    private lateinit var tot: TextView
    private lateinit var name: TextView
    private lateinit var play: FloatingActionButton
    private lateinit var pause: FloatingActionButton
    private lateinit var stop: FloatingActionButton
    private lateinit var next: FloatingActionButton
    private lateinit var back: FloatingActionButton

    private val h = Handler(Looper.getMainLooper())
    private val upd = object : Runnable {
        override fun run() {
            if (p.isPlaying) {
                bar.progress = p.currentPosition
                update()
            }
            h.postDelayed(this, 1000)
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                Toast.makeText(this, "Доступ к аудио разрешён", Toast.LENGTH_SHORT).show()
                load()
            } else {
                name.text = "Разрешение не выдано"
                Toast.makeText(this, "Нужно разрешение для работы плеера", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        setContentView(R.layout.activity_player)

        bar = findViewById(R.id.seekBar)
        cur = findViewById(R.id.textCurrentTime)
        tot = findViewById(R.id.textTotalTime)
        name = findViewById(R.id.textTrackName)
        play = findViewById(R.id.play)
        pause = findViewById(R.id.pause)
        stop = findViewById(R.id.stop)
        next = findViewById(R.id.next)
        back = findViewById(R.id.back)

        play.setOnClickListener { playOrResume() }
        pause.setOnClickListener { if (p.isPlaying) p.pause() }
        stop.setOnClickListener { stopTrack() }
        next.setOnClickListener { change(1) }
        back.setOnClickListener { change(-1) }

        bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, pos: Int, fromUser: Boolean) {
                if (fromUser) p.seekTo(pos)
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        p = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setOnCompletionListener { change(1) }
        }

        checkPermission()
    }

    private fun checkPermission() {
        val permission = Manifest.permission.READ_MEDIA_AUDIO

        if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            load()
        } else {
            requestPermissionLauncher.launch(permission)
        }
    }

    @SuppressLint("Range")
    private fun load() {
        list.clear()
        val q = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Audio.Media.DATA),
            "${MediaStore.Audio.Media.IS_MUSIC}!=0",
            null,
            "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        )
        q?.use {
            val p = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            while (it.moveToNext()) {
                val f = File(it.getString(p))
                if (f.exists()) list.add(f)
            }
        }
        if (list.isEmpty()) name.text = "Музыка не найдена" else start()
    }

    private fun start() {
        val f = list[i]
        p.reset()
        p.setDataSource(f.path)
        p.prepare()
        p.start()
        name.text = f.nameWithoutExtension
        bar.max = p.duration
        h.post(upd)
    }

    private fun playOrResume() {
        if (list.isEmpty()) return
        if (p.isPlaying) return
        if (p.currentPosition > 0 && bar.max > 0) {
            p.start()
            h.post(upd)
        } else start()
    }

    private fun stopTrack() {
        p.pause()
        p.seekTo(0)
        bar.progress = 0
        update()
    }

    private fun change(s: Int) {
        if (list.isEmpty()) return
        i = (i + s + list.size) % list.size
        start()
    }

    private fun update() {
        fun f(ms: Int) = "%02d:%02d".format(ms / 60000, (ms / 1000) % 60)
        cur.text = f(p.currentPosition)
        tot.text = f(p.duration)
    }

    override fun onPause() {
        super.onPause()
        if (p.isPlaying) p.pause()
        h.removeCallbacks(upd)
    }

    override fun onDestroy() {
        super.onDestroy()
        p.release()
        h.removeCallbacks(upd)
    }
}