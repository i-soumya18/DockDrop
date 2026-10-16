package `in`.soumyaranjansahoo.dockdrop

import android.app.Activity

object DebugEntryPoint {
    fun launchSpikeLabIfDebug(activity: Activity): Boolean = false
    fun openSpikeLab(activity: Activity) {}
}
