package org.grakovne.sideload.kindle.common.configuration

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
class RestTemplateConfiguration(
    @Value("\${http.client.connect-timeout:5s}") private val connectTimeout: Duration,
    @Value("\${http.client.read-timeout:60s}") private val readTimeout: Duration
) {

    @Bean
    fun restTemplate(builder: RestTemplateBuilder) = builder
        .connectTimeout(connectTimeout)
        .readTimeout(readTimeout)
        .build()
}
