package com.justaranize.expense

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 32, 24, 24)
            setBackgroundColor(Color.rgb(245, 240, 230))
        }

        val title = TextView(this).apply {
            text = "Expense"
            textSize = 30f
            setTextColor(Color.rgb(55, 48, 40))
        }

        val category = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                arrayOf(
                    "Food",
                    "Transport",
                    "Shopping",
                    "Bills",
                    "Other"
                )
            )
        }

        val amount = EditText(this).apply {
            hint = "Amount"
            inputType = 2
        }

        val save = Button(this).apply {
            text = "Save Expense"
        }

        root.addView(title)
        root.addView(category)
        root.addView(amount)
        root.addView(save)

        setContentView(root)
    }
}
