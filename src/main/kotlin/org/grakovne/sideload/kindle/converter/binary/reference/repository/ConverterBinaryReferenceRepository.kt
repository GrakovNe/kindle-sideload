package org.grakovne.sideload.kindle.converter.binary.reference.repository

import org.grakovne.sideload.kindle.converter.binary.reference.domain.ConverterBinaryReference
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.*

interface ConverterBinaryReferenceRepository : JpaRepository<ConverterBinaryReference, UUID> {

    @Query(
        value = """FROM ConverterBinaryReference 
        WHERE publishedAt = (
            SELECT MAX(publishedAt) 
            FROM ConverterBinaryReference
        )"""
    )
    fun findLatest(): ConverterBinaryReference?
}