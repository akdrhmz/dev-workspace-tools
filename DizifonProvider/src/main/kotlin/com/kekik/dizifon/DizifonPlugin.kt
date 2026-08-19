package com.kekik.dizifon

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class DizifonPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(DizifonProvider())
    }
}
