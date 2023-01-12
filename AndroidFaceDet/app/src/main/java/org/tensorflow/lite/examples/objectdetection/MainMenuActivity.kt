package org.tensorflow.lite.examples.objectdetection

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button

class MainMenuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        val startBtn: Button = findViewById(R.id.startBtn)
        startBtn.setOnClickListener{
            Log.d("MainMenu", "Start button pressed")
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        val historyBtn: Button = findViewById(R.id.historyBtn)
        historyBtn.setOnClickListener{
            Log.d("MainMenu", "History button pressed")
            val intent = Intent(this, GraphActivity::class.java)
            startActivity(intent)
        }
    }
}