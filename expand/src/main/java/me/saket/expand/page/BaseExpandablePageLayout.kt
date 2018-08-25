package me.saket.expand.page

import android.animation.ObjectAnimator
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Outline
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.RelativeLayout
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import me.saket.expand.InboxRecyclerView

/** Animates change in dimensions by clipping bounds instead of changing the layout params. */
abstract class BaseExpandablePageLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : RelativeLayout(context, attrs) {

  val clippedRect = Rect()
  private var dimensionAnimator: ValueAnimator? = null
  private var isFullyVisible: Boolean = false

  var animationDurationMillis = DEFAULT_ANIM_DURATION

  private val clippedWidth: Int
    get() = clippedRect.width()

  protected val clippedHeight: Int
    get() = clippedRect.height()

  val animationInterpolator: TimeInterpolator
    get() = ANIM_INTERPOLATOR

  init {
    init()
  }

  private fun init() {
    outlineProvider = object : ViewOutlineProvider() {
      override fun getOutline(view: View, outline: Outline) {
        outline.setRect(0, 0, clippedRect.width(), clippedRect.height())
        outline.alpha = clippedRect.height().toFloat() / height
      }
    }
  }

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)

    if (isFullyVisible) {
      setClippedDimensions(w, h)
    }
  }

  fun animateDimensions(toWidth: Int, toHeight: Int) {
    cancelOngoingClipAnimation()

    dimensionAnimator = ObjectAnimator.ofFloat(0f, 1f).apply {
      duration = animationDurationMillis
      interpolator = animationInterpolator
      startDelay = InboxRecyclerView.animationStartDelay.toLong()

      val fromWidth = clippedWidth
      val fromHeight = clippedHeight

      addUpdateListener {
        val scale = it.animatedValue as Float
        val newWidth = ((toWidth - fromWidth) * scale + fromWidth).toInt()
        val newHeight = ((toHeight - fromHeight) * scale + fromHeight).toInt()
        setClippedDimensions(newWidth, newHeight)
      }
    }
    dimensionAnimator!!.start()
  }

  fun setClippedDimensions(newClippedWidth: Int, newClippedHeight: Int) {
    isFullyVisible = newClippedWidth > 0 && newClippedHeight > 0 && newClippedWidth == width && newClippedHeight == height

    clippedRect.right = newClippedWidth
    clippedRect.bottom = newClippedHeight

    clipBounds = Rect(clippedRect.left, clippedRect.top, clippedRect.right, clippedRect.bottom)
    invalidateOutline()
  }

  /** Immediately reset the clipping so that this layout is visible. */
  fun resetClipping() {
    setClippedDimensions(width, height)
  }

  private fun cancelOngoingClipAnimation() {
    if (dimensionAnimator != null) {
      dimensionAnimator!!.cancel()
    }
  }

  companion object {
    const val DEFAULT_ANIM_DURATION = 250L
    private val ANIM_INTERPOLATOR = FastOutSlowInInterpolator()
  }
}