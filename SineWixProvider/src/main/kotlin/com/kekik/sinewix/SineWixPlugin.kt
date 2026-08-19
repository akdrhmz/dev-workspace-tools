package com.kekik.sinewix

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class SineWixPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(SineWixProvider())
    }
}
