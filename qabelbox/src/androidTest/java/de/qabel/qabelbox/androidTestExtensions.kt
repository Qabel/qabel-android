package de.qabel.qabelbox

import org.mockito.Mockito


fun <T> stubResult(methodCall: T, result: T)
        = Mockito.`when`(methodCall).thenReturn(result)
