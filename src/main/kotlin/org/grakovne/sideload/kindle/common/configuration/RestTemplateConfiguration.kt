package org.grakovne.sideload.kindle.common.configuration

import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RestTemplateConfiguration(
    private val properties: OutboundHttpClientProperties
) {

    @Bean
    fun restTemplate(builder: RestTemplateBuilder) = builder
        .connectTimeout(properties.connectTimeout)
        .readTimeout(properties.readTimeout)
        .build()
}
