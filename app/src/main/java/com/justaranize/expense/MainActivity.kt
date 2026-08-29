package com.justaranize.expense

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
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

        createNotificationChannel()
        showExpense()

        if (
            android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                100
            )
        }
    }

    // =========================
    // EXPENSE
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
            text = "Pengeluaran"
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
                hideKeyboard()
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
                "Makanan",
                "Transportasi",
                "Belanja",
                "Tagihan",
                "Lainnya"
            )
        )

        otherNote = EditText(this).apply {
            hint = "Keterangan"
            visibility = View.GONE
            setSingleLine(true)
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
    inputType = InputType.TYPE_CLASS_NUMBER
    gravity = Gravity.CENTER
    textSize = 28f
    setSingleLine(true)

    setTextColor(Color.BLACK)
    background = null

    setText("Rp 0")
    setSelection(text.length)
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
                        ?.replace(" ", "")
                        ?.trim()

                    if (raw.isNullOrEmpty()) return

                    val number =
                        raw.toLongOrNull()
                            ?: return

                    formattingAmount = true

                    amount.setText(
                        formatInput(number)
                    )

                    amount.setSelection(
                        amount.text.length
                    )

                    formattingAmount = false
                }
            }
        )

        val save = Button(this).apply {
            text = "SIMPAN PENGELUARAN"

            setOnClickListener {
                saveExpense()
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

    // =========================
    // INPUT
    // =========================

    private fun formatInput(value: Long): String {

        val formatted =
            NumberFormat
                .getNumberInstance(
                    Locale("id", "ID")
                )
                .format(value)

        return "Rp $formatted"
    }

    private fun formatCurrency(value: Long): String {

        return NumberFormat
            .getNumberInstance(
                Locale("id", "ID")
            )
            .format(value)
    }

    private fun getRawAmount(): Long? {

        val raw = amount.text
            .toString()
            .replace(".", "")
            .replace(",", "")
            .replace("Rp", "")
            .replace(" ", "")
            .trim()

        return raw.toLongOrNull()
    }

    private fun saveExpense() {

        val value = getRawAmount()

        if (value == null || value <= 0) {
            amount.error = "Masukkan nominal"
            return
        }

        val position =
            category.selectedItemPosition

        val note =
            otherNote.text
                .toString()
                .trim()

        if (position == 4 && note.isEmpty()) {
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
                    "IDR"
                )

                put(
                    "note",
                    note
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

        // Setelah selesai input kembali kosong
        amount.text.clear()
        otherNote.text.clear()
        category.setSelection(0)

        hideKeyboard()
    }

    // =========================
    // MENU
    // =========================

    private fun showMenu() {

        val items = arrayOf(
            "Riwayat",
            "Pengingat",
            "Tentang",
            "Keluar"
        )

        AlertDialog.Builder(this)
            .setTitle("Menu")
            .setItems(items) { _, which ->

                when (which) {

                    0 -> showHistory()

                    1 -> showReminder()

                    2 -> showAbout()

                    3 -> finish()
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

                text = "Riwayat"
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
                    "Harian",
                    "Mingguan",
                    "Bulanan",
                    "Tahunan"
                )
            )

        val totalLabel =
            TextView(this).apply {

                text = "Total Pengeluaran"
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

                text = "RINCIAN PENGELUARAN"

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
                "Rp ${formatCurrency(sum)}"

            chart.text =
                if (filtered.isEmpty()) {
                    "Chart kosong"
                } else {
                    buildChart(filtered)
                }
        }

        period.onItemSelectedListener =
            object :
                AdapterView.OnItemSelectedListener {

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

        for (i in 0 until array.length()) {
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

    private fun buildChart(
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

        return categories.entries
            .joinToString("\n\n") {

                "${it.key}\nRp ${
                    formatCurrency(it.value)
                }"
            }
    }

    // =========================
    // DETAILED
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

        val message =
            if (categories.isEmpty()) {

                "Belum ada pengeluaran."

            } else {

                categories.entries
                    .joinToString("\n\n") {

                        "${it.key}\nRp ${
                            formatCurrency(
                                it.value
                            )
                        }"
                    }
            }

        AlertDialog.Builder(this)
            .setTitle(
                "Rincian Pengeluaran"
            )
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

                text = "Pengingat"
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

                text = "TAMBAH PENGINGAT"

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

        for (i in 0 until array.length()) {
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

                hint = "Tulis pesan pengingat"

                inputType =
                    InputType.TYPE_CLASS_TEXT

                if (existing != null) {
                    setText(
                        existing.getString(
                            "message"
                        )
                    )
                }
            }

        val timeButton =
            Button(this)

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

        timeButton.text =
            String.format(
                Locale.US,
                "%02d:%02d",
                selectedHour,
                selectedMinute
            )

        timeButton.setOnClickListener {

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
            ).show()
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
                        "Edit Pengingat"
                    else
                        "Tambah Pengingat"
                )
                .setView(container)

        if (index >= 0) {

            builder.setNeutralButton(
                "HAPUS"
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
                "SIMPAN"
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

                        (
                            System.currentTimeMillis()
                                and 0x7FFFFFFF
                            ).toInt()
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
                "BATAL",
                null
            )
            .show()
    }

    // =========================
    // NOTIFICATION
    // =========================

    private fun createNotificationChannel() {

        if (
            android.os.Build.VERSION.SDK_INT >= 26
        ) {

            val channel =
                NotificationChannel(
                    "expense_reminder",
                    "Pengingat Pengeluaran",
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
    // ABOUT
    // =========================

    private fun showAbout() {

        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    24,
                    16,
                    24,
                    8
                )
            }

        val message =
            TextView(this).apply {

                text =
                    "And app made with the spirit of lazyness 🤣"

                textSize = 18f
                setTextColor(Color.BLACK)

                setPadding(
                    0,
                    8,
                    0,
                    24
                )
            }

        val donate =
            Button(this).apply {

                text = "DONATE"

                setOnClickListener {

                    try {

                        val intent =
                            Intent(
                                Intent.ACTION_VIEW,
                                android.net.Uri.parse(
                                    "https://www.buymeacoffee.com/"
                                )
                            )

                        startActivity(intent)

                    } catch (_: Exception) {}
                }
            }

        root.addView(message)
        root.addView(donate)

        AlertDialog.Builder(this)
            .setTitle("Tentang")
            .setView(root)
            .setPositiveButton(
                "OK",
                null
            )
            .show()
    }

    // =========================
    // KEYBOARD
    // =========================

    private fun hideKeyboard() {

        val imm =
            getSystemService(
                INPUT_METHOD_SERVICE
            ) as InputMethodManager

        imm.hideSoftInputFromWindow(
            currentFocus?.windowToken,
            0
        )

        currentFocus?.clearFocus()
    }

    // =========================
    // BACK
    // =========================

    override fun onBackPressed() {

        if (
            amount.text
                .toString()
                .isNotEmpty()
        ) {
            hideKeyboard()
        }

        showExpense()
    }
}
