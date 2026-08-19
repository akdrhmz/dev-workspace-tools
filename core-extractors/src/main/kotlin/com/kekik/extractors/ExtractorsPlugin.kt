package com.kekik.extractors

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class ExtractorsPlugin : Plugin() {
    override fun load(context: Context) {
        registerExtractorAPI(RapidVid())
        registerExtractorAPI(VidMoxy())
        registerExtractorAPI(Tortuga())
        registerExtractorAPI(CloseLoad())
        registerExtractorAPI(Vidmoly())
    }
}