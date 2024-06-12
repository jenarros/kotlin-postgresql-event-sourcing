package com.example.eventsourcing

import com.example.eventsourcing.config.EventSourcingProperties
import com.example.eventsourcing.config.IntegrationEventProperties
import com.example.eventsourcing.config.Json.objectMapper
import com.example.eventsourcing.config.Kafka.TOPIC_ORDER_EVENTS
import com.example.eventsourcing.config.Kafka.kafkaProducer
import com.example.eventsourcing.config.SnapshottingProperties
import com.example.eventsourcing.controller.ErrorHandler
import com.example.eventsourcing.controller.OrdersController
import com.example.eventsourcing.domain.AggregateType
import com.example.eventsourcing.projection.OrderProjection
import com.example.eventsourcing.repository.AggregateRepository
import com.example.eventsourcing.repository.EventRepository
import com.example.eventsourcing.repository.EventSubscriptionRepository
import com.example.eventsourcing.service.AggregateStore
import com.example.eventsourcing.service.CommandProcessor
import com.example.eventsourcing.service.EventSubscriptionProcessor
import com.example.eventsourcing.service.ScheduledEventSubscriptionProcessor
import com.example.eventsourcing.service.command.DefaultCommandHandler
import com.example.eventsourcing.service.command.PlaceOrderCommandHandler
import com.example.eventsourcing.service.event.OrderIntegrationEventSender
import com.example.eventsourcing.service.event.OrderProjectionUpdater
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.flywaydb.core.Flyway
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy
import org.hibernate.jpa.HibernatePersistenceProvider
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.then
import org.http4k.filter.ServerFilters
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.http4k.server.Undertow
import org.http4k.server.asServer
import org.springframework.data.jpa.repository.support.SimpleJpaRepository
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import java.time.Clock
import java.util.*
import kotlin.time.DurationUnit
import kotlin.time.toDuration

fun app(
    clock: Clock,
    kafkaBootstrapServers: String,
    snapshottingProperties: SnapshottingProperties,
    hikariConfig: HikariConfig,
    integrationEventProperties: IntegrationEventProperties
): RoutingHttpHandler {
    val dataSource = HikariDataSource(hikariConfig).also {
        Flyway.configure()
            .dataSource(it)
            .locations("classpath:db/migration") // Specify the location of your migration files
            .load()
            .migrate()
    }
    val namedParameterJdbcTemplate = NamedParameterJdbcTemplate(dataSource)
    val aggregateRepository = AggregateRepository(namedParameterJdbcTemplate, objectMapper)
    val eventRepository = EventRepository(namedParameterJdbcTemplate, objectMapper)
    val aggregateStore = AggregateStore(
        aggregateRepository, eventRepository,
        EventSourcingProperties(mapOf(AggregateType.ORDER to snapshottingProperties))
    )
    val hibernateProperties = Properties().also {
        it.setProperty("hibernate.physical_naming_strategy", CamelCaseToUnderscoresNamingStrategy::class.java.name)
    }

    val entityManager = LocalContainerEntityManagerFactoryBean().also { em ->
        em.dataSource = dataSource
        em.setPackagesToScan("com.example.eventsourcing")
        em.jpaVendorAdapter = HibernateJpaVendorAdapter()
        em.persistenceUnitName = "persistence"
        em.setJpaProperties(hibernateProperties)
        em.setPersistenceProviderClass(HibernatePersistenceProvider::class.java)
        em.afterPropertiesSet()
    }.`object`?.createEntityManager() ?: throw RuntimeException("Unable to create entity manager")

    val orderProjectionRepository =
        SimpleJpaRepository<OrderProjection, UUID>(OrderProjection::class.java, entityManager)

    val commandProcessor = CommandProcessor(
        aggregateStore,
        listOf(PlaceOrderCommandHandler()),
        DefaultCommandHandler(),
        listOf(OrderProjectionUpdater(orderProjectionRepository))
    )

    if (integrationEventProperties.enabled) {
        val kafkaTemplate = kafkaProducer(kafkaBootstrapServers, integrationEventProperties.topic)
        val orderIntegrationEventSender =
            OrderIntegrationEventSender(aggregateStore, kafkaTemplate, integrationEventProperties.topic, objectMapper)
        val eventSubscriptionProcessor =
            EventSubscriptionProcessor(EventSubscriptionRepository(namedParameterJdbcTemplate), eventRepository)

        val scheduledEventSubscriptionProcessor = ScheduledEventSubscriptionProcessor(
            listOf(orderIntegrationEventSender),
            eventSubscriptionProcessor
        )

        GlobalScope.launch {
            while (true) {
                delay(integrationEventProperties.delay) // Delay for 1 second
                scheduledEventSubscriptionProcessor.processNewEvents()
            }
        }
    }

    val ordersController = OrdersController(clock, objectMapper, commandProcessor, orderProjectionRepository)

    return ServerFilters.CatchAll(ErrorHandler)
        .then(
            routes(
                "/orders" bind Method.GET to { request: Request ->
                    ordersController.orders()
                },
                "/orders" bind Method.POST to { request: Request ->
                    ordersController.placeOrder(
                        objectMapper.readTree(request.bodyString())
                    )
                },
                "/orders/{orderId}" bind Method.GET to { request: Request ->
                    ordersController.getOrder(
                        UUID.fromString(request.path("orderId"))
                    )
                },
                "/orders/{orderId}" bind Method.PUT to { request: Request ->
                    ordersController.modifyOrder(
                        UUID.fromString(request.path("orderId")),
                        objectMapper.readTree(request.bodyString())
                    )
                },
            )
        )
}

fun main() {
    app(
        Clock.systemDefaultZone(),
        (System.getenv("KAFKA_BOOTSTRAP_SERVERS") ?: "localhost:9092"),
        SnapshottingProperties(true, 10),
        HikariConfig("/hikari.properties"),
        IntegrationEventProperties(true, 1.toDuration(DurationUnit.SECONDS), TOPIC_ORDER_EVENTS)
    )
        .asServer(Undertow(8080))
        .start()
}
