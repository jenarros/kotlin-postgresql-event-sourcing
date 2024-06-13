package com.example.eventsourcing.adapters.db.projection

import com.example.eventsourcing.domain.OrderAggregate
import com.example.eventsourcing.domain.model.OrderWaypoint

object OrderProjectionMapper {
    fun toProjection(order: OrderAggregate): OrderProjection = OrderProjection()
        .also {
            it.id = order.aggregateId
            it.version = order.version
            it.status = order.status
            it.riderId = order.riderId
            it.price = order.price
            it.route = order.route.map { waypointDtoToWaypointProjection(it) }
            it.driverId = order.driverId
            it.placedDate = order.placedDate
            it.acceptedDate = order.acceptedDate
            it.completedDate = order.completedDate
            it.cancelledDate = order.cancelledDate
        }

    private fun waypointDtoToWaypointProjection(orderWaypoint: OrderWaypoint): WaypointProjection =
        WaypointProjection().also {
            it.address = orderWaypoint.address
            it.latitude = orderWaypoint.latitude
            it.longitude = orderWaypoint.longitude
        }
}