package com.example.cradle_vsa_sms_relay.broad_castrecivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.cradle_vsa_sms_relay.MessageListener
import com.example.cradle_vsa_sms_relay.Sms
import org.json.JSONObject

/**
 * this broadcast receiver sends message to activity from service whenever service receives a sms
 */
open class ServiceToActivityBroadCastReciever(var mListener: MessageListener? = null) : BroadcastReceiver() {
    override fun onReceive(p0: Context?, p1: Intent?) {
        if (p1 != null) {
            if (p1.action.equals("update")){
                Log.d("bugg","received from service")
                val intent = p1.extras
                val message: String? = intent?.getString("sms");
                //its in json format
                val sms = Sms.fromJson(message.toString())
                mListener?.messageRecieved(sms)
            }
        }
    }
}