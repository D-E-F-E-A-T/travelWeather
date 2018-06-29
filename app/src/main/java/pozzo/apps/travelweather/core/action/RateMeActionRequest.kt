package pozzo.apps.travelweather.core.action

import android.content.Context
import com.splunk.mint.Mint
import pozzo.apps.tools.AndroidUtil
import pozzo.apps.travelweather.R
import pozzo.apps.travelweather.analytics.MapAnalytics
import pozzo.apps.travelweather.map.overlay.MapTutorial
import pozzo.apps.travelweather.map.overlay.Tutorial

class RateMeActionRequest(private val context: Context, private val mapAnalytics: MapAnalytics)
    : ActionRequest(R.string.rateMe) {

    companion object {
        const val AMOUNT_OF_OCCURRENCES = 2
    }

    init {
        mapAnalytics.sendRateDialogShown()
    }

    override fun execute() {
        mapAnalytics.sendIWantToRate()
        if (!AndroidUtil.openUrl(context.getString(R.string.googlePlay), context)) {
            Mint.logException(Exception("Hmm, seems like we have an Android that can't display " +
                    "google play links..."))
        }
    }

    fun isTimeToDisplay(mapTutorial: MapTutorial, daySelectionCount: Int) : Boolean {
        return !mapTutorial.hasPlayed(Tutorial.RATE_DIALOG)
                && daySelectionCount > RateMeActionRequest.AMOUNT_OF_OCCURRENCES
                && mapTutorial.hasPlayed(Tutorial.ROUTE_CREATED_TUTORIAL)
    }
}