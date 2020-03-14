package com.example.cradle_vsa_sms_relay

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class SmsService : Service(), MessageListener{
    val CHANNEL_ID = "ForegroundServiceChannel"
    private val readingServerUrl =
        "https://cmpt373.csil.sfu.ca:8048/api/patient/reading"
    private val referralsServerUrl = "https://cmpt373.csil.sfu.ca:8048/api/referral"
    private val referralSummeriesServerUrl =
        "https://cmpt373.csil.sfu.ca:8048/api/mobile/summarized/follow_up"
    private val binder = ServiceToActivityBroadCastReciever()


    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        MessageReciever.bindListener(this)
        val input = intent?.getStringExtra("inputExtra")
        createNotificationChannel()
        val notificationIntent = Intent(this,MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this,0,notificationIntent,0)
        val notification = NotificationCompat.Builder(this,CHANNEL_ID)
            .setContentTitle("SMS RELAY SERVICE RUNNING").setContentText(input)
            .setSmallIcon(R.drawable.ic_launcher_background).setContentIntent(pendingIntent).build()
        startForeground(1,notification)
        return START_NOT_STICKY


    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }
    override fun messageRecieved(message: Sms) {
        //sendMessageToServer()
       // Toast.makeText(this,message.messageBody,Toast.LENGTH_LONG).show()
        Log.d("bugg","message: "+ message.messageBody);
        val intent = Intent();
        val bundle = Bundle();

        bundle.putString("sms",message.toJson().toString())
        intent.putExtras(bundle)
        intent.setAction("update")
        sendBroadcast(intent)
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(
                NotificationManager::class.java
            )
            manager?.createNotificationChannel(serviceChannel)
        }
    }
}