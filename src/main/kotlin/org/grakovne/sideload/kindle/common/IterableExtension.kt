package org.grakovne.sideload.kindle.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll


suspend fun <T, R> List<T>.parallelMap(
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    transform: suspend (T) -> R
) = scope
    .async {
        val deferredList = map { element ->
            async {
                transform(element)
            }
        }

        deferredList.awaitAll()
    }.await()