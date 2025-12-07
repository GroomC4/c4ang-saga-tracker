package com.groom.saga.application.service

import com.groom.saga.application.dto.ProcessSagaEventCommand
import com.groom.saga.domain.model.SagaInstance
import com.groom.saga.domain.model.SagaStatus
import com.groom.saga.domain.model.SagaStep
import com.groom.saga.domain.model.SagaType
import com.groom.saga.domain.port.LoadSagaPort
import com.groom.saga.domain.port.SagaMetricsPort
import com.groom.saga.domain.port.SaveSagaPort
import com.groom.saga.domain.service.SagaAggregator
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant

class SagaEventProcessorTest : DescribeSpec({

    // 테스트에서 사용하는 Mocks 컨테이너
    data class TestMocks(
        val loadSagaPort: LoadSagaPort,
        val saveSagaPort: SaveSagaPort,
        val sagaMetricsPort: SagaMetricsPort,
        val processor: SagaEventProcessor
    )

    // 각 테스트에서 새로운 mock 인스턴스 생성을 위한 헬퍼 함수
    fun createProcessor(): TestMocks {
        val loadSagaPort = mockk<LoadSagaPort>()
        val saveSagaPort = mockk<SaveSagaPort>()
        val sagaMetricsPort = mockk<SagaMetricsPort>(relaxed = true)
        val sagaAggregator = SagaAggregator()

        val processor = SagaEventProcessor(
            loadSagaPort = loadSagaPort,
            saveSagaPort = saveSagaPort,
            sagaMetricsPort = sagaMetricsPort,
            sagaAggregator = sagaAggregator
        )
        return TestMocks(loadSagaPort, saveSagaPort, sagaMetricsPort, processor)
    }

    describe("SagaEventProcessor") {

        describe("process") {

            it("새로운 Saga 이벤트를 처리하고 저장한다") {
                val mocks = createProcessor()
                val command = ProcessSagaEventCommand(
                    eventId = "evt-001",
                    eventTimestamp = Instant.now(),
                    sagaId = "saga-123",
                    sagaType = SagaType.ORDER_CREATION,
                    step = "ORDER_CREATED",
                    status = SagaStatus.STARTED,
                    orderId = "ORD-001",
                    metadata = null,
                    recordedAt = Instant.now()
                )

                every { mocks.loadSagaPort.findStepsByEventId("evt-001") } returns null
                every { mocks.loadSagaPort.findBySagaId("saga-123") } returns null
                every { mocks.saveSagaPort.saveInstanceAndStep(any(), any()) } returns Pair(
                    mockk<SagaInstance>(),
                    mockk<SagaStep>()
                )

                mocks.processor.process(command)

                verify { mocks.saveSagaPort.saveInstanceAndStep(any(), any()) }
                verify { mocks.sagaMetricsPort.incrementProcessedEvents("ORDER_CREATED", SagaStatus.STARTED) }
            }

            it("중복 이벤트는 무시한다") {
                val mocks = createProcessor()
                val command = ProcessSagaEventCommand(
                    eventId = "evt-001",
                    eventTimestamp = Instant.now(),
                    sagaId = "saga-123",
                    sagaType = SagaType.ORDER_CREATION,
                    step = "ORDER_CREATED",
                    status = SagaStatus.STARTED,
                    orderId = "ORD-001",
                    metadata = null,
                    recordedAt = Instant.now()
                )

                every { mocks.loadSagaPort.findStepsByEventId("evt-001") } returns mockk<SagaStep>()

                mocks.processor.process(command)

                verify(exactly = 0) { mocks.saveSagaPort.saveInstanceAndStep(any(), any()) }
            }

            it("기존 Saga에 새 Step을 추가한다") {
                val mocks = createProcessor()
                val existingInstance = SagaInstance.create(
                    sagaId = "saga-123",
                    sagaType = SagaType.ORDER_CREATION,
                    orderId = "ORD-001",
                    initialStep = "ORDER_CREATED"
                )

                val command = ProcessSagaEventCommand(
                    eventId = "evt-002",
                    eventTimestamp = Instant.now(),
                    sagaId = "saga-123",
                    sagaType = SagaType.ORDER_CREATION,
                    step = "STOCK_RESERVED",
                    status = SagaStatus.IN_PROGRESS,
                    orderId = "ORD-001",
                    metadata = null,
                    recordedAt = Instant.now()
                )

                every { mocks.loadSagaPort.findStepsByEventId("evt-002") } returns null
                every { mocks.loadSagaPort.findBySagaId("saga-123") } returns existingInstance
                every { mocks.saveSagaPort.saveInstanceAndStep(any(), any()) } returns Pair(
                    mockk<SagaInstance>(),
                    mockk<SagaStep>()
                )

                mocks.processor.process(command)

                verify { mocks.saveSagaPort.saveInstanceAndStep(any(), any()) }
            }

            it("FAILED 상태일 때 메트릭을 증가시킨다") {
                val mocks = createProcessor()
                val command = ProcessSagaEventCommand(
                    eventId = "evt-003",
                    eventTimestamp = Instant.now(),
                    sagaId = "saga-123",
                    sagaType = SagaType.ORDER_CREATION,
                    step = "STOCK_RESERVATION_FAILED",
                    status = SagaStatus.FAILED,
                    orderId = "ORD-001",
                    metadata = mapOf("failureReason" to "OUT_OF_STOCK"),
                    recordedAt = Instant.now()
                )

                every { mocks.loadSagaPort.findStepsByEventId("evt-003") } returns null
                every { mocks.loadSagaPort.findBySagaId("saga-123") } returns null
                every { mocks.saveSagaPort.saveInstanceAndStep(any(), any()) } returns Pair(
                    mockk<SagaInstance>(),
                    mockk<SagaStep>()
                )

                mocks.processor.process(command)

                verify { mocks.sagaMetricsPort.incrementFailed() }
            }
        }
    }
})
