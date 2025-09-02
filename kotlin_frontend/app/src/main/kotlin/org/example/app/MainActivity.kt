package org.example.app

import org.apache.commons.text.WordUtils
import org.example.list.LinkedList
import org.example.utilities.SplitUtils
import org.example.utilities.StringUtils

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import android.widget.ImageButton
import android.widget.LinearLayout

class MainActivity : Activity() {

    private lateinit var voiceSearchBar: VoiceSearchBar
    private lateinit var inputExpressionRow: LinearLayout
    private lateinit var tvCurrentExpression: TextView
    private lateinit var btnClearExpression: ImageButton

    private var currentExpression: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val textView = findViewById(R.id.textView) as TextView
        textView.text = buildMessage()

        // Initialize VoiceSearchBar and connect to this Activity
        voiceSearchBar = findViewById(R.id.voiceSearchBar)
        voiceSearchBar.setHostActivity(this)

        // Setup the input/expression row and clear button
        inputExpressionRow = findViewById(R.id.inputExpressionRow)
        tvCurrentExpression = findViewById(R.id.tvCurrentExpression)
        btnClearExpression = findViewById(R.id.btnClearExpression)

        // (Optional: hide row initially if desired, currently always visible)
        updateCurrentExpression(voiceSearchBar.getQuery())

        // Listen to changes in the VoiceSearchBar EditText to update current expression
        voiceSearchBar.addQueryTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateCurrentExpression(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnClearExpression.setOnClickListener {
            voiceSearchBar.setQuery("")
            updateCurrentExpression("")
        }
    }

    private fun updateCurrentExpression(expression: String) {
        currentExpression = expression
        tvCurrentExpression.text = expression
        // Optionally, you could hide the row or clear button when empty for more minimal effect
        // Example:
        // inputExpressionRow.visibility = if (expression.isBlank()) View.GONE else View.VISIBLE
        // btnClearExpression.visibility = if (expression.isBlank()) View.INVISIBLE else View.VISIBLE
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
