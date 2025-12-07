package com.groom.saga.adapter.inbound.web

import com.groom.saga.adapter.inbound.web.dto.PageInfo
import com.groom.saga.adapter.inbound.web.dto.SagaDetailResponse
import com.groom.saga.adapter.inbound.web.dto.SagaListResponse
import com.groom.saga.adapter.inbound.web.dto.SagaStatisticsResponse
import com.groom.saga.adapter.inbound.web.dto.SagaStepResponse
import com.groom.saga.adapter.inbound.web.dto.SagaStepsResponse
import com.groom.saga.application.dto.GetSagaQuery
import com.groom.saga.application.dto.SearchSagasQuery
import com.groom.saga.application.service.SagaQueryService
import com.groom.saga.domain.model.SagaStatus
import com.groom.saga.domain.model.SagaType
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/api/v1/sagas")
@Tag(name = "Saga Query API", description = "Saga 조회 API")
class SagaQueryController(
    private val sagaQueryService: SagaQueryService
) {

    @GetMapping("/{sagaId}")
    @Operation(summary = "Saga 단건 조회", description = "특정 Saga의 상세 정보를 조회합니다.")
    fun getSaga(
        @PathVariable sagaId: String
    ): ResponseEntity<SagaDetailResponse> {
        val result = sagaQueryService.getSaga(GetSagaQuery(sagaId))
        return ResponseEntity.ok(SagaDetailResponse.from(result))
    }

    @GetMapping("/{sagaId}/steps")
    @Operation(summary = "Saga 단계 조회", description = "특정 Saga의 모든 단계를 조회합니다.")
    fun getSagaSteps(
        @PathVariable sagaId: String
    ): ResponseEntity<SagaStepsResponse> {
        val steps = sagaQueryService.getSagaSteps(sagaId)
        return ResponseEntity.ok(
            SagaStepsResponse(
                sagaId = sagaId,
                steps = steps.map { SagaStepResponse.from(it) }
            )
        )
    }

    @GetMapping
    @Operation(summary = "Saga 목록 조회", description = "조건에 맞는 Saga 목록을 조회합니다.")
    fun searchSagas(
        @Parameter(description = "주문 ID") @RequestParam(required = false) orderId: String?,
        @Parameter(description = "Saga 타입") @RequestParam(required = false) sagaType: String?,
        @Parameter(description = "상태") @RequestParam(required = false) status: String?,
        @Parameter(description = "시작일 (ISO 8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) fromDate: Instant?,
        @Parameter(description = "종료일 (ISO 8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) toDate: Instant?,
        @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<SagaListResponse> {
        val query = SearchSagasQuery(
            orderId = orderId,
            sagaType = sagaType?.let { SagaType.fromString(it) },
            status = status?.let { SagaStatus.fromString(it) },
            fromDate = fromDate,
            toDate = toDate,
            page = page,
            size = size
        )

        val pageResult = sagaQueryService.searchSagas(query)

        return ResponseEntity.ok(
            SagaListResponse(
                content = pageResult.content.map { SagaDetailResponse.from(it) },
                page = PageInfo(
                    number = pageResult.number,
                    size = pageResult.size,
                    totalElements = pageResult.totalElements,
                    totalPages = pageResult.totalPages
                )
            )
        )
    }

    @GetMapping("/statistics")
    @Operation(summary = "Saga 통계 조회", description = "Saga 상태별 통계를 조회합니다.")
    fun getStatistics(
        @Parameter(description = "Saga 타입") @RequestParam(required = false) sagaType: String?,
        @Parameter(description = "시작일 (ISO 8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) fromDate: Instant?,
        @Parameter(description = "종료일 (ISO 8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) toDate: Instant?
    ): ResponseEntity<SagaStatisticsResponse> {
        val query = SearchSagasQuery(
            sagaType = sagaType?.let { SagaType.fromString(it) },
            fromDate = fromDate,
            toDate = toDate
        )

        val result = sagaQueryService.getStatistics(query)
        return ResponseEntity.ok(SagaStatisticsResponse.from(result))
    }
}
