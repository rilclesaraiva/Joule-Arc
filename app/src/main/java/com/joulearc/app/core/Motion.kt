package com.joulearc.app.core

import androidx.activity.ComponentActivity
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.PausableMonotonicFrameClock
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/** Durations of the app's own animations, in milliseconds. */
object AppMotion {
    const val SHORT = 200
    const val MEDIUM = 350
    const val LONG = 500
}

/** Always 1x: the app defines its animation speed, not the system animation scale. */
private object AppMotionScale : MotionDurationScale {
    override val scaleFactor: Float get() = 1f
}

/**
 * A recomposer whose animations ignore the system animation scale (developer options or
 * "remove animations"), so the app always moves the way it was designed. Frames pause while
 * the activity is stopped, as with the default recomposer, so nothing animates in the background.
 */
fun ComponentActivity.appRecomposer(): Recomposer {
    val main = AndroidUiDispatcher.Main
    val clock = PausableMonotonicFrameClock(requireNotNull(main[MonotonicFrameClock]))
    val context = main + clock + AppMotionScale
    val recomposer = Recomposer(context)
    lifecycleScope.launch(context) { recomposer.runRecomposeAndApplyChanges() }
    lifecycle.addObserver(
        LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> clock.resume()
                Lifecycle.Event.ON_STOP -> clock.pause()
                Lifecycle.Event.ON_DESTROY -> recomposer.cancel()
                else -> Unit
            }
        },
    )
    return recomposer
}
