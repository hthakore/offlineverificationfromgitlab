package com.example.demolib

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.offlineverification.Voicekey

class MainActivity : AppCompatActivity() {
    var voiceinstance: Voicekey? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        var btnChangeText = findViewById(R.id.btnChangeText) as Button
        voiceinstance = Voicekey(this)
        btnChangeText.setOnClickListener {
            voiceinstance!!.phrase = "One Two Three"
            Log.e("GetPhrase =>",voiceinstance!!.phrase)
        }
    }
}