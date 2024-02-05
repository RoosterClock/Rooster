package com.rooster.rooster

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Calendar


class AlarmAdapter(private val alarmList: List<Alarm>, val alarmDbHelper: AlarmDbHelper) : RecyclerView.Adapter<AlarmAdapter.ViewHolder>() {

    val alarmModes = arrayOf("At", "Before", "Between", "After")
    var alarmRelatives = arrayOf(
        "Pick Time",
        "Astronomical Dawn",
        "Nautical Dawn",
        "Civil Dawn",
        "Sunrise",
        "Solar Noon",
        "Sunset",
        "Civil Dusk",
        "Nautical Dusk",
        "Astronomical Dusk")

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val alarmContainer: LinearLayout = itemView.findViewById(R.id.alarmContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.alarm_item_short, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val container = holder.alarmContainer
        val context = container.context
        var alarm = alarmList[position]
        var alarmExtension = holder.alarmContainer.findViewById<LinearLayout>(R.id.alarmExtension)

        linkButtons(holder, container, context, alarm)
    }

    private fun linkButtons(holder: ViewHolder, container: LinearLayout, context: Context, alarm: Alarm) {


        // Label

        val tvLabel = container.findViewById<TextView>(R.id.textViewAlarmLabel)
        tvLabel.setOnClickListener {
            val input = EditText(context)
            input.hint = "Enter new alarm label"
            input.setText(alarm.label)

            // Create a dialog box to display the text input
            val dialog = AlertDialog.Builder(context)
                .setTitle("Edit Alarm Label")
                .setView(input)
                .setPositiveButton("Save") { _, _ ->
                    alarm.label = input.text.toString()
                    alarmDbHelper.updateAlarm(alarm)
                    reloadAlarmList(holder)
                }
                .setNegativeButton("Cancel") { _, _ -> }
                .create()

            // Show the dialog box
            dialog.show()
        }
        tvLabel.setText(alarm.label)


        // Expand Button

        var extendedView = container.findViewById<LinearLayout>(R.id.alarmExtension)
        var expandButton = container.findViewById<ImageButton>(R.id.expandButton)
        expandButton.setOnClickListener {
            // Check to make sure that the extended view is not already visible before making it visible.
            if (extendedView.visibility == View.GONE) {
                extendedView.visibility = View.VISIBLE
                expandButton.rotation = 180.0F
            } else {
                extendedView.visibility = View.GONE
                expandButton.rotation = 0F
            }
        }

        // Time
        val pickRaw = container.findViewById<LinearLayout>(R.id.timePickRaw)
        pickRaw.setOnClickListener{
            promptModePick(context, container, alarm, holder)
        }
        arrangeLayout(context, container, alarm, alarm.mode, holder)


        // Enabled
        val swicthEnabled = container.findViewById<Switch>(R.id.switchAlarmEnabled)
        swicthEnabled.isChecked = alarm.enabled
        swicthEnabled.setOnCheckedChangeListener { _, isChecked ->
            alarm.enabled = isChecked
            alarmDbHelper.updateAlarm(alarm)
        }

        // Week Days

        val days = arrayListOf<String>("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")
        for (d in days) {
            val dynamicId = context.resources.getIdentifier("$d"+"Button", "id", context.packageName);
            val dynamicButton = holder.alarmContainer.findViewById<Button>(dynamicId)
            dynamicButton.setOnClickListener{
                onDaysClicked(it, alarm)
                setButtonState(dynamicButton, dynamicButton.isSelected)
            }
            dynamicButton.isSelected = alarm.getDayEnabled(d)
            setButtonState(dynamicButton, dynamicButton.isSelected)
        }

        val ringtoneTitle = holder.alarmContainer.findViewById<TextView>(R.id.ringtoneTitle)
        val title = getRingtoneTitleFromUri(holder.alarmContainer.context, alarm.ringtoneUri)
        ringtoneTitle.text = title
        val ringtoneButton = holder.alarmContainer.findViewById<LinearLayout>(R.id.layoutRingtone)
        ringtoneButton.setOnClickListener {
            val ringtoneActivity = Intent(context, RingtoneActivity::class.java)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M || Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtoneActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val b = Bundle()
            b.putLong("alarm_id", alarm.id)
            ringtoneActivity.putExtras(b);
            context.applicationContext.startActivity(ringtoneActivity);
        }

        // Vibrate
        var vibrateButton = holder.alarmContainer.findViewById<CheckBox>(R.id.checkBoxVibrate)
        vibrateButton.setOnClickListener{
            // Update alarm
        }

        // Delete
        var deleteButton = holder.alarmContainer.findViewById<ImageButton>(R.id.buttonDelete)
        deleteButton.setOnClickListener{
            Log.e("Delete", "Alarm ID: " + alarm.id.toString())
            alarmDbHelper.deleteAlarm(alarm.id)
            reloadAlarmList(holder)
        }
        var deleteText = holder.alarmContainer.findViewById<TextView>(R.id.buttonDeleteText)
        deleteText.setOnClickListener{
            Log.e("Delete", "Alarm ID: " + alarm.id.toString())
            alarmDbHelper.deleteAlarm(alarm.id)
            reloadAlarmList(holder)
        }
    }

    private fun promptModePick(
        context: Context,
        container: LinearLayout,
        alarm: Alarm,
        holder: ViewHolder
    ) {
        val dialog = AlertDialog.Builder(context)
            .setTitle("Choose Time Mode")
            .setItems(alarmModes) { dialog, which ->
                // Update the alarm mode
                alarm.mode = alarmModes[which]
                alarmDbHelper.updateAlarm(alarm)
                arrangeLayout(context, container, alarm, alarmModes[which], holder)
            }
            .create()
        dialog.show()
    }

    private fun arrangeLayout(
        context: Context,
        container: LinearLayout,
        alarm: Alarm,
        s: String,
        holder: ViewHolder
    ) {
        when(s) {
            "At" -> arrangeLayoutAt(context, container, alarm, holder)
            "After" -> arrangeLayoutAfterBefore(context, container, alarm, holder)
            "Before" -> arrangeLayoutAfterBefore(context, container, alarm, holder)
            "Between" -> arrangeLayoutBetween(context, container, alarm, holder)
        }
    }

    private fun arrangeLayoutAt(
        context: Context,
        container: LinearLayout,
        alarm: Alarm,
        holder: ViewHolder
    ) {
        cleanView(container)

        var tvTime1 = container.findViewById<TextView>(R.id.tvAlarmTime1)
        var tvCal = container.findViewById<TextView>(R.id.calculatedTime)

        tvTime1.visibility = View.VISIBLE

        var time = alarm.getFormattedTime(alarm.calculatedTime, false)
        if (alarm.relative1 != "Pick Time") {
            tvTime1.setText(alarm.relative1)
            tvCal.setText(time)
        } else {
            tvCal.setText(time)
        }
        tvTime1.setOnClickListener {
            promptRelativePick(context, container, alarm, 1, holder)
        }
        tvCal.setOnClickListener {
            promptRelativePick(context, container, alarm, 1, holder)
        }
    }

    private fun cleanView(container: LinearLayout) {
        var tvTime0 = container.findViewById<TextView>(R.id.tvAlarmTime0)
        var tvTime1 = container.findViewById<TextView>(R.id.tvAlarmTime1)
        var tvTime2 = container.findViewById<TextView>(R.id.tvAlarmTime2)
        var tvTime3 = container.findViewById<TextView>(R.id.tvAlarmTime3)
        var tvTime4 = container.findViewById<TextView>(R.id.tvAlarmTime4)
        var tvTime5 = container.findViewById<TextView>(R.id.tvAlarmTime5)
        var tvTimeCalculated = container.findViewById<TextView>(R.id.calculatedTime)

        tvTime0.setText("At")
        tvTime1.setText("")
        tvTime2.setText("")
        tvTime3.setText("")
        tvTime4.setText("")
        tvTime5.setText("")

        var textSize = 15F
        tvTime1.textSize = textSize
        tvTime2.textSize = textSize
        tvTime3.textSize = textSize
        tvTime4.textSize = textSize
        tvTime5.textSize = textSize

        tvTime1.visibility = View.VISIBLE
        tvTime1.visibility = View.VISIBLE
        tvTime2.visibility = View.VISIBLE
        tvTime3.visibility = View.VISIBLE
        tvTime4.visibility = View.VISIBLE
        tvTime5.visibility = View.VISIBLE
        tvTimeCalculated.visibility = View.VISIBLE

        tvTime2.setOnClickListener {  }
        tvTime3.setOnClickListener {  }
        tvTime4.setOnClickListener {  }
        tvTime5.setOnClickListener {  }
    }

    private fun promptRelativePick(
        context: Context,
        container: LinearLayout,
        alarm: Alarm,
        index: Int,
        holder: ViewHolder
    ) {
        val dialog = AlertDialog.Builder(context)
            .setTitle("Choose Time Mode")
            .setItems(alarmRelatives) { dialog, which ->
                if (index == 1) {
                    alarm.relative1 = alarmRelatives[which]
                } else if (index == 2) {
                    alarm.relative2 = alarmRelatives[which]
                }
                val pickerDialog = when (alarmRelatives[which]) {
                    "Pick Time" -> {
                        TimePickerDialog(context, { _, hour, minute ->
                            val swicthEnabled = container.findViewById<Switch>(R.id.switchAlarmEnabled)
                            val calendar = Calendar.getInstance()
                            calendar.set(Calendar.HOUR_OF_DAY, hour)
                            calendar.set(Calendar.MINUTE, minute)
                            if (index == 1) {
                                alarm.time1 = calendar.time.time
                            } else if (index == 2) {
                                alarm.time2 = calendar.time.time
                            }
                            alarm.enabled = true
                            swicthEnabled.isChecked = alarm.enabled
                            alarmDbHelper.updateAlarm(alarm)
                            arrangeLayout(context, container, alarm, alarm.mode, holder)
                        }, (alarm.time1 / 3600).toInt(), (alarm.time1 % 60 / 60).toInt(), true)
                    }
                    else -> {
                        // Update the alarm in the database
                        if (index == 1) {
                            alarm.time1 = alarmDbHelper.getRelativeTime(alarm.relative1)
                        } else if (index == 2) {
                            alarm.time2 = alarmDbHelper.getRelativeTime(alarm.relative2)
                        }

                        val swicthEnabled = container.findViewById<Switch>(R.id.switchAlarmEnabled)
                        swicthEnabled.isChecked = alarm.enabled
                        alarmDbHelper.updateAlarm(alarm)
                        arrangeLayout(context, container, alarm, alarm.mode, holder)
                        null
                    }
                }
                pickerDialog?.show()
            }
            .create()
        dialog.show()
    }

    private fun arrangeLayoutAfterBefore(
        context: Context,
        container: LinearLayout,
        alarm: Alarm,
        holder: ViewHolder
    ) {
        cleanView(container)
        var tvTime1 = container.findViewById<TextView>(R.id.tvAlarmTime1)
        var tvTime2 = container.findViewById<TextView>(R.id.tvAlarmTime2)
        var tvTime3 = container.findViewById<TextView>(R.id.tvAlarmTime3)
        var tvTime4 = container.findViewById<TextView>(R.id.tvAlarmTime4)
        var tvTime5 = container.findViewById<TextView>(R.id.tvAlarmTime5)
        var tvTimeCalculated = container.findViewById<TextView>(R.id.calculatedTime)
        tvTimeCalculated.setText(alarm.getFormattedTime(alarm.calculatedTime, true))
        tvTimeCalculated.visibility = View.VISIBLE

        tvTime2.visibility = View.VISIBLE
        tvTime3.visibility = View.VISIBLE
        tvTime4.visibility = View.VISIBLE

        tvTime2.setText(alarm.getFormattedTime(alarm.time1, false))
        tvTime3.setText(alarm.mode)

        if (alarm.relative2 != "Pick Time") {
            tvTime5.setText("(" + alarm.getFormattedTime(alarm.time2, true) + ")")
            tvTime4.setText(alarm.relative2)
            tvTime5.visibility = View.VISIBLE
        } else {
            tvTime5.setText("")
            tvTime5.visibility = View.VISIBLE
        }
        tvTime2.setOnClickListener {
            var picker = TimePickerDialog(context, { _, hour, minute ->
                val swicthEnabled = container.findViewById<Switch>(R.id.switchAlarmEnabled)
                alarm.time1 = (hour.toLong() * 60 + minute) * 60
                alarm.enabled = true
                swicthEnabled.isChecked = alarm.enabled
                Log.e("ALARM", alarm.getFormattedTime(alarm.time1, false).toString())
                alarmDbHelper.updateAlarm(alarm)
                arrangeLayout(context, container, alarm, alarm.mode, holder)
            }, (alarm.time1 / 3600).toInt(), (alarm.time1 % 60 / 60).toInt(), true)
            picker.show()
        }

        tvTime3.setOnClickListener {
            promptModePick(context, container, alarm, holder)
        }
        tvTime4.setOnClickListener {
            promptRelativePick(context, container, alarm, 2, holder)
            arrangeLayout(context, container, alarm, alarm.mode, holder)
        }
        tvTime5.setOnClickListener {
            promptRelativePick(context, container, alarm, 2, holder)
            arrangeLayout(context, container, alarm, alarm.mode, holder)
        }
    }

    private fun arrangeLayoutBetween(
        context: Context,
        container: LinearLayout,
        alarm: Alarm,
        holder: ViewHolder
    ) {
        cleanView(container)
        var tvTime0 = container.findViewById<TextView>(R.id.tvAlarmTime0)
        var tvTime1 = container.findViewById<TextView>(R.id.tvAlarmTime1)
        var tvTime2 = container.findViewById<TextView>(R.id.tvAlarmTime2)
        var tvTime3 = container.findViewById<TextView>(R.id.tvAlarmTime3)
        var tvTime4 = container.findViewById<TextView>(R.id.tvAlarmTime4)
        var tvTime5 = container.findViewById<TextView>(R.id.tvAlarmTime5)
        var tvTimeCalculated = container.findViewById<TextView>(R.id.calculatedTime)
        tvTimeCalculated.setText(alarm.getFormattedTime(alarm.calculatedTime, false))
        tvTimeCalculated.visibility = View.VISIBLE


        tvTime1.visibility = View.VISIBLE
        tvTime2.visibility = View.VISIBLE
        tvTime3.visibility = View.VISIBLE
        tvTime4.visibility = View.VISIBLE

        tvTime0.setText("Between")
        tvTime1.setText(alarm.getFormattedTime(alarm.time1, false))
        tvTime3.setText("and")
        tvTime4.setText(alarm.getFormattedTime(alarm.time2, false))

        if (alarm.relative1 != "Pick Time") {
            tvTime2.setText("(" + alarm.getFormattedTime(alarm.time1, false) + ")")
            tvTime1.setText(alarm.relative1)
            tvTime2.visibility = View.VISIBLE
        } else {
            tvTime2.setText("")
            tvTime2.visibility = View.VISIBLE
        }

        if (alarm.relative2 != "Pick Time") {
            tvTime5.setText("(" + alarm.getFormattedTime(alarm.time2, false) + ")")
            tvTime4.setText(alarm.relative2)
            tvTime5.visibility = View.VISIBLE
        } else {
            tvTime5.setText("")
            tvTime5.visibility = View.VISIBLE
        }

        tvTime1.setOnClickListener {
            promptRelativePick(context, container, alarm, 1, holder)
            arrangeLayout(context, container, alarm, alarm.mode, holder)
        }

        tvTime2.setOnClickListener {
            promptRelativePick(context, container, alarm, 1, holder)
            arrangeLayout(context, container, alarm, alarm.mode, holder)
        }

        tvTime3.setOnClickListener {
            promptModePick(context, container, alarm, holder)
        }
        tvTime4.setOnClickListener {
            promptRelativePick(context, container, alarm, 2, holder)
            arrangeLayout(context, container, alarm, alarm.mode, holder)
        }
        tvTime5.setOnClickListener {
            promptRelativePick(context, container, alarm, 2, holder)
            arrangeLayout(context, container, alarm, alarm.mode, holder)
        }
    }

    override fun getItemCount(): Int {
        return alarmList.size
    }

    private fun reloadAlarmList(holder: ViewHolder) {
        val activity = holder.alarmContainer.context as AlarmListActivity
        activity.reloadAlarmList()
    }

    fun onDaysClicked(view: View, alarm: Alarm) {
        Log.e("Update", "Alarm ID: " + alarm.id.toString())
        if (view is Button) {
            val day = when (view.id) {
                R.id.mondayButton -> "monday"
                R.id.tuesdayButton -> "tuesday"
                R.id.wednesdayButton -> "wednesday"
                R.id.thursdayButton -> "thursday"
                R.id.fridayButton -> "friday"
                R.id.saturdayButton -> "saturday"
                R.id.sundayButton -> "sunday"
                else -> return
            }
            val isChecked = !view.isSelected
            view.isSelected = isChecked
            alarm.setDayEnabled(day, isChecked)
            alarmDbHelper.updateAlarm(alarm)
        }
    }

    private fun setButtonState(button: Button?, isSelected: Boolean) {
        button?.isSelected = isSelected
        val textColor: Int
        val bgDrawable: Int

        if (isSelected) {
            if (button != null) {
                textColor = button.context.getColor(R.color.astronomical)
                button?.setTextColor(textColor)
            }
            bgDrawable = R.drawable.rounded_button_selected
        } else {
            if (button != null) {
                textColor = button.context.getColor(R.color.primary)
                button?.setTextColor(textColor)
            }
            bgDrawable = R.drawable.rounded_button
        }
        button?.setBackgroundResource(bgDrawable)
    }
    fun getRingtoneTitleFromUri(context: Context, ringtoneUri: String?): String {
        if (ringtoneUri == null) return "Default Ringtone" // Return a default or an indication that no custom ringtone is set

        try {
            val uri = Uri.parse(ringtoneUri)
            val ringtone = RingtoneManager.getRingtone(context, uri)
            return ringtone.getTitle(context)
        } catch (e: Exception) {
            e.printStackTrace()
            return "Unknown Ringtone" // In case of any error, return a default name
        }
    }
}