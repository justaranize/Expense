package com.justaranize.expense

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.text.*
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

    private var formattingAmount = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showExpense()

        if (
            android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.POST_NOTIFICATIONS
                ),
                100
            )
        }

        createNotificationChannel()
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
            hint = "${currencyPrefix()} 0"
            inputType =
                InputType.TYPE_CLASS_NUMBER
            gravity = Gravity.CENTER
            textSize = 28f
            setSingleLine(true)
        }

        amount.addTextChangedListener(
            object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {}

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {}

                override fun afterTextChanged(
                    s: Editable?
                ) {

                    if (formattingAmount) return

                    val raw = s
                        ?.toString()
                        ?.replace(".", "")
                        ?.replace(",", "")
                        ?.replace("Rp", "")
                        ?.replace("$", "")
                        ?.replace(" ", "")
                        ?.trim()

                    if (raw.isNullOrEmpty()) {
                        return
                    }

                    val number =
                        raw.toLongOrNull()
                            ?: return

                    formattingAmount = true

                    amount.setText(
                        formatInputCurrency(number)
                    )

                    amount.setSelection(
                        amount.text.length
                    )

                    formattingAmount = false
                }
            }
        )

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
        root.addView(save)

        setContentView(root)
    }

    // =========================
    // CURRENCY
    // =========================

    private fun getCurrency(): String {

        return getSharedPreferences(
            "expense",
            MODE_PRIVATE
        ).getString(
            "currency",
            "IDR"
        ) ?: "IDR"
    }

    private fun currencyPrefix(): String {

        return if (getCurrency() == "USD") {
            "$"
        } else {
            "Rp"
        }
    }

    private fun formatInputCurrency(
        value: Long
    ): String {

        val formatted =
            if (getCurrency() == "USD") {

                NumberFormat
                    .getNumberInstance(Locale.US)
                    .format(value)

            } else {

                NumberFormat
                    .getNumberInstance(
                        Locale("id", "ID")
                    )
                    .format(value)
            }

        return "${currencyPrefix()} $formatted"
    }

    private fun formatCurrency(
        value: Long
    ): String {

        return if (getCurrency() == "USD") {

            NumberFormat
                .getNumberInstance(Locale.US)
                .format(value)

        } else {

            NumberFormat
                .getNumberInstance(
                    Locale("id", "ID")
                )
                .format(value)
        }
    }

    // =========================
    // SAVE EXPENSE
    // =========================

    private fun saveCurrentExpense() {

        val raw = amount.text
            .toString()
            .replace(".", "")
            .replace(",", "")
            .replace("Rp", "")
            .replace("$", "")
            .replace(" ", "")
            .trim()

        val value = raw.toLongOrNull()

        if (value == null || value <= 0) {
            amount.error = "Masukkan nominal"
            return
        }

        val position =
            category.selectedItemPosition

        if (
            position == 4 &&
            otherNote.text
                .toString()
                .trim()
                .isEmpty()
        ) {
            otherNote.error =
                "Keterangan wajib diisi"
            return
        }

        val expense =
            JSONObject().apply {

                put(
                    "category",
                    category.selectedItem.toString()
                )

                put(
                    "amount",
                    value
                )

                put(
                    "currency",
                    getCurrency()
                )

                put(
                    "note",
                    otherNote.text
                        .toString()
                        .trim()
                )

                put(
                    "timestamp",
                    System.currentTimeMillis()
                )
            }

        val prefs =
            getSharedPreferences(
                "expense",
                MODE_PRIVATE
            )

        val expenses =
            JSONArray(
                prefs.getString(
                    "expenses",
                    "[]"
                )
            )

        expenses.put(expense)

        prefs.edit()
            .putString(
                "expenses",
                expenses.toString()
            )
            .apply()

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

        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    24,
                    32,
                    24,
                    24
                )

                setBackgroundColor(
                    Color.WHITE
                )
            }

        val title =
            TextView(this).apply {
                text = "History"
                textSize = 30f
                setTextColor(Color.BLACK)
            }

        val period =
            Spinner(this)

        period.adapter =
            ArrayAdapter(
                this,
                android.R.layout
                    .simple_spinner_dropdown_item,
                arrayOf(
                    "Daily",
                    "Weekly",
                    "Monthly",
                    "Yearly"
                )
            )

        val totalLabel =
            TextView(this).apply {
                text = "Total Expense"
                textSize = 16f
                setTextColor(Color.BLACK)
            }

        val total =
            TextView(this).apply {
                textSize = 28f
                gravity = Gravity.CENTER
                setTextColor(Color.BLACK)
                setPadding(
                    0,
                    16,
                    0,
                    16
                )
            }

        val chart =
            TextView(this).apply {
                textSize = 16f
                gravity = Gravity.CENTER
                setTextColor(Color.BLACK)
                setPadding(
                    0,
                    24,
                    0,
                    24
                )
            }

        val detailed =
            Button(this).apply {

                text =
                    "DETAILED EXPENSE"

                setOnClickListener {

                    showDetailedExpense(
                        period.selectedItemPosition
                    )
                }
            }

        fun refresh() {

            val expenses =
                getExpenses()

            val filtered =
                expenses.filter {

                    matchesPeriod(
                        it.getLong("timestamp"),
                        period.selectedItemPosition
                    )
                }

            var sum = 0L

            for (expense in filtered) {
                sum += expense.getLong("amount")
            }

            total.text =
                "${currencyPrefix()} ${
                    formatCurrency(sum)
                }"

            chart.text =
                if (filtered.isEmpty()) {
                    "Chart kosong"
                } else {
                    buildSimpleChart(filtered)
                }
        }

        period.onItemSelectedListener =
            object :
                AdapterView
                    .OnItemSelectedListener {

                override fun
                    onNothingSelected(
                        parent:
                        AdapterView<*>?
                    ) {}

                override fun
                    onItemSelected(
                        parent:
                        AdapterView<*>?,
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

    private fun getExpenses():
        List<JSONObject> {

        val prefs =
            getSharedPreferences(
                "expense",
                MODE_PRIVATE
            )

        val array =
            JSONArray(
                prefs.getString(
                    "expenses",
                    "[]"
                )
            )

        val result =
            mutableListOf<JSONObject>()

        for (i in
            0 until array.length()) {

            result.add(
                array.getJSONObject(i)
            )
        }

        return result
    }

    private fun matchesPeriod(
        timestamp: Long,
        period: Int
    ): Boolean {

        val now =
            Calendar.getInstance()

        val date =
            Calendar.getInstance().apply {
                timeInMillis = timestamp
            }

        return when (period) {

            0 ->
                date.get(Calendar.YEAR) ==
                    now.get(Calendar.YEAR) &&
                date.get(Calendar.DAY_OF_YEAR) ==
                    now.get(Calendar.DAY_OF_YEAR)

            1 ->
                date.get(Calendar.YEAR) ==
                    now.get(Calendar.YEAR) &&
                date.get(Calendar.WEEK_OF_YEAR) ==
                    now.get(Calendar.WEEK_OF_YEAR)

            2 ->
                date.get(Calendar.YEAR) ==
                    now.get(Calendar.YEAR) &&
                date.get(Calendar.MONTH) ==
                    now.get(Calendar.MONTH)

            3 ->
                date.get(Calendar.YEAR) ==
                    now.get(Calendar.YEAR)

            else -> false
        }
    }

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

        val result =
            StringBuilder()

        for ((name, value) in categories) {

            result
                .append(name)
                .append("\n")

            result
                .append(currencyPrefix())
                .append(" ")
                .append(
                    formatCurrency(value)
                )
                .append("\n\n")
        }

        return result
            .toString()
            .trim()
    }

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

        val message =

            if (categories.isEmpty()) {

                "Belum ada pengeluaran."

            } else {

                categories.entries
                    .joinToString("\n\n") {

                        "${it.key}\n${
                            currencyPrefix()
                        } ${
                            formatCurrency(
                                it.value
                            )
                        }"
                    }
            }

        AlertDialog.Builder(this)
            .setTitle("Detailed Expense")
            .setMessage(message)
            .setPositiveButton(
                "OK",
                null
            )
            .show()
    }

    // =========================
    // REMINDER
    // =========================

    private fun showReminder() {

        val reminders =
            getReminders()

        val labels =
            mutableListOf<String>()

        for (reminder in reminders) {

            labels.add(
                "${reminder.getString("time")}  " +
                "${reminder.getString("message")}"
            )
        }

        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    24,
                    24,
                    24,
                    24
                )

                setBackgroundColor(
                    Color.WHITE
                )
            }

        val title =
            TextView(this).apply {
                text = "Reminder"
                textSize = 30f
                setTextColor(Color.BLACK)
            }

        val list =
            ListView(this)

        list.adapter =
            ArrayAdapter(
                this,
                android.R.layout
                    .simple_list_item_1,
                labels
            )

        list.setOnItemClickListener {
                _, _, position, _ ->

            editReminder(position)
        }

        val add =
            Button(this).apply {

                text = "ADD REMINDER"

                setOnClickListener {
                    editReminder(-1)
                }
            }

        root.addView(title)
        root.addView(
            list,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )
        root.addView(add)

        setContentView(root)
    }

    private fun getReminders():
        MutableList<JSONObject> {

        val prefs =
            getSharedPreferences(
                "expense",
                MODE_PRIVATE
            )

        val array =
            JSONArray(
                prefs.getString(
                    "reminders",
                    "[]"
                )
            )

        val result =
            mutableListOf<JSONObject>()

        for (i in
            0 until array.length()) {

            result.add(
                array.getJSONObject(i)
            )
        }

        return result
    }

    private fun saveReminders(
        reminders: List<JSONObject>
    ) {

        val array = JSONArray()

        for (reminder in reminders) {
            array.put(reminder)
        }

        getSharedPreferences(
            "expense",
            MODE_PRIVATE
        )
            .edit()
            .putString(
                "reminders",
                array.toString()
            )
            .apply()
    }

    private fun editReminder(
        index: Int
    ) {

        val reminders =
            getReminders()

        val existing =
            if (index >= 0) {
                reminders[index]
            } else {
                null
            }

        val message =
            EditText(this).apply {

                hint =
                    "Tulis pesan reminder"

                inputType =
                    InputType.TYPE_CLASS_TEXT

                setSingleLine(false)

                if (existing != null) {
                    setText(
                        existing.getString(
                            "message"
                        )
                    )
                }
            }

        val timeButton =
            Button(this).apply {

                text =
                    if (existing != null) {
                        existing.getString("time")
                    } else {
                        "Pilih waktu"
                    }
            }

        var selectedHour =
            if (existing != null) {
                existing
                    .getString("time")
                    .substringBefore(":")
                    .toInt()
            } else {
                9
            }

        var selectedMinute =
            if (existing != null) {
                existing
                    .getString("time")
                    .substringAfter(":")
                    .toInt()
            } else {
                0
            }

        timeButton.setOnClickListener {

            val picker =
                TimePickerDialog(
                    this,
                    { _, hour, minute ->

                        selectedHour = hour
                        selectedMinute = minute

                        timeButton.text =
                            String.format(
                                Locale.US,
                                "%02d:%02d",
                                hour,
                                minute
                            )
                    },
                    selectedHour,
                    selectedMinute,
                    true
                )

            picker.show()
        }

        val container =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    24,
                    8,
                    24,
                    8
                )

                addView(message)
                addView(timeButton)
            }

        val builder =
            AlertDialog.Builder(this)
                .setTitle(
                    if (index >= 0)
                        "Edit Reminder"
                    else
                        "Add Reminder"
                )
                .setView(container)

        if (index >= 0) {

            builder.setNeutralButton(
                "DELETE"
            ) { _, _ ->

                cancelReminder(
                    reminders[index]
                )

                reminders.removeAt(index)

                saveReminders(reminders)

                showReminder()
            }
        }

        builder
            .setPositiveButton(
                "SAVE"
            ) { _, _ ->

                val text =
                    message.text
                        .toString()
                        .trim()

                if (text.isEmpty()) {
                    return@setPositiveButton
                }

                val id =
                    if (existing != null) {
                        existing.getInt("id")
                    } else {
                        (System.currentTimeMillis() and
                            0x7FFFFFFF)
                            .toInt()
                    }

                val reminder =
                    JSONObject().apply {

                        put("id", id)

                        put(
                            "message",
                            text
                        )

                        put(
                            "time",
                            String.format(
                                Locale.US,
                                "%02d:%02d",
                                selectedHour,
                                selectedMinute
                            )
                        )
                    }

                if (index >= 0) {

                    cancelReminder(
                        reminders[index]
                    )

                    reminders[index] =
                        reminder

                } else {

                    reminders.add(
                        reminder
                    )
                }

                saveReminders(reminders)

                scheduleReminder(
                    reminder
                )

                showReminder()
            }
            .setNegativeButton(
                "CANCEL",
                null
            )
            .show()
    }

    // =========================
    // ALARM
    // =========================

    private fun createNotificationChannel() {

        if (
            android.os.Build.VERSION.SDK_INT >= 26
        ) {

            val channel =
                NotificationChannel(
                    "expense_reminder",
                    "Expense Reminder",
                    NotificationManager
                        .IMPORTANCE_DEFAULT
                )

            val manager =
                getSystemService(
                    NotificationManager::class.java
                )

            manager.createNotificationChannel(
                channel
            )
        }
    }

    private fun scheduleReminder(
        reminder: JSONObject
    ) {

        val time =
            reminder.getString("time")

        val hour =
            time.substringBefore(":")
                .toInt()

        val minute =
            time.substringAfter(":")
                .toInt()

        val calendar =
            Calendar.getInstance().apply {

                set(
                    Calendar.HOUR_OF_DAY,
                    hour
                )

                set(
                    Calendar.MINUTE,
                    minute
                )

                set(
                    Calendar.SECOND,
                    0
                )

                set(
                    Calendar.MILLISECOND,
                    0
                )

                if (
                    before(
                        Calendar.getInstance()
                    )
                ) {
                    add(
                        Calendar.DAY_OF_YEAR,
                        1
                    )
                }
            }

        val intent =
            Intent(
                this,
                ReminderReceiver::class.java
            ).apply {

                putExtra(
                    "id",
                    reminder.getInt("id")
                )

                putExtra(
                    "message",
                    reminder.getString(
                        "message"
                    )
                )
            }

        val pending =
            PendingIntent.getBroadcast(
                this,
                reminder.getInt("id"),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )

        val alarm =
            getSystemService(
                ALARM_SERVICE
            ) as AlarmManager

        alarm.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pending
        )
    }

    private fun cancelReminder(
        reminder: JSONObject
    ) {

        val intent =
            Intent(
                this,
                ReminderReceiver::class.java
            )

        val pending =
            PendingIntent.getBroadcast(
                this,
                reminder.getInt("id"),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )

        val alarm =
            getSystemService(
                ALARM_SERVICE
            ) as AlarmManager

        alarm.cancel(pending)
    }

    // =========================
    // SETTINGS
    // =========================

    private fun showSettings() {

        val items = arrayOf(
            "Currency: ${getCurrency()}",
            "Language: Indonesia",
            "Conversion Rate",
            "Disclaimer"
        )

        AlertDialog.Builder(this)
            .setTitle("Setting")
            .setItems(items) { _, which ->

                when (which) {

                    0 -> chooseCurrency()

                    1 -> {}

                    2 -> {}

                    3 ->
                        showDisclaimer()
                }
            }
            .setNegativeButton(
                "OK",
                null
            )
            .show()
    }

    private fun chooseCurrency() {

        val currencies =
            arrayOf(
                "IDR",
                "USD"
            )

        val current =
            if (
                getCurrency() == "USD"
            ) 1 else 0

        AlertDialog.Builder(this)
            .setTitle("Currency")
            .setSingleChoiceItems(
                currencies,
                current
            ) { dialog, which ->

                getSharedPreferences(
                    "expense",
                    MODE_PRIVATE
                )
                    .edit()
                    .putString(
                        "currency",
                        currencies[which]
                    )
                    .apply()

                dialog.dismiss()
                showExpense()
            }
            .show()
    }

    private fun showDisclaimer() {

        AlertDialog.Builder(this)
            .setTitle("Disclaimer")
            .setMessage(
                "Expense is a simple personal " +
                "expense tracker. Use the data " +
                "at your own discretion."
            )
            .setPositiveButton(
                "OK",
                null
            )
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
            .setPositiveButton(
                "OK",
                null
            )
            .show()
    }

    // =========================
    // BACK
    // =========================

    override fun onBackPressed() {
        showExpense()
    }
}
