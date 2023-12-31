package org.grakovne.sideload.kindle.common.configuration

import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RestTemplateConfiguration {

    @Bean
    fun restTemplate(builder: RestTemplateBuilder) = builder.build()
}