package org.example.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.LinearLayout
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import android.text.format.DateFormat
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat

class HistoryAdapter(
    private var entries: MutableList<CalculationHistory.HistoryEntry>,
    private val onEntryRemoved: (Int) -> Unit,
    private val onFavoriteToggled: (Int) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val container: LinearLayout = itemView as LinearLayout
        val timestampView: TextView = itemView.findViewById(R.id.historyTimestamp)
        val expressionView: TextView = itemView.findViewById(R.id.historyExpression)
        val resultView: TextView = itemView.findViewById(R.id.historyResult)
        val favoriteButton: ImageView = itemView.findViewById(R.id.favoriteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context = parent.context

        // Create views programmatically for the history item
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 8, 0, 14)
        }

        // Timestamp TextView
        val timestamp = TextView(context).apply {
            id = R.id.historyTimestamp
            textSize = 12f
            alpha = 0.8f
            setTextColor(ContextCompat.getColor(context, R.color.hintText))
        }

        // Expression TextView
        val expression = TextView(context).apply {
            id = R.id.historyExpression
            textSize = 15.5f
            maxLines = 3
            setTextColor(ContextCompat.getColor(context, R.color.onSurface))
        }

        // Result TextView
        val result = TextView(context).apply {
            id = R.id.historyResult
            textSize = 19f
            setPadding(0, 2, 0, 0)
            maxLines = 1
            setTextColor(ContextCompat.getColor(context, R.color.colorPrimary))
        }

        // Favorite button
        val favoriteButton = ImageView(context).apply {
            id = R.id.favoriteButton
            layoutParams = LinearLayout.LayoutParams(
                48, // width in pixels
                48  // height in pixels
            ).apply {
                gravity = android.view.Gravity.END or android.view.Gravity.CENTER_VERTICAL
                setMargins(8, 8, 8, 8)
            }
            setImageResource(R.drawable.ic_star_selector)
            background = ContextCompat.getDrawable(context, android.R.drawable.list_selector_background)
            setPadding(8, 8, 8, 8)
            isClickable = true
            isFocusable = true
            contentDescription = context.getString(R.string.favorite_button_desc)
        }

        container.addView(timestamp)
        container.addView(expression)
        container.addView(result)
        container.addView(favoriteButton)

        return ViewHolder(container)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = entries[position]
        
        holder.timestampView.text = DateFormat.format("MMM d, h:mm a", entry.timestamp)
        holder.expressionView.text = entry.expression
        holder.resultView.text = "= ${entry.result}"

        // Handle favorite status
        holder.favoriteButton.isSelected = entry.isFavorite
        holder.favoriteButton.setOnClickListener {
            onFavoriteToggled(position)
            notifyItemChanged(position)
        }

        // Set accessibility descriptions
        holder.container.apply {
            contentDescription = "Calculation: ${entry.expression} equals ${entry.result}, performed at ${DateFormat.format("MMMM d, h:mm a", entry.timestamp)}. ${if (entry.isFavorite) "Marked as favorite. " else ""}${context.getString(R.string.swipe_to_delete_desc)}"
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        }
    }

    override fun getItemCount() = entries.size

    fun removeItem(position: Int) {
        if (position >= 0 && position < entries.size) {
            entries.removeAt(position)
            notifyItemRemoved(position)
            onEntryRemoved(position)
        }
    }
}
