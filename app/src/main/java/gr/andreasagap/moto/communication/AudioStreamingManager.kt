package gr.andreasagap.moto.communication

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.app.ActivityCompat
import gr.andreasagap.moto.communication.threads.CallService


interface AudioAction {
    fun sendAction(buf: ByteArray)
}

private const val RECORDER_SAMPLE_RATE = 44100
private const val RECORDER_CHANNELS: Int = AudioFormat.CHANNEL_IN_STEREO
private const val RECORDER_AUDIO_ENCODING: Int = AudioFormat.ENCODING_PCM_16BIT
val BUFFER_SIZE_RECORDING = AudioRecord.getMinBufferSize(
    RECORDER_SAMPLE_RATE,
    RECORDER_CHANNELS,
    RECORDER_AUDIO_ENCODING
) * 6

class AudioStreamingManager(private val context: Context, private val audioAction: AudioAction) {

    private var audioRecord: AudioRecord? = null


    fun initAudioRecorder() {

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            RECORDER_SAMPLE_RATE, RECORDER_CHANNELS,
            RECORDER_AUDIO_ENCODING, BUFFER_SIZE_RECORDING
        )

        audioRecord?.startRecording()
        record()
    }

    private fun record() {
        Thread {
            try {
                do {
                    val buf = ByteArray(BUFFER_SIZE_RECORDING)
                    val byteRead = audioRecord?.read(buf, 0, buf.size) ?: break
                    if (byteRead < -1)
                        break
                    audioAction.sendAction(buf)
                } while (true)
            } catch (e: Exception) {
                closeService()
            }
        }.start()
    }

    fun stop() {
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    private fun closeService(){
        val intentToStopService = Intent(context, CallService::class.java)
        context.stopService(intentToStopService)
    }

}