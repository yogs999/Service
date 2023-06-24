package com.example.servicedemo

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.graphics.drawable.toBitmap
import com.example.servicedemo.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Timer
import java.util.TimerTask


@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    private lateinit var wifi: WifiManager
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var db:DatabaseReference
    private lateinit var firebaseStore:FirebaseFirestore
    private lateinit var storageRef:StorageReference
    private var counting=0
    private lateinit var sharedPreferences: SharedPreferences

    @SuppressLint("SuspiciousIndentation")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        counting = sharedPreferences.getInt("count", 0)

           binding.sendData.setOnClickListener {
               sendDataFirebase()
               sendImageFirebase()
               counting++
               val countText = counting.toString()
               binding.counttxt.text=countText
               binding.counttxt.text=counting.toString()
           }



       var batteryManager = getSystemService(BATTERY_SERVICE) as BatteryManager

        registerReceiver(this.BattryBroadcast, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        // screenshot automatic
        val timer2 = Timer()
        timer2.schedule(object : TimerTask() {
            override fun run() {
                val view=findViewById<View>(R.id.take)
                val bitmap:Bitmap=screenShot(view)
                runOnUiThread {
                    binding.imageCapture.setImageBitmap(bitmap)
                }
            }
        }, 1000)

// for timestamp
        Timer().schedule(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    val dateNow = LocalDateTime.now()
                    val dateformate = DateTimeFormatter.ofPattern("dd-mm-yyyy hh:mm:ss")
                    val formatedatetime = dateNow.format(dateformate)
                    binding.Date.text = formatedatetime
                }
            }
        }, 0, 1000)


        wifi = getSystemService(Context.WIFI_SERVICE) as WifiManager

        binding.InternetStatustxt.setOnCheckedChangeListener { buttonView, isChecked ->

            if (isChecked) {
                val intent = Intent(Settings.Panel.ACTION_WIFI)
                startActivityForResult(intent, 100)
            } else {
                wifi.isWifiEnabled = false
            }
        binding.InternetStatustxt.isChecked = wifi.isWifiEnabled
    }
        binding.BatteryPercantagetxt.setOnCheckedChangeListener { buttonView, isChecked ->

        }
    }

    private fun sendImageFirebase() {
       storageRef=FirebaseStorage.getInstance().reference.child("image")
        firebaseStore=FirebaseFirestore.getInstance()
        uploadImage()
    }

    //for image send on firebase
    private fun uploadImage() {

        storageRef = storageRef.child(System.currentTimeMillis().toString())

        val bitmap = binding.imageCapture.drawable?.toBitmap()
        if (bitmap != null) {
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val imageData = baos.toByteArray()

            val filename = "${System.currentTimeMillis()}.jpg"
            val imageRef = storageRef.child(filename)

            val uploadTask = imageRef.putBytes(imageData)
            uploadTask.addOnSuccessListener { taskSnapshot ->
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    val image = uri.toString()
                    Toast.makeText(this, "successfully", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener { exception ->
                    Log.e("MainActivity", "Failed", exception)
                }
            }
        }
    }
    //data send in firebase
    @SuppressLint("SuspiciousIndentation")
    private fun sendDataFirebase(){
        val time=binding.Date.text.toString()
        val count=binding.counttxt.text.toString()
        val connectvity=binding.InternetStatustxt.text.toString()
        val batteryStatus=binding.BatteryPercantagetxt .text.toString()
        val battry=binding.batteryStatustxt.text.toString()
        val location=binding.LocationStatus.text.toString()

        db=FirebaseDatabase.getInstance().getReference("user")

    val item=Item(time,count,connectvity,batteryStatus,battry,location)

        db.setValue(item).addOnSuccessListener {
            binding.Date.text.isBlank()
            binding.counttxt.text.isBlank()
            binding.InternetStatustxt.text.isBlank()
            binding.BatteryPercantagetxt.text.isBlank()
            binding.batteryStatustxt.text.isBlank()
            binding.LocationStatus.text.isBlank()

            Toast.makeText(this, "successful", Toast.LENGTH_SHORT).show()
        }
    }
      // find location
    private fun getLocationUser() {
        val task = fusedLocationProviderClient.lastLocation

        if (ActivityCompat
                .checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED && ActivityCompat
                .checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                101
            )
            return
        }

        task.addOnSuccessListener {
            if (it != null){
               binding.LocationStatus.text=it.longitude.toString()
            }
        }
    }


    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
           binding.InternetStatustxt.isChecked= true
        }
    }

     // taking screenshot and return bitmap
    fun screenShot(view:View):Bitmap {

        val bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
         val canvas=Canvas(bitmap)
          val bitmapbackground=view.background
          if(bitmapbackground!=null)bitmapbackground.draw(canvas)
           else canvas.drawColor(Color.WHITE)
           view.draw(canvas)

         return bitmap

    }

    //battery percantage show
    private val BattryBroadcast:BroadcastReceiver=object :BroadcastReceiver() {

        @SuppressLint("SetTextI18n")
        override fun onReceive(context: Context?, intent: Intent?) {

            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                val bm = context!!.getSystemService(BATTERY_SERVICE) as BatteryManager
                val batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                binding.batteryStatustxt.text = "$batLevel %"
            }
        }

    }
}
