package org.example.app

import org.apache.commons.text.WordUtils
import org.example.list.LinkedList
import org.example.utilities.SplitUtils
import org.example.utilities.StringUtils

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.app.UiModeManager
import android.content.Context
import android.view.Menu
import android.view.MenuItem
import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Button
import android.widget.ImageView
import android.widget.HorizontalScrollView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.Toast
import android.animation.ObjectAnimator

class MainActivity : Activity() {
    // Rest of the file remains unchanged
    private lateinit var voiceSearchBar: VoiceSearchBar
    private lateinit var resultBox: TextView
    private var currentExpression: String = ""
    private var historyDrawer: View? = null
    private lateinit var undoRedoManager: UndoRedoManager
    private lateinit var btnUndo: ImageButton
    private lateinit var btnRedo: ImageButton
    private var isHistoryDrawerShowing: Boolean = false
    private lateinit var resultHistoryRow: View
    private lateinit var newsTickerText: TextView
    private var newsTickerAnimator: ObjectAnimator? = null

    private lateinit var uiModeManager: UiModeManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        
        // Initialize undo/redo manager
        undoRedoManager = UndoRedoManager()
        undoRedoManager.addState("")  // Initial empty state

        // Set up undo/redo buttons
        btnUndo = findViewById(R.id.btnUndo)
        btnRedo = findViewById(R.id.btnRedo)
        
        setupUndoRedoButtons()

        val textView = findViewById<TextView>(R.id.textView)
        // No longer show 'Hello World' or placeholder message, clear or hide the label
        textView.text = ""

        // Initialize VoiceSearchBar and connect to this Activity
        voiceSearchBar = findViewById(R.id.voiceSearchBar)
        voiceSearchBar.setHostActivity(this)

        // Set up the result box below the calculator keypad
        resultBox = findViewById(R.id.resultBox)
        updateResultBox(voiceSearchBar.getQuery())

        // Find the result/history row (to show/hide when history is shown)
        resultHistoryRow = findViewById(R.id.resultHistoryRow)

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
        setupNewsTicker()
    }

    private fun setupNewsTicker() {
        newsTickerText = findViewById(R.id.newsTickerText)
        newsTickerText.text = getString(R.string.sample_news)
        newsTickerText.contentDescription = getString(R.string.news_ticker_content_desc)
        
        // Start auto-scrolling animation
        startNewsTickerAnimation()

        // Handle touch to pause/resume scrolling
        newsTickerText.setOnClickListener {
            if (newsTickerAnimator?.isPaused == true) {
                newsTickerAnimator?.resume()
            } else {
                newsTickerAnimator?.pause()
            }
        }
    }

    private fun startNewsTickerAnimation() {
        newsTickerText.post {
            val scrollView = newsTickerText.parent as HorizontalScrollView
            val textWidth = newsTickerText.width
            val screenWidth = scrollView.width

            // Only animate if text is wider than the screen
            if (textWidth > screenWidth) {
                // Calculate total scroll distance including padding for smooth loop
                val scrollAmount = textWidth + screenWidth

                newsTickerAnimator?.cancel()
                newsTickerAnimator = ObjectAnimator.ofInt(
                    scrollView,
                    "scrollX",
                    0,
                    scrollAmount
                ).apply {
                    duration = (scrollAmount * 25).toLong() // Speed based on content length
                    repeatCount = ObjectAnimator.INFINITE
                    repeatMode = ObjectAnimator.RESTART
                    
                    // Reset scroll position when reaching the end
                    addUpdateListener { animator ->
                        val currentScroll = animator.animatedValue as Int
                        if (currentScroll >= textWidth) {
                            scrollView.scrollTo(0, 0)
                            animator.start()
                        }
                    }
                    start()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        newsTickerAnimator?.pause()
    }

    override fun onResume() {
        super.onResume()
        newsTickerAnimator?.resume()
    }

    private fun updateResultBox(expression: String) {
        currentExpression = expression
        resultBox.text = expression
        undoRedoManager.addState(expression)
        updateUndoRedoButtonStates()
    }

    private fun setupUndoRedoButtons() {
        btnUndo.setOnClickListener {
            undoRedoManager.undo()?.let { prevState ->
                voiceSearchBar.setQuery(prevState)
                updateUndoRedoButtonStates()
            }
        }

        btnRedo.setOnClickListener {
            undoRedoManager.redo()?.let { nextState ->
                voiceSearchBar.setQuery(nextState)
                updateUndoRedoButtonStates()
            }
        }

        updateUndoRedoButtonStates()
    }

    private fun updateUndoRedoButtonStates() {
        btnUndo.isEnabled = undoRedoManager.canUndo()
        btnRedo.isEnabled = undoRedoManager.canRedo()
        btnUndo.alpha = if (undoRedoManager.canUndo()) 1.0f else 0.3f
        btnRedo.alpha = if (undoRedoManager.canRedo()) 1.0f else 0.3f
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
        // Helper to append symbol to the input bar and announce for accessibility
        fun appendToInput(symbol: String, contentDescription: String) {
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
            btn?.apply {
                contentDescription = when {
                    symbol.matches(Regex("[0-9]")) -> getString(R.string.numeric_button_desc, symbol)
                    symbol in setOf("+", "-", "×", "÷", "^") -> getString(R.string.operator_button_desc, symbol)
                    symbol.endsWith("(") -> getString(R.string.function_button_desc, symbol.removeSuffix("("))
                    symbol == "=" -> getString(R.string.equals_button_desc)
                    else -> symbol
                }
                setOnClickListener {
                    appendToInput(symbol, contentDescription.toString())
                    announceForAccessibility(contentDescription)
                }
                minHeight = resources.getDimensionPixelSize(R.dimen.min_touch_target_size)
                minWidth = resources.getDimensionPixelSize(R.dimen.min_touch_target_size)
            }
        }

        // Clear/reset button: clears input and result
        val btnClear = findViewById<Button>(R.id.btnClear)
        btnClear?.setOnClickListener {
            voiceSearchBar.setQuery("")
            resultBox.text = ""
            currentExpression = ""
            undoRedoManager.clear()
            undoRedoManager.addState("")
            updateUndoRedoButtonStates()
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

        // Prevent multiple drawer instances
        if (historyDrawer != null) {
            root.removeView(historyDrawer)
            historyDrawer = null
        }

        // Inflate and setup drawer
        val sheet = LayoutInflater.from(this).inflate(R.layout.view_history_drawer, root, false)
        sheet.setOnClickListener { /* Prevent clicks from closing drawer */ }
        root.addView(sheet)
        root.visibility = View.VISIBLE
        historyDrawer = sheet
        isHistoryDrawerShowing = true

        // Prepare animation
        sheet.alpha = 0f
        sheet.translationY = resources.displayMetrics.heightPixels.toFloat()

        // Animate drawer entry with proper timing
        resultHistoryRow.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                resultHistoryRow.visibility = View.GONE
                sheet.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(300)
                    .start()
            }
            .start()

        populateHistoryDrawer(sheet)
        root.isClickable = true
    }

    private fun hideHistoryDrawer() {
        val root = findViewById<ViewGroup>(R.id.historyDrawerContainer)
        if (!isHistoryDrawerShowing || historyDrawer == null) return

        historyDrawer?.let { drawer ->
            // First animate drawer down
            drawer.animate()
                .translationY(resources.displayMetrics.heightPixels.toFloat())
                .alpha(0f)
                .setDuration(250)
                .withEndAction {
                    root.removeView(drawer)
                    root.visibility = View.GONE
                    historyDrawer = null
                    isHistoryDrawerShowing = false
                }
                .start()

            // Simultaneously fade in result row
            resultHistoryRow.visibility = View.VISIBLE
            resultHistoryRow.alpha = 0f
            resultHistoryRow.animate()
                .alpha(1f)
                .setDuration(200)
                .start()
        }
    }

    private fun populateHistoryDrawer(drawer: View) {
        val recyclerView = drawer.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.historyRecyclerView)
        val allHistory = CalculationHistory.getHistory()

        if (allHistory.isEmpty()) {
            // Show empty state
            recyclerView.visibility = View.GONE
            val emptyText = TextView(this).apply {
                setText(R.string.empty_history_prompt)
                setTextColor(resources.getColor(R.color.hintText))
                textSize = 17f
                setPadding(0, 32, 0, 0)
                gravity = android.view.Gravity.CENTER_HORIZONTAL
            }
            val parent = recyclerView.parent as ViewGroup
            if (parent.findViewById<TextView>(R.id.emptyHistoryText) == null) {
                emptyText.id = R.id.emptyHistoryText
                parent.addView(emptyText)
            }
        } else {
            recyclerView.visibility = View.VISIBLE
            drawer.findViewById<TextView>(R.id.emptyHistoryText)?.let {
                (it.parent as ViewGroup).removeView(it)
            }

            // Set up RecyclerView if not already set up
            if (recyclerView.adapter == null) {
                val adapter = HistoryAdapter(allHistory.toMutableList()) { position ->
                    // Handle item removal
                    CalculationHistory.removeAt(position)
                }
                recyclerView.adapter = adapter
                recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
                
                // Add dividers between items
                val dividerItemDecoration = androidx.recyclerview.widget.DividerItemDecoration(
                    recyclerView.context,
                    androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
                )
                recyclerView.addItemDecoration(dividerItemDecoration)

                // Add swipe-to-delete functionality
                val swipeHandler = SwipeToDeleteCallback(adapter, this)
                val itemTouchHelper = androidx.recyclerview.widget.ItemTouchHelper(swipeHandler)
                itemTouchHelper.attachToRecyclerView(recyclerView)
            } else {
                // Update existing adapter
                (recyclerView.adapter as HistoryAdapter).let { adapter ->
                    adapter.notifyDataSetChanged()
                }
            }
        }
        // Wire up "clear history" button
        val btnClear = drawer.findViewById<Button>(R.id.btnClearHistory)
        btnClear.setOnClickListener {
            CalculationHistory.clear()
            populateHistoryDrawer(drawer)
            Toast.makeText(this, getString(R.string.empty_history_prompt), Toast.LENGTH_SHORT).show()
        }

        // Wire up close (X) button
        val btnClose = drawer.findViewById<ImageButton>(R.id.btnCloseHistory)
        btnClose.setOnClickListener {
            hideHistoryDrawer()
        }
    }

    // Update history panel when calculation changes
    private fun updateHistoryPanelIfVisible() {
        if (isHistoryDrawerShowing && historyDrawer != null) {
            populateHistoryDrawer(historyDrawer!!)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_theme, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_toggle_theme -> {
                toggleTheme()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun toggleTheme() {
        if (uiModeManager.nightMode == UiModeManager.MODE_NIGHT_NO) {
            uiModeManager.nightMode = UiModeManager.MODE_NIGHT_YES
        } else {
            uiModeManager.nightMode = UiModeManager.MODE_NIGHT_NO
        }
        recreate() // Recreate activity to apply theme changes
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
