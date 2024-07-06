package org.grakovne.sideload.kindle.shelf.converter

import org.grakovne.sideload.kindle.shelf.domain.ShelfContentItem
import org.grakovne.sideload.kindle.shelf.web.view.ShelfContentItemView
import org.springframework.stereotype.Service

@Service
class ShelfContentItemConverter {
    
    fun apply(item: ShelfContentItem) : ShelfContentItemView = ShelfContentItemView(
        name = item.name,
        fileName = item.file.name,
        environmentId = item.environmentId
    )
}