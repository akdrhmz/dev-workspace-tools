package com.kekik.atlasstream.providers.ugurfilm

// ! Bu araÃ§ @keyiflerolsun tarafÄ±ndan | @KekikAkademi iÃ§in yazÄ±lmÄ±ÅŸtÄ±r.



import com.fasterxml.jackson.annotation.JsonProperty


data class AjaxSource(
    @JsonProperty("status")      val status: String,
    @JsonProperty("iframe")      val iframe: String,
    @JsonProperty("alternative") val alternative: String,
)
