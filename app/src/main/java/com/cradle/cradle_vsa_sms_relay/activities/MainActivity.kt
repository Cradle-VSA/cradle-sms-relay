package com.cradle.cradle_vsa_sms_relay.activities

import android.Manifest
import android.app.ActivityOptions
import android.app.role.RoleManager
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Telephony
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.WorkInfo
import com.cradle.cradle_vsa_sms_relay.*
import com.cradle.cradle_vsa_sms_relay.dagger.MyApp
import com.cradle.cradle_vsa_sms_relay.database.ReferralDatabase
import com.cradle.cradle_vsa_sms_relay.database.SmsReferralEntitiy
import com.cradle.cradle_vsa_sms_relay.service.SmsService
import com.cradle.cradle_vsa_sms_relay.views.ReferralAlertDialog
import com.google.android.material.button.MaterialButton
import javax.inject.Inject


class MainActivity : AppCompatActivity(),
    SingleMessageListener {

    private var isServiceStarted = false
    var mIsBound: Boolean = false
    //our reference to the service
    var mService: SmsService? = null
    @Inject
    lateinit var database: ReferralDatabase
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(p0: ComponentName?) {
            mIsBound = false
            mService = null
            isServiceStarted = false
        }

        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            val binder = p1 as SmsService.MyBinder
            mIsBound=true
            isServiceStarted= true
            mService = binder.service
            mService?.singleMessageListener = this@MainActivity

            mService?.reuploadReferralListener = object : ReuploadReferralListener {
                override fun onReuploadReferral(workInfo: WorkInfo) {
                    if (workInfo.state == WorkInfo.State.RUNNING) {
                        //update recylcer view
                        setuprecyclerview()
                    }
                }
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        (application as MyApp).component.inject(this)
        // bind service in case its running
        if (SmsService.isServiceRunningInForeground(this,
                SmsService.javaClass)){
            val serviceIntent = Intent(
                this,
                SmsService::class.java
            )
            bindService(serviceIntent, serviceConnection, 0)
        }
        SetAsDefaultHandler()
        setupToolBar()
        setupStartService()
        setupStopService()
        setuprecyclerview()

    }

    private fun SetAsDefaultHandler(){

        val intent:Intent
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Telephony.Sms.getDefaultSmsPackage(this) != packageName) {
                intent = getSystemService(RoleManager::class.java).createRequestRoleIntent(RoleManager.ROLE_SMS)
                startActivityForResult(intent, 99)
            }
        } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (Telephony.Sms.getDefaultSmsPackage(this) != packageName) {
                intent =Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
                intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
                startActivity(intent)
            }
        }
    }

    private fun setupToolBar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title="";
        val settingButton:ImageButton = findViewById(R.id.settingIcon)
        settingButton.setOnClickListener {
            startActivity(Intent(this,SettingsActivity::class.java),
                ActivityOptions.makeCustomAnimation(this,R.anim.slide_down,R.anim.nothing).toBundle())
        }
    }

    private fun setuprecyclerview() {
        val emptyImageView:ImageView = findViewById(R.id.emptyRecyclerView)
        val smsRecyclerView: RecyclerView = findViewById(R.id.messageRecyclerview)
        val referrals =
            database.daoAccess().getAllReferrals().sortedByDescending { it.timeRecieved }

        if (referrals.isNotEmpty()){
            emptyImageView.visibility =GONE
        } else {
            emptyImageView.visibility = VISIBLE
        }
        val adapter = SmsRecyclerViewAdaper(referrals,this)
        adapter.onCLickList.add(object : AdapterClicker {
            override fun onClick(referralEntitiy: SmsReferralEntitiy) {
                val referralAlertDialog =ReferralAlertDialog(this@MainActivity,referralEntitiy)

                referralAlertDialog.setOnSendToServerListener(View.OnClickListener {
                    if (isServiceStarted && mIsBound){
                        if (!referralEntitiy.isUploaded) {
                            mService?.sendToServer(referralEntitiy)
                            Toast.makeText(
                                this@MainActivity, "Uploading the referral to the server",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else{
                            Toast.makeText(
                                this@MainActivity, "Referral is already uploaded to the server ",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        referralAlertDialog.cancel()
                    } else {
                        Toast.makeText(this@MainActivity,"Unable to send to the server, " +
                                "Make sure service is running.",Toast.LENGTH_SHORT).show()
                    }
                })
                referralAlertDialog.show()
            }
        })
        smsRecyclerView.adapter = adapter
        val layout: RecyclerView.LayoutManager = LinearLayoutManager(this)
        smsRecyclerView.layoutManager = layout
        adapter.notifyDataSetChanged()

    }


    private fun setupStopService() {
        findViewById<MaterialButton>(R.id.btnStopService).setOnClickListener {
            if (mService != null && isServiceStarted) {
                val intent: Intent = Intent(this, SmsService::class.java).also { intent ->
                    unbindService(serviceConnection)
                }
                intent.action = SmsService.STOP_SERVICE
                ContextCompat.startForegroundService(this, intent)
                isServiceStarted = false
                mIsBound = false
            }
        }
    }

    private fun setupStartService() {
        findViewById<MaterialButton>(R.id.btnStartService).setOnClickListener {
            checkpermissions()
        }
    }


    private fun checkpermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        Manifest.permission.FOREGROUND_SERVICE,
                        Manifest.permission.INTERNET,
                        Manifest.permission.READ_SMS,
                        Manifest.permission.RECEIVE_SMS,
                        Manifest.permission.RECEIVE_MMS,
                        Manifest.permission.SEND_SMS
                    ), 99
                )
            } else {
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        Manifest.permission.INTERNET,
                        Manifest.permission.READ_SMS,
                        Manifest.permission.RECEIVE_SMS,
                        Manifest.permission.RECEIVE_MMS
                    ), 99
                )
            }
        } else {
            //permission already granted
            if (!isServiceStarted) {
                startService()
            }

        }
    }

    fun startService() {
        val serviceIntent = Intent(
            this,
            SmsService::class.java
        ).also { intent -> bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE) }
        serviceIntent.action = SmsService.START_SERVICE
        ContextCompat.startForegroundService(this, serviceIntent)
        isServiceStarted = true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 99) {
            //do whatever when permissions are granted
            if (!isServiceStarted) {
                startService()
            }
        }
    }

    override fun newMessageReceived() {
        runOnUiThread {
            setuprecyclerview()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if(mIsBound || SmsService.isServiceRunningInForeground(this,
                SmsService::class.java)) {
            unbindService(serviceConnection)
        }
    }

    interface AdapterClicker {
        fun onClick(referralEntitiy: SmsReferralEntitiy)
    }
}
