package com.kekik.atlasstream.providers.watch2movies

// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.



import com.fasterxml.jackson.annotation.JsonProperty


@Suppress("unused")
data class Sources(
    @JsonProperty("type") val type: String,
    @JsonProperty("link") val link: String,
    @JsonProperty("sources") val sources: List<String?>,
    @JsonProperty("tracks") val tracks: List<String?>,
    @JsonProperty("title") val title: String
)