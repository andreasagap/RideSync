package gr.andreasagap.moto.communication.threads

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.widget.Chronometer
import android.widget.RemoteViews
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import gr.andreasagap.moto.communication.*
import gr.andreasagap.moto.communication.Constants.CHANNEL_ID
import gr.andreasagap.moto.communication.Constants.NOTIFICATION_ID
import gr.andreasagap.moto.communication.broadcasters.HungUpBroadcaster
import gr.andreasagap.moto.communication.extensions.noNull
import gr.andreasagap.moto.communication.presentation.MainActivity
import gr.andreasagap.moto.communication.presentation.PORT_USED
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.Locale
import kotlin.concurrent.thread


class CallService : Service(), AudioAction {

    private var isServer: Boolean = false
    private var address: InetAddress? = null
    private lateinit var audioStreamingManager: AudioStreamingManager
    private var socket: DatagramSocket = DatagramSocket()
    private lateinit var hostAddress: String
    private lateinit var thread: Thread
    private lateinit var speaker: AudioTrack
    private var myBuffer = ByteArray(BUFFER_SIZE_RECORDING)
    private lateinit var notification:
            NotificationCompat.Builder
    private lateinit var chronometer:Chronometer
    private var notificationShown = false
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        chronometer = Chronometer(this)
        initSpeaker()
        initRecorder()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val extras = intent?.extras
        extras?.let {
            val addressBytes = intent.getByteArrayExtra(Bundle_Address)
            if (addressBytes != null)
                address = InetAddress.getByAddress(addressBytes)
            isServer = it.getBoolean(Bundle_isServer)
        }
        initSocket()
        showNotification()
        thread = thread(start = true) {
            try {
                while (true) {
                    receiveAudio(receive() ?: ByteArray(0))
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return START_STICKY
    }

    private fun showNotification() {
        val customView = RemoteViews(packageName, R.layout.call_notification)
        val notificationIntent = Intent(this, MainActivity::class.java)
        val hungUpIntent = Intent(this, HungUpBroadcaster::class.java)
        val pendingIntent =
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        val hungUpPendingIntent =
            PendingIntent.getBroadcast(this, 0, hungUpIntent, PendingIntent.FLAG_IMMUTABLE)
        customView.setOnClickPendingIntent(R.id.btnDecline, hungUpPendingIntent)
        notificationShown = true
        customView.setTextViewText(R.id.nameTextView, getString(R.string.notification_communication_txt))
        customView.setTextViewText(R.id.timeTextVIew, "00:00")

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationChannel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_ID, NotificationManager.IMPORTANCE_HIGH
        )
        notificationChannel.setSound(null, null)
        notificationManager.createNotificationChannel(notificationChannel)
        notification = NotificationCompat.Builder(this, CHANNEL_ID)
        notification.setContentTitle(getString(R.string.app_name))
        notification.setSmallIcon(R.mipmap.ic_launcher_foreground)
        notification.setCategory(NotificationCompat.CATEGORY_CALL)
        notification.setVibrate(null)
        notification.setOngoing(true)
        notification.setFullScreenIntent(pendingIntent, true)
        notification.setStyle(NotificationCompat.DecoratedCustomViewStyle())
        notification.setCustomContentView(customView)
        notification.setCustomBigContentView(customView)

        startChronometer(customView)
        startForeground(NOTIFICATION_ID, notification.build())
    }
    val handler = Handler()

    private fun startChronometer(customView: RemoteViews) {
        chronometer.base =
            SystemClock.elapsedRealtime() // Set the starting point to the current system time
        chronometer.start()
        val runnable: Runnable = object : Runnable {
            override fun run() {
                val elapsedMillis = SystemClock.elapsedRealtime() - chronometer.base
                val seconds = (elapsedMillis / 1000).toInt() % 60
                val minutes = (elapsedMillis / (1000 * 60) % 60).toInt()
                val hours = (elapsedMillis / (1000 * 60 * 60) % 60).toInt()
                val time =
                    String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
                customView.setTextViewText(R.id.timeTextVIew, time)
                val notificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, notification.build())
                handler.postDelayed(this, 1000) // Update every second
            }
        }

        handler.postDelayed(runnable, 1000) // Start the periodic update


    }

    override fun onDestroy() {
        super.onDestroy()
        if(!MotorApplication().isActivityVisible()){
            closePeerConnection()
        }
        audioStreamingManager.stop()
        speaker.stop()
        socket.disconnect()
        socket.close()
        thread.interrupt()
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        chronometer.stop()
        handler.removeCallbacksAndMessages(null)
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun closePeerConnection() {
        val manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        val channel = manager.initialize(this, Looper.getMainLooper(), null)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.NEARBY_WIFI_DEVICES
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        manager.requestGroupInfo(channel) { group ->
            if (group != null) {
                manager.removeGroup(channel, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                    }

                    override fun onFailure(reason: Int) {
                    }
                })
            }
        }
    }

    private fun initSocket() {
        socket = DatagramSocket(null)
        socket.reuseAddress = true
        socket.broadcast = true
        socket.bind(InetSocketAddress(PORT_USED))
        if (!isServer) {
            hostAddress = address?.hostAddress.noNull()
        }
    }

    private fun initRecorder() {
        audioStreamingManager = AudioStreamingManager(this, this)
        audioStreamingManager.initAudioRecorder()
    }

    private fun initSpeaker() {
        val audioManager: AudioManager = this.getSystemService(AUDIO_SERVICE) as AudioManager
        val rate = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        audioManager.setParameters("noise_suppression=on")
        val audioFormat = AudioFormat.Builder()
            .setSampleRate(rate.toInt())
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
            .build()
        removeNoise(audioManager.generateAudioSessionId())
        speaker = AudioTrack(
            audioAttributes,
            audioFormat,
            BUFFER_SIZE_RECORDING,
            AudioTrack.MODE_STREAM,
            0
        )
    }

    private fun removeNoise(generateAudioSessionId: Int) {
        NoiseSuppressor.create(generateAudioSessionId)
        AutomaticGainControl.create(generateAudioSessionId)
        AcousticEchoCanceler.create(generateAudioSessionId)
    }

    override fun sendAction(buf: ByteArray) {
        if (isServer) {
            if (address == null) return
        }
        val packet = DatagramPacket(
            buf,
            buf.size,
            if (isServer) address else InetAddress.getByName(hostAddress),
            PORT_USED
        )

        socket.send(packet)

    }

    private fun receive(): ByteArray? {
        val dataPacket = DatagramPacket(
            myBuffer,
            myBuffer.size
        )
        /* Also here we can get the dataPacket.address which is the address of
        the client phone, so server can send back his recordings */
        socket.receive(dataPacket)
        if (isServer) {
            if (address == null) address = dataPacket.address
        }
        return dataPacket.data
    }
    private fun receiveAudio(buf: ByteArray) {
        speaker.write(buf, 0, BUFFER_SIZE_RECORDING)
        speaker.play()
    }
    companion object {
        const val Bundle_Address = "Bundle_Address"
        const val Bundle_isServer = "Bundle_isServer"
    }
}