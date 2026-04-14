package com.mobilebytelabs.kmptoolkit.bubble.android

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Lightweight Activity rendered inside an Android Bubble (API 30+).
 *
 * Receives content via intent extras:
 * - EXTRA_TITLE: bubble title
 * - EXTRA_MESSAGE: bubble message
 * - EXTRA_ACTIONS: comma-separated action labels
 *
 * Must be declared in AndroidManifest with:
 * - android:allowEmbedded="true"
 * - android:resizeableActivity="true"
 * - android:documentLaunchMode="always"
 */
class BubbleActivity : Activity() {

    companion object {
        const val EXTRA_TITLE = "bubble_title"
        const val EXTRA_MESSAGE = "bubble_message"
        const val EXTRA_ACTIONS = "bubble_actions"
        const val EXTRA_DEEP_LINK = "bubble_deep_link"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Bubble"
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: ""
        val actions = intent.getStringExtra(EXTRA_ACTIONS)?.split(",") ?: emptyList()

        val density = resources.displayMetrics.density

        val root = ScrollView(this).apply {
            setBackgroundColor(Color.WHITE)
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (16 * density).toInt()
            setPadding(pad, pad, pad, pad)
        }

        // Title
        layout.addView(
            TextView(this).apply {
                text = title
                textSize = 18f
                setTextColor(Color.BLACK)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                )
                params.bottomMargin = (8 * density).toInt()
                layoutParams = params
            },
        )

        // Message
        if (message.isNotEmpty()) {
            layout.addView(
                TextView(this).apply {
                    text = message
                    textSize = 14f
                    setTextColor(Color.DKGRAY)
                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    )
                    params.bottomMargin = (16 * density).toInt()
                    layoutParams = params
                },
            )
        }

        // Action buttons
        if (actions.isNotEmpty()) {
            val buttonRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
            }
            actions.forEach { label ->
                buttonRow.addView(
                    Button(this).apply {
                        text = label.trim()
                        isAllCaps = false
                        setOnClickListener { finish() }
                        val params = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                        )
                        params.marginStart = (8 * density).toInt()
                        layoutParams = params
                    },
                )
            }
            layout.addView(buttonRow)
        }

        root.addView(layout)
        setContentView(root)
    }
}
