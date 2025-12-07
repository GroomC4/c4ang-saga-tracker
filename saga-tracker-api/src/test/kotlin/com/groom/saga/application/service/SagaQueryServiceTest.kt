package com.groom.saga.application.service

import com.groom.saga.application.dto.GetSagaQuery
import com.groom.saga.application.dto.SearchSagasQuery
import com.groom.saga.common.exception.SagaNotFoundException
import com.groom.saga.domain.model.SagaInstance
import com.groom.saga.domain.model.SagaStatus
import com.groom.saga.domain.model.SagaStep
import com.groom.saga.domain.model.SagaType
import com.groom.saga.domain.port.LoadSagaPort
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.Instant

class SagaQueryServiceTest : DescribeSpec({

    val loadSagaPort = mockk<LoadSagaPort>()
    val service = SagaQueryService(loadSagaPort)

    describe("SagaQueryService") {

        describe("getSaga") {
            it("존재하는 Saga를 조회한다") {
                val instance = SagaInstance.create(
                    sagaId = "saga-123",
                    sagaType = SagaType.ORDER_CREATION,
                    orderId = "ORD-001",
                    initialStep = "ORDER_CREATED"
                )

                val steps = listOf(
                    SagaStep.create(
                        sagaId = "saga-123",
                        eventId = "evt-001",
                        step = "ORDER_CREATED",
                        status = SagaStatus.STARTED,
                        recordedAt = Instant.now()
                    )
                )

                every { loadSagaPort.findBySagaId("saga-123") } returns instance
                every { loadSagaPort.findStepsBySagaId("saga-123") } returns steps

                val result = service.getSaga(GetSagaQuery("saga-123"))

                result.sagaId shouldBe "saga-123"
                result.sagaType shouldBe SagaType.ORDER_CREATION
                result.stepCount shouldBe 1
            }

            it("존재하지 않는 Saga 조회 시 예외를 발생시킨다") {
                every { loadSagaPort.findBySagaId("unknown") } returns null

                shouldThrow<SagaNotFoundException> {
                    service.getSaga(GetSagaQuery("unknown"))
                }
            }
        }

        describe("getSagaSteps") {
            it("Saga의 모든 Step을 조회한다") {
                val instance = SagaInstance.create(
                    sagaId = "saga-123",
                    sagaType = SagaType.ORDER_CREATION,
                    orderId = "ORD-001",
                    initialStep = "ORDER_CREATED"
                )

                val steps = listOf(
                    SagaStep.create(
                        sagaId = "saga-123",
                        eventId = "evt-001",
                        step = "ORDER_CREATED",
                        status = SagaStatus.STARTED,
                        recordedAt = Instant.now().minusSeconds(10)
                    ),
                    SagaStep.create(
                        sagaId = "saga-123",
                        eventId = "evt-002",
                        step = "STOCK_RESERVED",
                        status = SagaStatus.IN_PROGRESS,
                        recordedAt = Instant.now()
                    )
                )

                every { loadSagaPort.findBySagaId("saga-123") } returns instance
                every { loadSagaPort.findStepsBySagaId("saga-123") } returns steps

                val result = service.getSagaSteps("saga-123")

                result.size shouldBe 2
                result[0].step shouldBe "ORDER_CREATED"
                result[1].step shouldBe "STOCK_RESERVED"
            }
        }

        describe("searchSagas") {
            it("조건에 맞는 Saga 목록을 조회한다") {
                val instances = listOf(
                    SagaInstance.create(
                        sagaId = "saga-123",
                        sagaType = SagaType.ORDER_CREATION,
                        orderId = "ORD-001",
                        initialStep = "ORDER_CREATED"
                    )
                )

                val page = PageImpl(instances, PageRequest.of(0, 20), 1)

                every {
                    loadSagaPort.searchSagas(
                        orderId = null,
                        sagaType = SagaType.ORDER_CREATION,
                        status = null,
                        fromDate = null,
                        toDate = null,
                        pageable = any()
                    )
                } returns page

                every { loadSagaPort.findStepsBySagaId("saga-123") } returns emptyList()

                val query = SearchSagasQuery(sagaType = SagaType.ORDER_CREATION)
                val result = service.searchSagas(query)

                result.totalElements shouldBe 1
                result.content[0].sagaId shouldBe "saga-123"
            }
        }

        describe("getStatistics") {
            it("통계를 계산한다") {
                every { loadSagaPort.countByStatus() } returns mapOf(
                    SagaStatus.COMPLETED to 90L,
                    SagaStatus.FAILED to 10L,
                    SagaStatus.COMPENSATED to 5L
                )

                every { loadSagaPort.countByType() } returns mapOf(
                    SagaType.ORDER_CREATION to 100L
                )

                val query = SearchSagasQuery()
                val result = service.getStatistics(query)

                result.total shouldBe 105L
                result.byStatus[SagaStatus.COMPLETED] shouldBe 90L
                result.byStatus[SagaStatus.FAILED] shouldBe 10L
                result.failureRate shouldBe (10.0 / 105.0)
                result.compensationRate shouldBe (5.0 / 10.0)
            }
        }
    }
})
