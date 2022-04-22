package com.example.sensorsapp

import android.Manifest
import android.app.Notification
import android.app.Notification.DEFAULT_SOUND
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.RemoteViews
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity(), LocationListener, SensorEventListener {

    private lateinit var brightnessText: TextView
    private lateinit var rotationText: TextView
    private lateinit var humidityText: TextView
    private lateinit var locationText: TextView

    private lateinit var locationManager: LocationManager
    private val locationPermissionCode = 2
    private lateinit var sensorsManager: SensorManager
    private var brightnessSensor: Sensor? = null
    private var humiditySensor: Sensor? = null
    private var rotationSensor: Sensor? = null
    private var temperatureSensor: Sensor? = null
    private var temperatureVal: Float = 0.1F

    private lateinit var backActivity: View

    private val CHANNEL_ID = "channel_id_example_01"
    private val notificationId = 101


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        brightnessText = findViewById(R.id.brightness_text)
        rotationText = findViewById(R.id.rotation_text)
        humidityText = findViewById(R.id.humidity_text)
        locationText = findViewById(R.id.location_text)

        backActivity = findViewById(R.id.lay)

        sensorsManager = getSystemService(SENSOR_SERVICE) as SensorManager

        brightnessSensor = sensorsManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        humiditySensor = sensorsManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY)
        rotationSensor = sensorsManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        temperatureSensor = sensorsManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)

        getLocation()
        createNotificationChannel()

        val mainHandler = Handler(Looper.getMainLooper())

        mainHandler.post(object : Runnable {
            override fun run() {
                mainHandler.postDelayed(this, 60000)
                sendNotification()
            }
        })
    }


    private fun getLocation() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if ((ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED)
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                locationPermissionCode
            )
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5f, this)
    }

    override fun onLocationChanged(location: Location) {
        locationText.text =
            "Location: \n%.5f".format(location.latitude) + ", %.5f".format(location.longitude)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_LIGHT) {
            val light = event.values[0]
            brightnessText.text = "Brightness: " + "%.2f".format(light)

            if (light.toInt() > 20000) {
                //AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                //delegate.applyDayNight()
                backActivity.setBackgroundColor(Color.parseColor("#FFFFFF"))
            } else {
                //AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                //delegate.applyDayNight()
                backActivity.setBackgroundColor(Color.parseColor("#787878"))

            }
        }
        if (event?.sensor?.type == Sensor.TYPE_RELATIVE_HUMIDITY) {
            humidityText.text = "Humidity: \n%.2f".format(event.values[0]) + "%"
        }
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            rotationText.text =
                "Rotation X: " + event.values[0] + "\nRotation Y: " + event.values[1] + "\nRotation Z: " + event.values[2]
        }
        if (event?.sensor?.type == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            temperatureVal = event.values[0]
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        return
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Name", NotificationManager.IMPORTANCE_HIGH).apply {
                setShowBadge(true)
                description = "Text"
            }
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Temperature")
            .setFullScreenIntent(pendingIntent, true)

            .setContentText(temperatureVal.toString())
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            notify(notificationId, builder.build())
        }
    }


    override fun onResume() {
        super.onResume()
        sensorsManager.registerListener(
            this,
            this.brightnessSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        sensorsManager.registerListener(
            this,
            this.humiditySensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        sensorsManager.registerListener(
            this,
            this.rotationSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        sensorsManager.registerListener(
            this,
            this.temperatureSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )

    }

    override fun onDestroy() {
        super.onDestroy()
        sensorsManager.unregisterListener(this)
    }
}