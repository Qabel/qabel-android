package de.qabel.qabelbox

import org.mockito.Mockito


fun <T> whenever(methodCall: T) = Mockito.`when`(methodCall)
fun <T> stubResult(methodCall: T, result: T)
        = Mockito.`when`(methodCall).thenReturn(result)

