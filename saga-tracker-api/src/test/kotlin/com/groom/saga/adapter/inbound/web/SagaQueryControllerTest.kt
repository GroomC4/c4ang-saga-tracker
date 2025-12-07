package com.groom.saga.adapter.inbound.web

import com.groom.saga.application.dto.SagaDetailResult
import com.groom.saga.application.dto.SagaStatisticsResult
import com.groom.saga.application.dto.SagaStepResult
import com.groom.saga.application.service.SagaQueryService
import com.groom.saga.common.exception.SagaNotFoundException
import com.groom.saga.domain.model.SagaStatus
import com.groom.saga.domain.model.SagaType
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.time.Instant

@WebMvcTest(SagaQueryController::class)
class SagaQueryControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockkBean
    lateinit var sagaQueryService: SagaQueryService

    @Test
    fun `GET sagas by id - returns saga detail`() {
        val result = SagaDetailResult(
            sagaId = "saga-123",
            sagaType = SagaType.ORDER_CREATION,
            orderId = "ORD-001",
            currentStatus = SagaStatus.COMPLETED,
            lastStep = "ORDER_CONFIRMED",
            lastTraceId = "trace-abc",
            startedAt = Instant.parse("2024-01-15T10:00:00Z"),
            updatedAt = Instant.parse("2024-01-15T10:05:00Z"),
            stepCount = 4
        )

        every { sagaQueryService.getSaga(any()) } returns result

        mockMvc.get("/api/v1/sagas/saga-123") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.sagaId") { value("saga-123") }
            jsonPath("$.sagaType") { value("ORDER_CREATION") }
            jsonPath("$.currentStatus") { value("COMPLETED") }
            jsonPath("$.stepCount") { value(4) }
        }
    }

    @Test
    fun `GET sagas by id - returns 404 when not found`() {
        every { sagaQueryService.getSaga(any()) } throws SagaNotFoundException("unknown")

        mockMvc.get("/api/v1/sagas/unknown") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.error") { value("NOT_FOUND") }
        }
    }

    @Test
    fun `GET sagas steps - returns steps list`() {
        val steps = listOf(
            SagaStepResult(
                id = 1,
                eventId = "evt-001",
                step = "ORDER_CREATED",
                status = SagaStatus.STARTED,
                producerService = "order-service",
                traceId = "trace-abc",
                metadata = null,
                recordedAt = Instant.parse("2024-01-15T10:00:00Z")
            ),
            SagaStepResult(
                id = 2,
                eventId = "evt-002",
                step = "STOCK_RESERVED",
                status = SagaStatus.IN_PROGRESS,
                producerService = "product-service",
                traceId = "trace-def",
                metadata = null,
                recordedAt = Instant.parse("2024-01-15T10:01:00Z")
            )
        )

        every { sagaQueryService.getSagaSteps(any()) } returns steps

        mockMvc.get("/api/v1/sagas/saga-123/steps") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.sagaId") { value("saga-123") }
            jsonPath("$.steps.length()") { value(2) }
            jsonPath("$.steps[0].step") { value("ORDER_CREATED") }
            jsonPath("$.steps[1].step") { value("STOCK_RESERVED") }
        }
    }

    @Test
    fun `GET sagas - returns paginated list`() {
        val results = listOf(
            SagaDetailResult(
                sagaId = "saga-123",
                sagaType = SagaType.ORDER_CREATION,
                orderId = "ORD-001",
                currentStatus = SagaStatus.COMPLETED,
                lastStep = "ORDER_CONFIRMED",
                lastTraceId = null,
                startedAt = Instant.now(),
                updatedAt = Instant.now(),
                stepCount = 4
            )
        )

        val page = PageImpl(results, PageRequest.of(0, 20), 1)

        every { sagaQueryService.searchSagas(any()) } returns page

        mockMvc.get("/api/v1/sagas") {
            param("sagaType", "ORDER_CREATION")
            param("page", "0")
            param("size", "20")
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.content.length()") { value(1) }
            jsonPath("$.page.totalElements") { value(1) }
        }
    }

    @Test
    fun `GET statistics - returns saga statistics`() {
        val statistics = SagaStatisticsResult(
            fromDate = null,
            toDate = null,
            total = 100,
            byStatus = mapOf(
                SagaStatus.COMPLETED to 90L,
                SagaStatus.FAILED to 10L
            ),
            byType = mapOf(
                SagaType.ORDER_CREATION to 100L
            ),
            failureRate = 0.1,
            compensationRate = 0.5
        )

        every { sagaQueryService.getStatistics(any()) } returns statistics

        mockMvc.get("/api/v1/sagas/statistics") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.total") { value(100) }
            jsonPath("$.failureRate") { value(0.1) }
            jsonPath("$.byStatus.COMPLETED") { value(90) }
        }
    }
}
