package de.qabel.qabelbox

import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch


infix fun <T:Any> T.isEqual(expected: T) = this shouldMatch equalTo(expected)

