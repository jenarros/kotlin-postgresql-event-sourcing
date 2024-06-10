package com.example.eventsourcing.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.http4k.format.Jackson

object Json {
    val objectMapper: ObjectMapper = Jackson.mapper.apply {
        enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
        propertyNamingStrategy = PropertyNamingStrategies.LOWER_CAMEL_CASE
        setSerializationInclusion(JsonInclude.Include.NON_NULL)
        configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false)
        configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false)
        configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true)
        configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    fun String.jsonify(): String =
        objectMapper.readTree(this).toPrettyString()
}
