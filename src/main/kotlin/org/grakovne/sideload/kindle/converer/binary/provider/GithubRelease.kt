package org.grakovne.sideload.kindle.converer.binary.provider

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonNaming
import java.time.Instant

@JsonNaming(SnakeCaseStrategy::class)
data class GitHubRelease(
    val publishedAt: Instant,
    val assets: List<Asset> = emptyList()
)

data class Asset(
    @JsonProperty("browser_download_url")
    val browserDownloadUrl: String
)
