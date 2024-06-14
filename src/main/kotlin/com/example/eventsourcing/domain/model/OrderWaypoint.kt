package com.example.eventsourcing.domain.model

import com.fasterxml.jackson.annotation.JsonProperty

data class OrderWaypoint(
    val address: String,
    @JsonProperty("lat") val latitude: Double,
    @JsonProperty("lon") val longitude: Double
)
