package edu.uw.ischool.dpham722.awty

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.Manifest.permission.SEND_SMS
import android.os.Bundle
import android.telephony.PhoneNumberUtils
import android.telephony.SmsManager
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale

const val ALARM_ACTION = "edu.uw.ischool.dpham722.ALARM"
const val TAG = "awty"
const val MIN_IN_MILLIS = 60 * 1000

class MainActivity : AppCompatActivity() {
    private var isAwtyActivated: Boolean = false

    private var receiver: BroadcastReceiver? = null

    private lateinit var messageEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var minsEditText: EditText
    private lateinit var startStopBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        messageEditText = findViewById(R.id.messageText)
        phoneEditText = findViewById(R.id.phoneText)
        minsEditText = findViewById(R.id.minsIntervalText)
        startStopBtn = findViewById(R.id.startStopBtn)

        startStopBtn.setOnClickListener {
            if (!isAwtyActivated) {
                startAwty()
            } else {
                stopAwty()
            }
        }
    }

    private fun startAwty() {
        val message = messageEditText.text.toString()
        val phone = phoneEditText.text.toString()
        val intervalMins = minsEditText.text.toString()
        val intervalMillis = intervalMins.toLongOrNull()

        if (message.isNotEmpty() && phone.isNotEmpty() && intervalMillis?.let { it > 0 } == true) {
            if (ContextCompat.checkSelfPermission(this, SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                isAwtyActivated = true
                updateUIForStart()

                val formattedPhone = PhoneNumberUtils.formatNumber(phone, Locale.getDefault().country)
                scheduleRepeatingAlarm(formattedPhone, message, intervalMillis)
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(SEND_SMS), 1)
            }
        } else {
            Toast.makeText(this, "Oh no! Cannot start AWTY because one or more values are invalid. Fix and try again.", Toast.LENGTH_LONG).show()
        }
    }

    private fun stopAwty() {
        if (receiver != null) {
            unregisterReceiver(receiver)
            receiver = null
        }

        isAwtyActivated = false
        updateUIForStop()
    }

    private fun updateUIForStart() {
        startStopBtn.text = getString(R.string.stop_button)
    }

    private fun updateUIForStop() {
        startStopBtn.text = getString(R.string.start_button)
    }

    private fun scheduleRepeatingAlarm(phone: String, message: String, intervalMillis: Long) {
        val intent = Intent(ALARM_ACTION)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val alarmManager: AlarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis(),
            intervalMillis * MIN_IN_MILLIS,
            pendingIntent
        )

        if (receiver == null) {
            receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    Log.i(TAG, "Received request to send SMS to $phone")
                    val smsManager = SmsManager.getDefault()
                    smsManager.sendTextMessage(phone, null, message, null, null)
                    Log.i(TAG, "Sent '$message' to $phone")
                }
            }
            val filter = IntentFilter(ALARM_ACTION)
            registerReceiver(receiver, filter)
        }
    }
}
