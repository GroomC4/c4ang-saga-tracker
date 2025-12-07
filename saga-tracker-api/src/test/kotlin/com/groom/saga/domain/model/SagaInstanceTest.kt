package com.groom.saga.domain.model

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.Instant

class SagaInstanceTest : DescribeSpec({

    describe("SagaInstance") {

        describe("create") {
            it("새로운 SagaInstance를 생성한다") {
                val instance = SagaInstance.create(
                    sagaId = "saga-123",
                    sagaType = SagaType.ORDER_CREATION,
                    orderId = "ORD-001",
                    initialStep = "ORDER_CREATED",
                    traceId = "trace-abc"
                )

                instance.sagaId shouldBe "saga-123"
                instance.sagaType shouldBe SagaType.ORDER_CREATION
                instance.orderId shouldBe "ORD-001"
                instance.currentStatus shouldBe SagaStatus.STARTED
                instance.lastStep shouldBe "ORDER_CREATED"
                instance.lastTraceId shouldBe "trace-abc"
                instance.startedAt shouldNotBe null
            }
        }

        describe("updateWithStep") {
            it("Step 정보로 Instance를 업데이트한다") {
                val instance = SagaInstance.create(
                    sagaId = "saga-123",
                    sagaType = SagaType.ORDER_CREATION,
                    orderId = "ORD-001",
                    initialStep = "ORDER_CREATED"
                )

                val step = SagaStep.create(
                    sagaId = "saga-123",
                    eventId = "evt-001",
                    step = "STOCK_RESERVED",
                    status = SagaStatus.IN_PROGRESS,
                    traceId = "trace-def",
                    recordedAt = Instant.now()
                )

                val updated = instance.updateWithStep(step)

                updated.currentStatus shouldBe SagaStatus.IN_PROGRESS
                updated.lastStep shouldBe "STOCK_RESERVED"
                updated.lastTraceId shouldBe "trace-def"
            }

            it("COMPLETED 상태로 업데이트한다") {
                val instance = SagaInstance.create(
                    sagaId = "saga-123",
                    sagaType = SagaType.ORDER_CREATION,
                    orderId = "ORD-001",
                    initialStep = "ORDER_CREATED"
                )

                val step = SagaStep.create(
                    sagaId = "saga-123",
                    eventId = "evt-001",
                    step = "ORDER_CONFIRMED",
                    status = SagaStatus.COMPLETED,
                    recordedAt = Instant.now()
                )

                val updated = instance.updateWithStep(step)

                updated.currentStatus shouldBe SagaStatus.COMPLETED
                updated.isTerminated() shouldBe true
            }
        }

        describe("isTerminated") {
            it("COMPLETED 상태는 종료 상태이다") {
                val instance = SagaInstance.create(
                    sagaId = "saga-123",
                    sagaType = SagaType.ORDER_CREATION,
                    orderId = "ORD-001",
                    initialStep = "ORDER_CREATED"
                ).copy(currentStatus = SagaStatus.COMPLETED)

                instance.isTerminated() shouldBe true
            }

            it("FAILED 상태는 종료 상태이다") {
                val instance = SagaInstance.create(
                    sagaId = "saga-123",
                    sagaType = SagaType.ORDER_CREATION,
                    orderId = "ORD-001",
                    initialStep = "ORDER_CREATED"
                ).copy(currentStatus = SagaStatus.FAILED)

                instance.isTerminated() shouldBe true
            }

            it("IN_PROGRESS 상태는 종료 상태가 아니다") {
                val instance = SagaInstance.create(
                    sagaId = "saga-123",
                    sagaType = SagaType.ORDER_CREATION,
                    orderId = "ORD-001",
                    initialStep = "ORDER_CREATED"
                ).copy(currentStatus = SagaStatus.IN_PROGRESS)

                instance.isTerminated() shouldBe false
            }
        }
    }
})
