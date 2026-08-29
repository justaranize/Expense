package com.justaranize.expense

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var category: Spinner
    private lateinit var otherNote: EditText
    private lateinit var amount: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showExpense()
    }

    private fun showExpense() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 32, 24, 24)
            setBackgroundColor(Color.WHITE)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val title = TextView(this).apply {
            text = "Expense"
            textSize = 30f
            setTextColor(Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val menu = Button(this).apply {
            text = "☰"
            setOnClickListener { showMenu() }
        }

        header.addView(title)
        header.addView(menu)

        category = Spinner(this).apply {
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

        otherNote = EditText(this).apply {
            hint = "Keterangan"
            visibility = View.GONE
        }

        category.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onNothingSelected(parent: AdapterView<*>?) {}

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    otherNote.visibility =
                        if (position == 4) View.VISIBLE else View.GONE
                }
            }

        amount = EditText(this).apply {
            hint = "Rp 0"
            inputType = InputType.TYPE_CLASS_NUMBER
            gravity = Gravity.CENTER
            textSize = 28f
        }

        val save = Button(this).apply {
            text = "SAVE EXPENSE"

            setOnClickListener {
                saveCurrentExpense()
            }
        }

        root.addView(header)
        root.addView(category)
        root.addView(otherNote)
        root.addView(amount)

        root.addView(
            save,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        setContentView(root)
    }

    private fun saveCurrentExpense() {

        val value = amount.text.toString().trim()

        if (value.isEmpty() || value.toLongOrNull() == null ||
            value.toLong() <= 0
        ) {
            amount.error = "Masukkan nominal"
            return
        }

        val position = category.selectedItemPosition

        if (position == 4 &&
            otherNote.text.toString().trim().isEmpty()
        ) {
            otherNote.error = "Keterangan wajib diisi"
            return
        }

        val categoryName = category.selectedItem.toString()
        val note = otherNote.text.toString().trim()

        val prefs = getSharedPreferences("expense", MODE_PRIVATE)

        val expenses = JSONArray(
            prefs.getString("expenses", "[]")
        )

        val expense = JSONObject().apply {
            put("category", categoryName)
            put("amount", value.toLong())
            put("note", note)
            put("timestamp", System.currentTimeMillis())
        }

        expenses.put(expense)

        prefs.edit()
            .putString("expenses", expenses.toString())
            .apply()

        // Setelah save: tetap di halaman Expense,
        // semua field kembali kosong.
        amount.text.clear()
        otherNote.text.clear()
        category.setSelection(0)
    }

    private fun showMenu() {

        val items = arrayOf(
            "History",
            "Reminder",
            "Setting",
            "About",
            "Exit"
        )

        AlertDialog.Builder(this)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> showHistory()
                    1 -> showReminder()
                    2 -> showSettings()
                    3 -> showAbout()
                    4 -> finish()
                }
            }
            .show()
    }

    private fun showHistory() {

        val prefs = getSharedPreferences("expense", MODE_PRIVATE)

        val expenses = JSONArray(
            prefs.getString("expenses", "[]")
        )

        var total = 0L

        for (i in 0 until expenses.length()) {
            total += expenses
                .getJSONObject(i)
                .getLong("amount")
        }

        val formatter = NumberFormat.getNumberInstance(
            Locale("id", "ID")
        )

        val totalText = "Rp ${formatter.format(total)}"

        AlertDialog.Builder(this)
            .setTitle("History")
            .setMessage(
                """
                [ Daily ▼ ]
                
                Total Expense
                
                $totalText
                
                [ CHART ]
                
                [ DETAILED EXPENSE ]
                """.trimIndent()
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showReminder() {

        val input = EditText(this).apply {
            hint = "Tulis reminder"
        }

        AlertDialog.Builder(this)
            .setTitle("Reminder")
            .setView(input)
            .setPositiveButton("SET REMINDER") { _, _ ->
                // Reminder akan kita kerjakan di tahap berikutnya.
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun showSettings() {

        val items = arrayOf(
            "Currency: IDR",
            "Language: Indonesia",
            "Conversion Rate",
            "Disclaimer"
        )

        AlertDialog.Builder(this)
            .setTitle("Setting")
            .setItems(items, null)
            .setNegativeButton("OK", null)
            .show()
    }

    private fun showAbout() {

        val message = """
            And app made with the spirit of lazyness 🤣
            
            Donate
            
            Buy Me a Coffee
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("About")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onBackPressed() {
        showExpense()
    }
}
