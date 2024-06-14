package com.example.kotlin.functional

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import java.util.*

class Api(private val httpHandler: HttpHandler) {
    fun placeOrder(body: String) =
        httpHandler(
            Request(Method.POST, "/orders")
                .json()
                .body(body)
        )

    fun modifyOrder(orderId: UUID, body: String) =
        httpHandler(
            Request(Method.PUT, "/orders/$orderId")
                .json()
                .body(body)
        )

    fun getOrder(orderId: UUID) =
        httpHandler(
            Request(Method.GET, "/orders/$orderId")
                .json()
        )

    private fun Request.json() =
        header("content-type", "application/json")
            .header("accept", "application/json")
}