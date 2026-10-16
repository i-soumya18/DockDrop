package `in`.soumyaranjansahoo.dockdrop

import android.app.Activity
import android.content.Intent
import `in`.soumyaranjansahoo.dockdrop.spikelab.SpikeLabActivity

object DebugEntryPoint {
    fun launchSpikeLabIfDebug(activity: Activity): Boolean {
        if (activity.intent?.getBooleanExtra("open_spikelab", false) == true) {
            activity.startActivity(Intent(activity, SpikeLabActivity::class.java))
            activity.finish()
            return true
        }
        return false
    }

    fun openSpikeLab(activity: Activity) {
        activity.startActivity(Intent(activity, SpikeLabActivity::class.java))
    }
}
