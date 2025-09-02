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
import android.widget.ImageView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.Toast
import android.animation.ObjectAnimator

class MainActivity : Activity() {

    private lateinit var voiceSearchBar: VoiceSearchBar
    private lateinit var resultBox: TextView
    private var currentExpression: String = ""
    private var historyDrawer: View? = null
    private var isHistoryDrawerShowing: Boolean = false

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

        setupHistoryPanel()
    }

    private fun updateResultBox(expression: String) {
        currentExpression = expression
        resultBox.text = expression
    }

    /**
     * PUBLIC_INTERFACE
     * Called to evaluate the expression and update the result box.
     * Also saves the calculation to history if successful.
     */
    private fun evaluateAndShowResult(expression: String) {
        if (expression.isBlank()) {
            resultBox.text = ""
            return
        }
        try {
            val result = CalculatorEngine.evaluate(expression)
            resultBox.text = result.toString()
            CalculationHistory.add(expression, result.toString())
            updateHistoryPanelIfVisible()
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

    // INIT AND LOGIC for History Sheet/Drawer
    private fun setupHistoryPanel() {
        val btnShowHistory = findViewById<ImageView>(R.id.btnShowHistory)
        val historyOverlayContainer = findViewById<ViewGroup>(R.id.historyDrawerContainer)
        // inflate only on demand
        btnShowHistory?.setOnClickListener {
            if (!isHistoryDrawerShowing) {
                showHistoryDrawer()
            }
        }
        // clicking the overlay background closes the drawer
        historyOverlayContainer.setOnClickListener {
            if (isHistoryDrawerShowing) {
                hideHistoryDrawer()
            }
        }
    }

    private fun showHistoryDrawer() {
        val root = findViewById<ViewGroup>(R.id.historyDrawerContainer)
        if (isHistoryDrawerShowing) return

        // inflate and add the history sheet UI
        val sheet = LayoutInflater.from(this).inflate(R.layout.view_history_drawer, root, false)
        sheet.setOnClickListener { /* Eat touch events to keep drawer open */ }
        root.addView(sheet)
        root.visibility = View.VISIBLE
        historyDrawer = sheet
        isHistoryDrawerShowing = true
        populateHistoryDrawer(sheet)

        // Dismiss on clicking outside
        root.isClickable = true
        root.bringToFront()

        // Animate drawer up (if possible)
        sheet.translationY = sheet.height.toFloat() + 150f
        ObjectAnimator.ofFloat(sheet, "translationY", 1200f, 0f)
            .apply { duration = 310 }.start()
    }

    private fun hideHistoryDrawer() {
        val root = findViewById<ViewGroup>(R.id.historyDrawerContainer)
        if (!isHistoryDrawerShowing || historyDrawer == null) return

        // Animate down + remove after
        historyDrawer?.let { drawer ->
            ObjectAnimator.ofFloat(drawer, "translationY", 0f, 1200f)
                .apply {
                    duration = 250
                    start()
                }
            drawer.postDelayed({
                root.removeView(drawer)
                root.visibility = View.GONE
                historyDrawer = null
                isHistoryDrawerShowing = false
            }, 250)
        }
    }

    private fun populateHistoryDrawer(drawer: View) {
        val historyListLayout = drawer.findViewById<LinearLayout>(R.id.historyList)
        historyListLayout.removeAllViews()
        val allHistory = CalculationHistory.getHistory()
        if (allHistory.isEmpty()) {
            val emptyText = TextView(this).apply {
                setText(R.string.empty_history_prompt)
                setTextColor(resources.getColor(R.color.hintText))
                textSize = 17f
                setPadding(0, 32, 0, 0)
                gravity = android.view.Gravity.CENTER_HORIZONTAL
            }
            historyListLayout.addView(emptyText)
        } else {
            for ((expr, result) in allHistory) {
                val entryView = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(0, 8, 0, 14)
                }
                val exprView = TextView(this).apply {
                    text = expr
                    setTextColor(resources.getColor(R.color.onSurface))
                    textSize = 15.5f
                    maxLines = 3
                }
                val resultView = TextView(this).apply {
                    text = "= $result"
                    setTextColor(resources.getColor(R.color.colorPrimary))
                    textSize = 19f
                    setPadding(0, 2, 0, 0)
                    maxLines = 1
                }
                entryView.addView(exprView)
                entryView.addView(resultView)
                historyListLayout.addView(entryView)
                // Optionally: add divider here
                val divider = View(this).apply {
                    setBackgroundColor(resources.getColor(R.color.divider))
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 1)
                }
                historyListLayout.addView(divider)
            }
        }
        // Wire up "clear history" button
        val btnClear = drawer.findViewById<Button>(R.id.btnClearHistory)
        btnClear.setOnClickListener {
            CalculationHistory.clear()
            populateHistoryDrawer(drawer)
            Toast.makeText(this, getString(R.string.empty_history_prompt), Toast.LENGTH_SHORT).show()
        }
    }

    // Update history panel when calculation changes
    private fun updateHistoryPanelIfVisible() {
        if (isHistoryDrawerShowing && historyDrawer != null) {
            populateHistoryDrawer(historyDrawer!!)
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
