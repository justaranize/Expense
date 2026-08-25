package com.justaranize.expense

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.rgb(245, 240, 230))
            setPadding(32, 32, 32, 32)
        }

        val title = TextView(this).apply {
            text = "Expense"
            textSize = 32f
            setTextColor(Color.rgb(55, 48, 40))
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "Your old-school expense organizer"
            textSize = 16f
            setTextColor(Color.rgb(90, 82, 70))
            gravity = Gravity.CENTER
        }

        root.addView(title)
        root.addView(subtitle)

        setContentView(root)
    }
}
