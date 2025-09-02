package org.example.app

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast

/**
 * VoiceSearchBar is a reusable UI component that shows a search text field and a microphone button.
 * Tapping the microphone button triggers Google Speech-to-Text via RecognizerIntent. The recognized
 * text is populated into the search field.
 *
 * Design: minimal, light theme. Colors use brand primary/secondary/accent from resources.
 *
 * To use:
 * - Place <include layout="@layout/view_voice_search_bar"/> in your layout or create programmatically.
 * - In an Activity, call setHostActivity(this) so this view can request permissions and startActivityForResult.
 * - Forward onActivityResult to this view's handleSpeechResult(requestCode, resultCode, data).
 */
class VoiceSearchBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(context, attrs, defStyle) {

    private var etSearch: EditText
    private var btnMic: ImageButton
    @Suppress("unused")
    private var ivSearchIcon: ImageView

    private var hostActivity: Activity? = null

    companion object {
        private const val REQ_CODE_SPEECH_INPUT = 0x1201
        private const val REQ_RECORD_AUDIO_PERMISSION = 0x2201
    }

    init {
        orientation = HORIZONTAL
        LayoutInflater.from(context).inflate(R.layout.view_voice_search_bar, this, true)
        etSearch = findViewById(R.id.etSearch)
        btnMic = findViewById(R.id.btnMic)
        ivSearchIcon = findViewById(R.id.ivSearchIcon)

        btnMic.setOnClickListener {
            startVoiceInput()
        }
    }

    /**
     * PUBLIC_INTERFACE
     * Sets the host Activity so the view can request permissions and start the speech recognizer.
     */
    fun setHostActivity(activity: Activity) {
        this.hostActivity = activity
    }

    /**
     * PUBLIC_INTERFACE
     * Returns the current text in the search field.
     */
    fun getQuery(): String = etSearch.text?.toString().orEmpty()

    /**
     * PUBLIC_INTERFACE
     * Sets the query text in the search field.
     */
    fun setQuery(text: String) {
        etSearch.setText(text)
        etSearch.setSelection(text.length)
    }

    /**
     * PUBLIC_INTERFACE
     * Adds a TextWatcher to listen for query updates.
     */
    fun addQueryTextChangedListener(watcher: TextWatcher) {
        etSearch.addTextChangedListener(watcher)
    }

    private fun startVoiceInput() {
        val activity = hostActivity ?: run {
            Toast.makeText(context, "Host activity not set.", Toast.LENGTH_SHORT).show()
            return
        }

        // Check microphone permission
        if (activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQ_RECORD_AUDIO_PERMISSION)
            return
        }

        // Build recognizer intent
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PROMPT, resources.getString(R.string.search_hint))
        }

        try {
            activity.startActivityForResult(intent, REQ_CODE_SPEECH_INPUT)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, resources.getString(R.string.voice_error_no_recognizer), Toast.LENGTH_LONG).show()
        }
    }

    /**
     * PUBLIC_INTERFACE
     * Handle onActivityResult forwarded from the host Activity to capture speech recognition result.
     *
     * @return true if the requestCode was consumed by this view, false otherwise.
     */
    fun handleSpeechResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode != REQ_CODE_SPEECH_INPUT) return false
        if (resultCode != Activity.RESULT_OK || data == null) {
            Toast.makeText(context, resources.getString(R.string.voice_error_result), Toast.LENGTH_SHORT).show()
            return true
        }
        val results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        val text = results?.firstOrNull()
        if (!text.isNullOrBlank()) {
            setQuery(text)
        } else {
            Toast.makeText(context, resources.getString(R.string.voice_error_result), Toast.LENGTH_SHORT).show()
        }
        return true
    }

    /**
     * PUBLIC_INTERFACE
     * Should be called by host Activity from onRequestPermissionsResult to continue flow after mic permission grant.
     *
     * @return true if permission request belongs to this view.
     */
    fun handlePermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray): Boolean {
        if (requestCode != REQ_RECORD_AUDIO_PERMISSION) return false
        val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        if (granted) {
            startVoiceInput()
        } else {
            Toast.makeText(context, resources.getString(R.string.voice_error_no_permission), Toast.LENGTH_SHORT).show()
        }
        return true
    }
}
