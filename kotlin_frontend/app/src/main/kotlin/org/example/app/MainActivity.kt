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
import android.widget.Button

class MainActivity : Activity() {

    private lateinit var voiceSearchBar: VoiceSearchBar
    private lateinit var resultBox: TextView

    private var currentExpression: String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val textView = findViewById<TextView>(R.id.textView)
        // No longer show 'Hello World' or placeholder message, clear or hide the label
        textView.text = ""

        // Initialize VoiceSearchBar and connect to this Activity
        voiceSearchBar = findViewById(R.id.voiceSearchBar)
        voiceSearchBar.setHostActivity(this)

        // Set up the result box below the calculator keypad
        resultBox = findViewById(R.id.resultBox)
        updateResultBox(voiceSearchBar.getQuery())

        // Listen to changes in the VoiceSearchBar EditText to update result box
        voiceSearchBar.addQueryTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateResultBox(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // --- CALCULATOR KEYPAD LOGIC: wire up all keys to update input ---
        wireCalculatorKeypad()
    }

    private fun updateResultBox(expression: String) {
        currentExpression = expression
        resultBox.text = expression
    }

    /**
     * PUBLIC_INTERFACE
     * Called to evaluate the expression and update the result box.
     */
    private fun evaluateAndShowResult(expression: String) {
        if (expression.isBlank()) {
            resultBox.text = ""
            return
        }
        try {
            val result = CalculatorEngine.evaluate(expression)
            resultBox.text = result.toString()
        } catch (e: Exception) {
            resultBox.text = "Error"
        }
    }

    /**
     * PUBLIC_INTERFACE
     * Adds listeners to calculator keypad buttons so that tapping a button appends the symbol to the input.
     */
    private fun wireCalculatorKeypad() {
        // Helper to append symbol to the input bar
        fun appendToInput(symbol: String) {
            val orig = voiceSearchBar.getQuery()
            if (symbol == "=") {
                // Evaluate
                evaluateAndShowResult(orig)
                return
            }
            val newExpr = orig + symbol
            voiceSearchBar.setQuery(newExpr)
            // updateCurrentExpression(newExpr) // voiceSearchBar will trigger update through listener!
        }

        // Button ID to symbol mapping (now including Clear button)
        val buttonMap = listOf(
            Pair(R.id.btn0, "0"),
            Pair(R.id.btn1, "1"),
            Pair(R.id.btn2, "2"),
            Pair(R.id.btn3, "3"),
            Pair(R.id.btn4, "4"),
            Pair(R.id.btn5, "5"),
            Pair(R.id.btn6, "6"),
            Pair(R.id.btn7, "7"),
            Pair(R.id.btn8, "8"),
            Pair(R.id.btn9, "9"),
            Pair(R.id.btnDot, "."),
            Pair(R.id.btnPlus, "+"),
            Pair(R.id.btnMinus, "-"),
            Pair(R.id.btnMultiply, "×"),
            Pair(R.id.btnDivide, "÷"),
            Pair(R.id.btnLParen, "("),
            Pair(R.id.btnRParen, ")"),
            Pair(R.id.btnSin, "sin("),
            Pair(R.id.btnCos, "cos("),
            Pair(R.id.btnTan, "tan("),
            Pair(R.id.btnLog, "log("),
            Pair(R.id.btnLn, "ln("),
            Pair(R.id.btnSqrt, "√("),
            Pair(R.id.btnPower, "^"),
            Pair(R.id.btnEquals, "=") // evaluate
        )

        // Attach handlers for all normal buttons
        for ((id, symbol) in buttonMap) {
            val btn = findViewById<Button>(id)
            btn?.setOnClickListener {
                appendToInput(symbol)
            }
        }

        // Clear/reset button: clears input and result
        val btnClear = findViewById<Button>(R.id.btnClear)
        btnClear?.setOnClickListener {
            voiceSearchBar.setQuery("")
            resultBox.text = ""
            currentExpression = ""
        }
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

    // No longer need buildMessage() or MessageUtils; cleanly removed.
}
