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
import java.util.Calendar
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var category: Spinner
    private lateinit var otherNote: EditText
    private lateinit var amount: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showExpense()
    }

    // =========================
    // EXPENSE INPUT
    // =========================

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
            setOnClickListener {
                showMenu()
            }
        }

        header.addView(title)
        header.addView(menu)

        category = Spinner(this)

        category.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            arrayOf(
                "Food",
                "Transport",
                "Shopping",
                "Bills",
                "Other"
            )
        )

        otherNote = EditText(this).apply {
            hint = "Keterangan"
            visibility = View.GONE
        }

        category.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {}

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    otherNote.visibility =
                        if (position == 4) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
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

        if (
            value.isEmpty() ||
            value.toLongOrNull() == null ||
            value.toLong() <= 0
        ) {
            amount.error = "Masukkan nominal"
            return
        }

        val position = category.selectedItemPosition

        if (
            position == 4 &&
            otherNote.text.toString().trim().isEmpty()
        ) {
            otherNote.error = "Keterangan wajib diisi"
            return
        }

        val expense = JSONObject().apply {
            put(
                "category",
                category.selectedItem.toString()
            )

            put(
                "amount",
                value.toLong()
            )

            put(
                "note",
                otherNote.text.toString().trim()
            )

            put(
                "timestamp",
                System.currentTimeMillis()
            )
        }

        val prefs = getSharedPreferences(
            "expense",
            MODE_PRIVATE
        )

        val expenses = JSONArray(
            prefs.getString("expenses", "[]")
        )

        expenses.put(expense)

        prefs.edit()
            .putString(
                "expenses",
                expenses.toString()
            )
            .apply()

        // Setelah save:
        // kembali kosong dan tetap di halaman input.

        amount.text.clear()
        otherNote.text.clear()
        category.setSelection(0)
    }

    // =========================
    // MENU
    // =========================

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

    // =========================
    // HISTORY
    // =========================

    private fun showHistory() {

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 32, 24, 24)
            setBackgroundColor(Color.WHITE)
        }

        val title = TextView(this).apply {
            text = "History"
            textSize = 30f
            setTextColor(Color.BLACK)
        }

        val period = Spinner(this)

        period.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            arrayOf(
                "Daily",
                "Weekly",
                "Monthly",
                "Yearly"
            )
        )

        val totalLabel = TextView(this).apply {
            text = "Total Expense"
            textSize = 16f
            setTextColor(Color.BLACK)
        }

        val total = TextView(this).apply {
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(Color.BLACK)
            setPadding(0, 16, 0, 16)
        }

        val chart = TextView(this).apply {
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.BLACK)
            setPadding(0, 24, 0, 24)
        }

        val detailed = Button(this).apply {
            text = "DETAILED EXPENSE"

            setOnClickListener {
                showDetailedExpense(
                    period.selectedItemPosition
                )
            }
        }

        fun refresh() {

            val expenses = getExpenses()

            val filtered = expenses.filter {

                matchesPeriod(
                    it.getLong("timestamp"),
                    period.selectedItemPosition
                )
            }

            var sum = 0L

            for (expense in filtered) {
                sum += expense.getLong("amount")
            }

            val formatter =
                NumberFormat.getNumberInstance(
                    Locale("id", "ID")
                )

            total.text =
                "Rp ${formatter.format(sum)}"

            chart.text =
                if (filtered.isEmpty()) {
                    "Chart kosong"
                } else {
                    buildSimpleChart(filtered)
                }
        }

        period.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {}

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    refresh()
                }
            }

        root.addView(title)
        root.addView(period)
        root.addView(totalLabel)
        root.addView(total)
        root.addView(chart)
        root.addView(detailed)

        setContentView(root)

        refresh()
    }

    // =========================
    // GET EXPENSES
    // =========================

    private fun getExpenses(): List<JSONObject> {

        val prefs = getSharedPreferences(
            "expense",
            MODE_PRIVATE
        )

        val array = JSONArray(
            prefs.getString("expenses", "[]")
        )

        val result = mutableListOf<JSONObject>()

        for (i in 0 until array.length()) {
            result.add(
                array.getJSONObject(i)
            )
        }

        return result
    }

    // =========================
    // PERIOD FILTER
    // =========================

    private fun matchesPeriod(
        timestamp: Long,
        period: Int
    ): Boolean {

        val now = Calendar.getInstance()

        val date = Calendar.getInstance().apply {
            timeInMillis = timestamp
        }

        return when (period) {

            // Daily
            0 ->

                date.get(Calendar.YEAR) ==
                    now.get(Calendar.YEAR) &&

                date.get(Calendar.DAY_OF_YEAR) ==
                    now.get(Calendar.DAY_OF_YEAR)

            // Weekly
            1 ->

                date.get(Calendar.YEAR) ==
                    now.get(Calendar.YEAR) &&

                date.get(Calendar.WEEK_OF_YEAR) ==
                    now.get(Calendar.WEEK_OF_YEAR)

            // Monthly
            2 ->

                date.get(Calendar.YEAR) ==
                    now.get(Calendar.YEAR) &&

                date.get(Calendar.MONTH) ==
                    now.get(Calendar.MONTH)

            // Yearly
            3 ->

                date.get(Calendar.YEAR) ==
                    now.get(Calendar.YEAR)

            else -> false
        }
    }

    // =========================
    // SIMPLE CHART
    // =========================

    private fun buildSimpleChart(
        expenses: List<JSONObject>
    ): String {

        val categories =
            linkedMapOf<String, Long>()

        for (expense in expenses) {

            val name =
                expense.getString("category")

            val value =
                expense.getLong("amount")

            categories[name] =
                (categories[name] ?: 0L) + value
        }

        val formatter =
            NumberFormat.getNumberInstance(
                Locale("id", "ID")
            )

        val result =
            StringBuilder()

        for ((name, value) in categories) {

            result
                .append(name)
                .append("\n")

            result
                .append("Rp ")
                .append(formatter.format(value))
                .append("\n\n")
        }

        return result.toString().trim()
    }

    // =========================
    // DETAILED EXPENSE
    // =========================

    private fun showDetailedExpense(
        period: Int
    ) {

        val expenses =
            getExpenses().filter {

                matchesPeriod(
                    it.getLong("timestamp"),
                    period
                )
            }

        val categories =
            linkedMapOf<String, Long>()

        for (expense in expenses) {

            val name =
                expense.getString("category")

            val value =
                expense.getLong("amount")

            categories[name] =
                (categories[name] ?: 0L) + value
        }

        val formatter =
            NumberFormat.getNumberInstance(
                Locale("id", "ID")
            )

        val message =

            if (categories.isEmpty()) {

                "Belum ada pengeluaran."

            } else {

                categories.entries.joinToString(
                    "\n\n"
                ) {

                    "${it.key}\nRp ${
                        formatter.format(it.value)
                    }"
                }
            }

        AlertDialog.Builder(this)
            .setTitle("Detailed Expense")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    // =========================
    // REMINDER
    // =========================

    private fun showReminder() {

        val input = EditText(this).apply {
            hint = "Tulis reminder"
        }

        AlertDialog.Builder(this)
            .setTitle("Reminder")
            .setView(input)
            .setPositiveButton("SET REMINDER") { _, _ ->

                // Reminder notification
                // akan dibuat di tahap berikutnya.
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    // =========================
    // SETTINGS
    // =========================

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

    // =========================
    // ABOUT
    // =========================

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

    // =========================
    // BACK
    // =========================

    override fun onBackPressed() {

        // Dari halaman mana pun:
        // Back -> Expense.
        // Back lagi -> Exit.

        showExpense()
    }
}
