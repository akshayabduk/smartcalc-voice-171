package org.example.app

import org.apache.commons.text.WordUtils
import org.example.list.LinkedList
import org.example.utilities.SplitUtils
import org.example.utilities.StringUtils

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var voiceSearchBar: VoiceSearchBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val textView = findViewById(R.id.textView) as TextView
        textView.text = buildMessage()

        // Initialize voice search bar and connect to this Activity
        voiceSearchBar = findViewById(R.id.voiceSearchBar)
        voiceSearchBar.setHostActivity(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // Let the VoiceSearchBar try to handle the result first
        if (this::voiceSearchBar.isInitialized && voiceSearchBar.handleSpeechResult(requestCode, resultCode, data)) {
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (this::voiceSearchBar.isInitialized && voiceSearchBar.handlePermissionsResult(requestCode, permissions, grantResults)) {
            return
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun buildMessage(): String {
        val tokens: LinkedList = SplitUtils.split(MessageUtils.message())
        val result: String = StringUtils.join(tokens)
        return WordUtils.capitalize(result)
    }
}
