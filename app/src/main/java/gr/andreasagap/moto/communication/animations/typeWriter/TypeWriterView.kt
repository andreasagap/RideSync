package gr.andreasagap.moto.communication.animations.typeWriter

import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import gr.andreasagap.moto.communication.R


interface TypeWriterListener {
    fun onTypingEnd(text: String?)
}

class TypeWriterView : AppCompatTextView {
    private var mText: CharSequence? = null
    private var mPrintingText: String? = null
    private var mIndex = 0
    private var mDelay: Long = 50 //Default 500ms delay
    private var mContext: Context? = null
    private var mTypeWriterListener: TypeWriterListener? = null
    private var animating = false
    private var mBlinker: Runnable? = null
    private var i = 0
    private var mShowBlink = true
    private val mHandler: Handler = Handler()
    private val mCharacterAdder: Runnable = object : Runnable {
        override fun run() {
            if (animating) {
                mText?.let {
                    text = "${it.subSequence(0, mIndex++)}_"
                    if (mIndex <= it.length) {
                        mHandler.postDelayed(this, mDelay)
                    } else {
                        //typing end
                        mTypeWriterListener?.onTypingEnd(mPrintingText)
                        animating = false
                        if(mShowBlink)
                            callBlink()
                        else
                            text = String.format("%s", it)
                    }
                }
            }
        }
    }

    constructor(context: Context?) : super(context!!) {
        mContext = context
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(
        context!!, attrs
    ) {

        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.TypeWriterView,
            0, 0).apply {

            try {
                mShowBlink = getBoolean(R.styleable.TypeWriterView_showBlink, false)
            } finally {
                recycle()
            }
        }
    }

    private fun callBlink() {
        mBlinker = Runnable {
            if (i <= 10) {
                if (i++ % 2 != 0) {
                    text = String.format("%s _", mText)
                    mBlinker?.let { mHandler.postDelayed(it, 150) }
                } else {
                    text = String.format("%s   ", mText)
                    mBlinker?.let { mHandler.postDelayed(it, 150) }
                }
            } else i = 0
        }
        mHandler.postDelayed(mBlinker!!, 150)
    }


    /**
     * Call this function to display
     *
     * @param text attribute
     */
    fun animateText(text: String?) {
        if (!animating) {
            animating = true
            mText = text
            mPrintingText = text
            mIndex = 0
            setText("")
            mHandler.removeCallbacks(mCharacterAdder)
            //typing start
            mHandler.postDelayed(mCharacterAdder, mDelay)
        } else {
            //CAUTION: Already typing something..
            Toast.makeText(mContext, "Typewriter busy typing: $mText", Toast.LENGTH_SHORT).show()
        }
    }

    fun setInitialText(text: String?){
        mText = text
    }

    /**
     * Call this function to set delay in MILLISECOND [20..150]
     *
     * @param delay
     */
    fun setDelay(delay: Int) {
        if (delay in 20..150) mDelay = delay.toLong()
    }

    /**
     * Call this to remove animation at any time
     */
    fun removeAnimation() {
        mHandler.removeCallbacks(mCharacterAdder)
        animating = false
        text = mText
    }

    /**
     * Set listener to receive typing effects
     *
     * @param typeWriterListener
     */
    fun setTypeWriterListener(typeWriterListener: TypeWriterListener?) {
        mTypeWriterListener = typeWriterListener
    }
}