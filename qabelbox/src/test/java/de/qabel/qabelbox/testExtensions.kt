package de.qabel.qabelbox

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import com.nhaarman.mockito_kotlin.whenever
import de.qabel.qabelbox.util.waitFor
import rx.Observable
import rx.lang.kotlin.onError


infix fun <T:Any> T.isEqual(expected: T) = this shouldMatch equalTo(expected)

infix fun <T> T.eq(thing: T) {
    assertThat(this, equalTo(thing))
}

infix fun <T> Observable<T>.evalsTo(thing: T) {
    assertThat(this.toBlocking().first(), equalTo(thing))
}

infix fun <T> Observable<T>.matches(matcher: Matcher<T>) {
    assertThat(this.toBlocking().first(), matcher)
}

infix fun <T> Observable<T>.errorsWith(error: Throwable) {
    var e : Throwable? = null
    this.toBlocking().subscribe({}, { e = it})
    e eq error
}

fun <T> stubMethod(methodCall: T, result: T)
        = whenever(methodCall).thenReturn(result)
