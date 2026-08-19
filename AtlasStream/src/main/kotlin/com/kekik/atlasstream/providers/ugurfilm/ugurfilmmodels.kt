package com.kekik.atlasstream.providers.ugurfilm

// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.



import com.fasterxml.jackson.annotation.JsonProperty


data class AjaxSource(
    @JsonProperty("status")      val status: String,
    @JsonProperty("iframe")      val iframe: String,
    @JsonProperty("alternative") val alternative: String,
)