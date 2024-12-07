package gr.andreasagap.moto.communication

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

class SiriVisualView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private var phase = 0f
    private var amplitude = 1.5f // wave height
    private var frequency = 1.2f // number of waves
    private var idleAmplitude = 0.05f // default height
    private var numberOfWaves = 8.0f // number of wave lines
    private var phaseShift = -0.1f // wave speed
    private var primaryWaveLineWidth = 2.0f // outer line stroke
    private var secondaryWaveLineWidth = 0.5f // inner line stroke
    private var density = 5f
    private var mPaintColor: Paint = Paint()
    private var isStraightLine = false
    private val path: Path = Path()
    fun updateViewColor(@ColorInt color: Int) {
        mPaintColor.color = color
    }

    fun updateSpeaking(isSpeaking: Boolean) {
        isStraightLine = isSpeaking
    }

    fun updateAmplitude(ampli: Float) {
        amplitude = max(ampli, idleAmplitude)
    }

    fun updateSpeed(phase: Float) {
        phaseShift = phase
    }

    /** Here you can override default wave values and customize SiriVisualView

    updateNumberOfWaves()
    updatePrimaryLineStroke()
    . . .
     */

    override fun onDraw(canvas: Canvas) {
        if (isStraightLine) {
            var i = 0
            while (i < numberOfWaves) {
                mPaintColor.strokeWidth = if (i == 0) primaryWaveLineWidth else secondaryWaveLineWidth
                val halfHeight = height / 2.toFloat()
                val width = width.toFloat()
                val mid = width / 2.toFloat()
                val maxAmplitude = halfHeight - 4.0f
                val progress = 1.0f - i.toFloat() / numberOfWaves
                val normedAmplitude = (1.5f * progress - 0.5f) * amplitude
                path.reset()
                var x = 0f
                while (x < width + density) {
                    // We use a parable to scale the sinus wave, that has its peak in the middle of the view.
                    val scaling = (-(1 / mid * (x - mid).toDouble()).pow(2.0) + 1).toFloat()
                    val y = (scaling * maxAmplitude * normedAmplitude * sin(2 * Math.PI * (x / width) * frequency + phase) + halfHeight).toFloat()
                    if (x == 0f) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                    x += density
                }
                mPaintColor.style = Paint.Style.STROKE
                mPaintColor.isAntiAlias = true
                canvas.drawPath(path, mPaintColor)
                i++
            }
        } else {
            for(i in 5 downTo -5 step 5) {
                canvas.drawLine(
                    i.toFloat(),
                    height / 2.toFloat(),
                    width.toFloat(),
                    height / 2.toFloat(),
                    mPaintColor
                )
            }
        }
        phase += phaseShift
        invalidate()
    }
}