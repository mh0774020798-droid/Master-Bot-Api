package com.aiapp.generated

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Vibrator
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.CompoundButton
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var prefs: SharedPreferences
    private var statusReceiver: BroadcastReceiver? = null
    private lateinit var mainButton: FrameLayout
    private lateinit var mainButtonText: TextView
    private lateinit var mainButtonSub: TextView
    private lateinit var batteryText: TextView
    private lateinit var shakeSwitch: Switch
    private lateinit var interceptSwitch: Switch
    private lateinit var bgServiceSwitch: Switch
    private lateinit var sosSwitch: Switch
    private lateinit var sosSpeedBar: SeekBar
    private lateinit var sensitivityLow: TextView
    private lateinit var sensitivityMed: TextView
    private lateinit var sensitivityHigh: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) 
        
        prefs = getSharedPreferences("FlashlightPrefs", Context.MODE_PRIVATE)

        // Handle Camera Button Interception immediately if triggered from shortcut
        val action = intent?.action
        if (action == Intent.ACTION_CAMERA_BUTTON || action == "android.media.action.STILL_IMAGE_CAMERA") {
            if (prefs.getBoolean("intercept_camera", true)) {
                val serviceIntent = Intent(this, FlashlightService::class.java).apply {
                    this.action = FlashlightService.ACTION_TOGGLE
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
                Toast.makeText(this, "פנס הופעל באמצעות כפתור פיזי", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
        }

        // Request permissions
        val permissions = mutableListOf(android.Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        val missing = permissions.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) {
            requestPermissions(missing.toTypedArray(), 101)
        }

        // Build UI
        val root = ScrollView(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(0xFFF8FAFC.toInt())
            isFillViewport = true
        }

        val container = LinearLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(20), dpToPx(24), dpToPx(20), dpToPx(32))
        }

        // Header
        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, 0, 0, dpToPx(24))
        }
        val titleText = TextView(this).apply {
            text = "פנס הקסם Pro"
            textSize = 28f
            setTextColor(0xFF0F172A.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        val subtitleText = TextView(this).apply {
            text = "שליטה חכמה, ניעור ומקשי קיצור"
            textSize = 14f
            setTextColor(0xFF64748B.toInt())
            gravity = Gravity.CENTER
            setPadding(0, dpToPx(4), 0, 0)
        }
        headerLayout.addView(titleText)
        headerLayout.addView(subtitleText)
        container.addView(headerLayout)

        // Main Power Button Card
        val mainCard = createCard()
        mainButton = FrameLayout(this).apply {
            val size = dpToPx(160)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dpToPx(16)
                bottomMargin = dpToPx(16)
            }
            background = createCircularButtonDrawable(false)
            isClickable = true
            isFocusable = true
            setOnClickListener {
                toggleFlashlight()
            }
        }
        val buttonContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }
        mainButtonText = TextView(this).apply {
            text = "כבוי"
            textSize = 22f
            setTextColor(0xFFFFFFFF.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        mainButtonSub = TextView(this).apply {
            text = "לחץ להפעלה"
            textSize = 12f
            setTextColor(0xCCFFFFFF.toInt())
            gravity = Gravity.CENTER
        }
        buttonContent.addView(mainButtonText)
        buttonContent.addView(mainButtonSub)
        mainButton.addView(buttonContent)
        mainCard.addView(mainButton)

        batteryText = TextView(this).apply {
            text = "סוללה: --%"
            textSize = 13f
            setTextColor(0xFF64748B.toInt())
            gravity = Gravity.CENTER
            setPadding(0, dpToPx(8), 0, 0)
        }
        mainCard.addView(batteryText)
        container.addView(mainCard)

        // Shake Settings Card
        val shakeCard = createCard()
        val shakeHeader = createSectionHeader("הפעלה בניעור (גם כשהמסך כבוי)")
        shakeCard.addView(shakeHeader)

        shakeSwitch = Switch(this).apply {
            text = "אפשר הפעלה בניעור"
            textSize = 16f
            setTextColor(0xFF1E293B.toInt())
            isChecked = prefs.getBoolean("shake_enabled", true)
            setPadding(0, dpToPx(12), 0, dpToPx(12))
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean("shake_enabled", isChecked).apply()
                updateServiceSettings()
            }
        }
        shakeCard.addView(shakeSwitch)

        val sensitivityLabel = TextView(this).apply {
            text = "רגישות ניעור:"
            textSize = 14f
            setTextColor(0xFF64748B.toInt())
            setPadding(0, dpToPx(8), 0, dpToPx(8))
        }
        shakeCard.addView(sensitivityLabel)

        val sensitivityLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(40))
            weightSum = 3f
        }
        sensitivityLow = createSensitivityButton("נמוכה", 0, 3)
        sensitivityMed = createSensitivityButton("בינונית", 1, 3)
        sensitivityHigh = createSensitivityButton("גבוהה", 2, 3)
        
        sensitivityLayout.addView(sensitivityLow)
        sensitivityLayout.addView(sensitivityMed)
        sensitivityLayout.addView(sensitivityHigh)
        shakeCard.addView(sensitivityLayout)
        container.addView(shakeCard)

        // SOS Mode Card
        val sosCard = createCard()
        sosCard.addView(createSectionHeader("מצב SOS / הבהוב"))

        sosSwitch = Switch(this).apply {
            text = "הפעל מצב הבהוב"
            textSize = 16f
            setTextColor(0xFF1E293B.toInt())
            isChecked = prefs.getBoolean("sos_enabled", false)
            setPadding(0, dpToPx(12), 0, dpToPx(12))
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean("sos_enabled", isChecked).apply()
                val serviceIntent = Intent(this@MainActivity, FlashlightService::class.java).apply {
                    action = if (isChecked) FlashlightService.ACTION_START_SOS else FlashlightService.ACTION_STOP_SOS
                }
                startFlashlightService(serviceIntent)
            }
        }
        sosCard.addView(sosSwitch)

        val speedLabel = TextView(this).apply {
            text = "מהירות הבהוב:"
            textSize = 14f
            setTextColor(0xFF64748B.toInt())
            setPadding(0, dpToPx(8), 0, dpToPx(4))
        }
        sosCard.addView(speedLabel)

        sosSpeedBar = SeekBar(this).apply {
            max = 900
            progress = 1000 - prefs.getInt("sos_interval", 500)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val interval = 1000 - progress
                    prefs.edit().putInt("sos_interval", interval).apply()
                    if (sosSwitch.isChecked) {
                        updateServiceSettings()
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        sosCard.addView(sosSpeedBar)
        container.addView(sosCard)

        // Physical Button Card
        val physicalCard = createCard()
        physicalCard.addView(createSectionHeader("הגדרות כפתור פיזי"))

        interceptSwitch = Switch(this).apply {
            text = "השתלט על כפתור המצלמה"
            textSize = 16f
            setTextColor(0xFF1E293B.toInt())
            isChecked = prefs.getBoolean("intercept_camera", true)
            setPadding(0, dpToPx(12), 0, dpToPx(12))
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean("intercept_camera", isChecked).apply()
            }
        }
        physicalCard.addView(interceptSwitch)

        val interceptDesc = TextView(this).apply {
            text = "לחיצה כפולה על כפתור ההפעלה או לחיצה על כפתור המצלמה הפיזי תפעיל ישירות את הפנס במקום את המצלמה."
            textSize = 12f
            setTextColor(0xFF64748B.toInt())
            setPadding(0, 0, 0, dpToPx(8))
        }
        physicalCard.addView(interceptDesc)
        container.addView(physicalCard)

        // Extra Utilities Card
        val utilsCard = createCard()
        utilsCard.addView(createSectionHeader("כלים נוספים"))

        bgServiceSwitch = Switch(this).apply {
            text = "שירות פועל ברקע תמידי"
            textSize = 16f
            setTextColor(0xFF1E293B.toInt())
            isChecked = prefs.getBoolean("bg_service_enabled", true)
            setPadding(0, dpToPx(12), 0, dpToPx(12))
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean("bg_service_enabled", isChecked).apply()
                if (isChecked) {
                    startFlashlightService(Intent(this@MainActivity, FlashlightService::class.java).apply {
                        action = FlashlightService.ACTION_START_SERVICE
                    })
                } else {
                    stopService(Intent(this@MainActivity, FlashlightService::class.java))
                }
            }
        }
        utilsCard.addView(bgServiceSwitch)

        val screenLightBtn = Button(this).apply {
            text = "מסך מאיר (צבע לבן מלא)"
            setTextColor(0xFF1E293B.toInt())
            textSize = 15f
            background = createButtonDrawable(0xFFE2E8F0.toInt(), 0xFFCBD5E1.toInt())
            setOnClickListener {
                openScreenLightDialog()
            }
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(48)).apply {
                topMargin = dpToPx(8)
            }
            layoutParams = params
        }
        utilsCard.addView(screenLightBtn)
        container.addView(utilsCard)

        // Footer Credits
        val footerCard = createCard().apply {
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dpToPx(16)
                bottomMargin = dpToPx(16)
            }
            layoutParams = params
        }
        val footerText = TextView(this).apply {
            text = "נבנה ע\"י פלטפורמת מאסטר בוט, ע\"י רביב דיגיטל\nלפרטים נוספים: 0556798858b@gmail.com"
            textSize = 13f
            setTextColor(0xFF64748B.toInt())
            gravity = Gravity.CENTER
            setLineSpacing(4f, 1.1f)
            isClickable = true
            setOnClickListener {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:0556798858b@gmail.com")
                    putExtra(Intent.EXTRA_SUBJECT, "פנס הקסם Pro")
                }
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "לא נמצאה אפליקציית מייל", Toast.LENGTH_SHORT).show()
                }
            }
        }
        footerCard.addView(footerText)
        container.addView(footerCard)

        root.addView(container)
        setContentView(root)

        updateSensitivityUI(prefs.getInt("shake_sensitivity", 1))
        startBackgroundServiceIfNeeded()
    }

    override fun onStart() {
        super.onStart()
        registerStatusReceiver()
        registerBatteryReceiver()
        // Refresh UI state
        val isRunning = FlashlightService.isTorchOn
        updateUIState(isRunning)
    }

    override fun onStop() {
        super.onStop()
        statusReceiver?.let { unregisterReceiver(it) }
        statusReceiver = null
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics).toInt()
    }

    private fun createCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dpToPx(16)
            }
            layoutParams = params
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dpToPx(16).toFloat()
                setStroke(dpToPx(1), 0xFFE2E8F0.toInt())
            }
            elevation = dpToPx(2).toFloat()
        }
    }

    private fun createSectionHeader(title: String): TextView {
        return TextView(this).apply {
            text = title
            textSize = 16f
            setTextColor(0xFF0F172A.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dpToPx(8))
        }
    }

    private fun createSensitivityButton(title: String, index: Int, total: Int): TextView {
        return TextView(this).apply {
            text = title
            gravity = Gravity.CENTER
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            isClickable = true
            setOnClickListener {
                prefs.edit().putInt("shake_sensitivity", index).apply()
                updateSensitivityUI(index)
                updateServiceSettings()
            }
        }
    }

    private fun updateSensitivityUI(selectedIndex: Int) {
        val activeBg = GradientDrawable().apply {
            setColor(0xFFF59E0B.toInt())
            cornerRadius = dpToPx(8).toFloat()
        }
        val inactiveBg = GradientDrawable().apply {
            setColor(0xFFF1F5F9.toInt())
            cornerRadius = dpToPx(8).toFloat()
        }

        val buttons = listOf(sensitivityLow, sensitivityMed, sensitivityHigh)
        for (i in buttons.indices) {
            if (i == selectedIndex) {
                buttons[i].background = activeBg
                buttons[i].setTextColor(Color.WHITE)
                buttons[i].typeface = android.graphics.Typeface.DEFAULT_BOLD
            } else {
                buttons[i].background = inactiveBg
                buttons[i].setTextColor(0xFF64748B.toInt())
                buttons[i].typeface = android.graphics.Typeface.DEFAULT
            }
        }
    }

    private fun createCircularButtonDrawable(active: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(if (active) 0xFFF59E0B.toInt() else 0xFF1E293B.toInt())
            setStroke(dpToPx(6), if (active) 0xFFFEF3C7.toInt() else 0xFF475569.toInt())
        }
    }

    private fun createButtonDrawable(normalColor: Int, pressedColor: Int): StateListDrawable {
        val normal = GradientDrawable().apply {
            setColor(normalColor)
            cornerRadius = dpToPx(12).toFloat()
        }
        val pressed = GradientDrawable().apply {
            setColor(pressedColor)
            cornerRadius = dpToPx(12).toFloat()
        }
        return StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), pressed)
            addState(intArrayOf(), normal)
        }
    }

    private fun toggleFlashlight() {
        val serviceIntent = Intent(this, FlashlightService::class.java).apply {
            action = FlashlightService.ACTION_TOGGLE
        }
        startFlashlightService(serviceIntent)
    }

    private fun startFlashlightService(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun startBackgroundServiceIfNeeded() {
        if (prefs.getBoolean("bg_service_enabled", true)) {
            val serviceIntent = Intent(this, FlashlightService::class.java).apply {
                action = FlashlightService.ACTION_START_SERVICE
            }
            startFlashlightService(serviceIntent)
        }
    }

    private fun updateServiceSettings() {
        val serviceIntent = Intent(this, FlashlightService::class.java).apply {
            action = FlashlightService.ACTION_UPDATE_SETTINGS
        }
        startService(serviceIntent)
    }

    private fun updateUIState(isOn: Boolean) {
        mainButton.background = createCircularButtonDrawable(isOn)
        mainButtonText.text = if (isOn) "פעיל" else "כבוי"
        mainButtonSub.text = if (isOn) "לחץ לכיבוי" else "לחץ להפעלה"
        sosSwitch.isChecked = FlashlightService.isSosOn
    }

    private fun registerStatusReceiver() {
        statusReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == FlashlightService.ACTION_STATUS_CHANGED) {
                    val isOn = intent.getBooleanExtra("status", false)
                    updateUIState(isOn)
                }
            }
        }
        registerReceiver(statusReceiver, IntentFilter(FlashlightService.ACTION_STATUS_CHANGED))
    }

    private fun registerBatteryReceiver() {
        val batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                if (level != -1 && scale != -1) {
                    val pct = (level * 100 / scale.toFloat()).toInt()
                    batteryText.text = "סוללה: $pct%"
                }
            }
        }
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    private fun openScreenLightDialog() {
        val dialog = android.app.Dialog(this, android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen)
        val layout = FrameLayout(this).apply {
            setBackgroundColor(Color.WHITE)
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        val closeText = TextView(this).apply {
            text = "לחץ פה כדי לסגור"
            textSize = 18f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER
            }
        }
        layout.addView(closeText)
        dialog.setContentView(layout)

        // Force Max Brightness
        val window = dialog.window
        if (window != null) {
            val lp = window.attributes
            lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
            window.attributes = lp
        }

        layout.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }
}

class FlashlightService : Service(), SensorEventListener {

    private lateinit var cameraManager: CameraManager
    private var cameraId: String? = null
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var prefs: SharedPreferences
    private var lastShakeTime: Long = 0
    private val handler = Handler(Looper.getMainLooper())
    private var strobeRunnable: Runnable? = null

    companion object {
        const val ACTION_TOGGLE = "com.aiapp.generated.TOGGLE"
        const val ACTION_START_SERVICE = "com.aiapp.generated.START_SERVICE"
        const val ACTION_UPDATE_SETTINGS = "com.aiapp.generated.UPDATE_SETTINGS"
        const val ACTION_START_SOS = "com.aiapp.generated.START_SOS"
        const val ACTION_STOP_SOS = "com.aiapp.generated.STOP_SOS"
        const val ACTION_STATUS_CHANGED = "com.aiapp.generated.STATUS_CHANGED"
        const val CHANNEL_ID = "flashlight_service_channel"

        var isTorchOn = false
        var isSosOn = false
    }

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("FlashlightPrefs", Context.MODE_PRIVATE)
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val list = cameraManager.cameraIdList
            if (list.isNotEmpty()) {
                cameraId = list[0]
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        
        registerShakeListener()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, createNotification())

        when (intent?.action) {
            ACTION_TOGGLE -> {
                toggleTorch()
            }
            ACTION_START_SOS -> {
                startStrobe()
            }
            ACTION_STOP_SOS -> {
                stopStrobe()
            }
            ACTION_UPDATE_SETTINGS -> {
                registerShakeListener()
                if (isSosOn) {
                    stopStrobe()
                    startStrobe()
                }
            }
        }
        return START_STICKY
    } 

    private fun toggleTorch() {
        if (isSosOn) {
            stopStrobe()
        }
        setTorchState(!isTorchOn)
    }

    private fun setTorchState(on: Boolean) {
        val id = cameraId ?: return
        try {
            cameraManager.setTorchMode(id, on)
            isTorchOn = on
            notifyStatusChanged()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startStrobe() {
        isSosOn = true
        notifyStatusChanged()
        strobeRunnable = object : Runnable {
            override fun run() {
                if (!isSosOn) return
                setTorchState(!isTorchOn)
                val interval = prefs.getInt("sos_interval", 500).toLong()
                handler.postDelayed(this, interval)
            }
        }
        handler.post(strobeRunnable!!)
    }

    private fun stopStrobe() {
        isSosOn = false
        strobeRunnable?.let { handler.removeCallbacks(it) }
        strobeRunnable = null
        setTorchState(false)
    }

    private fun registerShakeListener() {
        sensorManager.unregisterListener(this)
        if (prefs.getBoolean("shake_enabled", true)) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val gX = x / SensorManager.GRAVITY_EARTH
            val gY = y / SensorManager.GRAVITY_EARTH
            val gZ = z / SensorManager.GRAVITY_EARTH
            val gForce = Math.sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()

            // Sensitivity levels: Low (15), Medium (12), High (9)
            val sensitivitySetting = prefs.getInt("shake_sensitivity", 1)
            val threshold = when (sensitivitySetting) {
                0 -> 2.2f // Low sensitivity (requires harder shake)
                2 -> 1.4f // High sensitivity
                else -> 1.8f // Medium
            }

            if (gForce > threshold) {
                val now = System.currentTimeMillis()
                if (now - lastShakeTime > 1200) { // Debounce
                    lastShakeTime = now
                    triggerVibration()
                    toggleTorch()
                }
            }
        }
    }

    private fun triggerVibration() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(android.os.VibrationEffect.createOneShot(150, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(150)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun notifyStatusChanged() {
        val intent = Intent(ACTION_STATUS_CHANGED).apply {
            putExtra("status", isTorchOn)
        }
        sendBroadcast(intent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "שירות פנס רקע",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "מאפשר זיהוי ניעור להפעלת הפנס כשהמסך כבוי"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val toggleIntent = Intent(this, FlashlightService::class.java).apply {
            action = ACTION_TOGGLE
        }
        val togglePendingIntent = PendingIntent.getService(
            this, 1, toggleIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            Notification.Builder(this)
        }

        return builder
            .setContentTitle("פנס הקסם פועל ברקע")
            .setContentText("ניעור המכשיר יפעיל/יכבה את הפנס")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_compass, "הפעל/כבוי", togglePendingIntent)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        stopStrobe()
        setTorchState(false)
        super.onDestroy()
    }
}