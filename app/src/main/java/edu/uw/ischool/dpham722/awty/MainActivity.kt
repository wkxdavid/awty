package edu.uw.ischool.dpham722.awty

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.telephony.PhoneNumberUtils
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
            isAwtyActivated = true
            updateUIForStart()

            val formattedPhone = PhoneNumberUtils.formatNumber(phone, Locale.getDefault().country)
            scheduleRepeatingAlarm(formattedPhone, message, intervalMillis)
        } else {
            Toast.makeText(this, "Oh noes, one or more values are invalid.", Toast.LENGTH_LONG).show()
        }
    }

    private fun stopAwty() {
        unregisterReceiver(receiver)
        receiver = null

        isAwtyActivated = false
        updateUIForStop()
    }

    private fun updateUIForStart() {
        startStopBtn.text = getString(R.string.stop_button)
    }

    private fun updateUIForStop() {
        startStopBtn.text = getString(R.string.start_button)
    }

    private fun scheduleRepeatingAlarm(phone: String, message: String, intervalMillis: Long?) {
        val intent = Intent(ALARM_ACTION)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val alarmManager: AlarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis(),
            intervalMillis!! * MIN_IN_MILLIS,
            pendingIntent
        )

        if (receiver == null) {
            receiver = createBroadcastReceiver(phone, message)
            val filter = IntentFilter(ALARM_ACTION)
            registerReceiver(receiver, filter)
        }
    }

    private fun createBroadcastReceiver(phone: String, message: String): BroadcastReceiver {
        return object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Log.i(TAG, "received. making toast...")
                showCustomToast(phone, message)
            }
        }
    }

    private fun showCustomToast(phone: String, message: String) {
        val customToastView = layoutInflater.inflate(R.layout.custom_toast, null)
        val toastCaptionTextView = customToastView.findViewById<TextView>(R.id.captionTextView)
        val toastMessageTextView = customToastView.findViewById<TextView>(R.id.messageTextView)

        toastCaptionTextView.text = getString(R.string.texting_phone, phone)
        toastMessageTextView.text = message

        val customToast = Toast(this).apply {
            duration = Toast.LENGTH_LONG
            view = customToastView
        }
        customToast.show()
    }
}