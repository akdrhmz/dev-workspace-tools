package com.kekik.atlasstream.providers.filmmodu

// ! Bu araÃ§ @keyiflerolsun tarafÄ±ndan | @KekikAkademi iÃ§in yazÄ±lmÄ±ÅŸtÄ±r.



import com.fasterxml.jackson.annotation.JsonProperty


data class GetSource(
    @JsonProperty("subtitle") val subtitle: String?    = null,
    @JsonProperty("sources")  val sources: List<Sources>? = arrayListOf() // arrayListOf() yerine null da olabilir, duruma gÃ¶re
)

data class Sources(
    @JsonProperty("src")            val src: String,
    @JsonProperty("label")          val label: String,
    @JsonProperty("type")           val type: String? = null, // EKLENDÄ°
    @JsonProperty("withCredentials") val withCredentials: Boolean? = null, // EKLENDÄ°
    @JsonProperty("res")            val res: String? = null // EKLENDÄ°
)
