package com.kekik.atlasstream.providers.dizikorea

// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.



import com.fasterxml.jackson.annotation.JsonProperty

data class KoreaSearch(
    @JsonProperty("theme") val theme: String
)