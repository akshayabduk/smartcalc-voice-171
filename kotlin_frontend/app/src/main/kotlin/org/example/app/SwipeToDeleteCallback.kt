package org.example.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import android.graphics.Color
import kotlin.math.abs

class SwipeToDeleteCallback(
    private val adapter: HistoryAdapter,
    private val context: Context
) : ItemTouchHelper.SimpleCallback(
    0, // Drag directions
    ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT // Swipe directions
) {
    private val deleteIcon = context.getDrawable(android.R.drawable.ic_menu_delete)
    private val iconPadding = 24 // dp
    private val background = ColorDrawable(Color.parseColor("#FF5252"))
    private val backgroundCornerRadius = 8f // dp
    private val dpToPx = context.resources.displayMetrics.density

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.adapterPosition
        adapter.removeItem(position)
        viewHolder.itemView.announceForAccessibility(context.getString(R.string.calculation_deleted))
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView
        val itemHeight = itemView.bottom - itemView.top

        // Draw the red delete background
        val backgroundCornerRadiusPx = backgroundCornerRadius * dpToPx
        val backgroundBounds = RectF(
            itemView.left.toFloat(),
            itemView.top.toFloat(),
            itemView.right.toFloat(),
            itemView.bottom.toFloat()
        )
        
        background.bounds = android.graphics.Rect(
            backgroundBounds.left.toInt(),
            backgroundBounds.top.toInt(),
            backgroundBounds.right.toInt(),
            backgroundBounds.bottom.toInt()
        )
        
        // Only show background if swiping
        if (dX != 0f) {
            val paint = Paint().apply {
                color = Color.parseColor("#FF5252")
                isAntiAlias = true
            }
            c.drawRoundRect(backgroundBounds, backgroundCornerRadiusPx, backgroundCornerRadiusPx, paint)
            
            // Draw delete icon
            deleteIcon?.let { icon ->
                val iconMargin = (itemHeight - icon.intrinsicHeight) / 2
                val iconTop = itemView.top + iconMargin
                val iconBottom = iconTop + icon.intrinsicHeight

                when {
                    dX > 0 -> { // Swiping right
                        val iconLeft = itemView.left + iconMargin
                        val iconRight = iconLeft + icon.intrinsicWidth
                        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    }
                    dX < 0 -> { // Swiping left
                        val iconRight = itemView.right - iconMargin
                        val iconLeft = iconRight - icon.intrinsicWidth
                        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    }
                }
                icon.draw(c)
            }
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}
