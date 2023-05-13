package com.example.eventsourcing.repository

import com.example.eventsourcing.domain.Aggregate
import com.example.eventsourcing.domain.AggregateType
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.postgresql.util.PGobject
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Types
import java.util.*
import java.util.Map

class AggregateRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val objectMapper: ObjectMapper
) {
    fun createAggregateIfAbsent(
        aggregateType: AggregateType?,
        aggregateId: UUID?
    ) {
        jdbcTemplate.update(
            """
                        INSERT INTO ES_AGGREGATE (ID, VERSION, AGGREGATE_TYPE)
                        VALUES (:aggregateId, 0, :aggregateType)
                        ON CONFLICT DO NOTHING
                        
                        """.trimIndent(),
            Map.of(
                "aggregateId", aggregateId,
                "aggregateType", aggregateType.toString()
            )
        )
    }

    fun checkAndUpdateAggregateVersion(
        aggregateId: UUID?,
        expectedVersion: Int,
        newVersion: Int
    ): Boolean {
        val updatedRows = jdbcTemplate.update(
            """
                        UPDATE ES_AGGREGATE
                           SET VERSION = :newVersion
                         WHERE ID = :aggregateId
                           AND VERSION = :expectedVersion
                        
                        """.trimIndent(),
            Map.of(
                "newVersion", newVersion,
                "aggregateId", aggregateId,
                "expectedVersion", expectedVersion
            )
        )
        return updatedRows > 0
    }

    fun createAggregateSnapshot(aggregate: Aggregate?) {
        try {
            jdbcTemplate.update(
                """
                            INSERT INTO ES_AGGREGATE_SNAPSHOT (AGGREGATE_ID, VERSION, JSON_DATA)
                            VALUES (:aggregateId, :version, :jsonObj::json)
                            
                            """.trimIndent(),
                Map.of(
                    "aggregateId", aggregate!!.aggregateId,
                    "version", aggregate.version,
                    "jsonObj", objectMapper.writeValueAsString(aggregate)
                )
            )
        } catch (e: JsonProcessingException) {
            throw RuntimeException(e)
        }
    }

    fun readAggregateSnapshot(
        aggregateId: UUID,
        version: Int?
    ): Optional<Aggregate> {
        val parameters = MapSqlParameterSource()
        parameters.addValue("aggregateId", aggregateId)
        parameters.addValue("version", version, Types.INTEGER)
        return jdbcTemplate.query(
            """
                        SELECT a.AGGREGATE_TYPE,
                               s.JSON_DATA
                          FROM ES_AGGREGATE_SNAPSHOT s
                          JOIN ES_AGGREGATE a ON a.ID = s.AGGREGATE_ID
                         WHERE s.AGGREGATE_ID = :aggregateId
                           AND (:version IS NULL OR s.VERSION <= :version)
                         ORDER BY s.VERSION DESC
                         LIMIT 1
                        
                        """.trimIndent(),
            parameters
        ) { rs: ResultSet, rowNum: Int -> toAggregate(rs, rowNum) }.stream().findFirst()
    }

    @Throws(SQLException::class)
    private fun toAggregate(rs: ResultSet, rowNum: Int): Aggregate {
        return try {
            val aggregateType = AggregateType.valueOf(rs.getString("AGGREGATE_TYPE"))
            val jsonObj = rs.getObject("JSON_DATA") as PGobject
            val json = jsonObj.value
            objectMapper.readValue(json, aggregateType.aggregateClass)
        } catch (e: JsonProcessingException) {
            throw RuntimeException(e)
        }
    }
}
