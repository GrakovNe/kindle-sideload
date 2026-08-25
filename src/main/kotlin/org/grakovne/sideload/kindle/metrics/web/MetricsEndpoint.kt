package org.grakovne.sideload.kindle.metrics.web

import jakarta.servlet.http.HttpServletRequest
import mu.KotlinLogging
import org.grakovne.sideload.kindle.metrics.api.MetricsApiService
import org.grakovne.sideload.kindle.metrics.api.domain.DailyMetrics
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/metrics")
class MetricsEndpoint(
    private val metricsApiService: MetricsApiService,
    @Value("\${admin.token}")
    private val adminToken: String
) {

    @GetMapping("/daily")
    fun dailyMetrics(request: HttpServletRequest): ResponseEntity<DailyMetrics> {
        val header = request.getHeader("Authorization")

        if (header != "Bearer $adminToken") {
            logger.debug { "Rejected metrics request: missing or invalid Authorization header" }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        return ResponseEntity.ok(metricsApiService.fetchDailyMetrics())
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
