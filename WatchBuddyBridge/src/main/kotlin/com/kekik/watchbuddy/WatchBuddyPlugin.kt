package com.kekik.watchbuddy

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class WatchBuddyPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(WatchBuddyProvider())
    }
}