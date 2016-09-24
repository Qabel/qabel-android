package de.qabel.qabelbox

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import com.nhaarman.mockito_kotlin.whenever
import rx.Observable
import rx.Single


infix fun <T:Any> T.isEqual(expected: T) = this shouldMatch equalTo(expected)

infix fun <T> T.eq(thing: T) {
    assertThat(this, equalTo(thing))
}

infix fun <T> Observable<T>.evalsTo(thing: T) {
    assertThat(this.toBlocking().first(), equalTo(thing))
}

infix fun <T> Single<T>.evalsTo(thing: T) {
    assertThat(this.toBlocking().value(), equalTo(thing))
}

infix fun <T> Observable<T>.errorsWith(error: Throwable) {
    var e : Throwable? = null
    this.toBlocking().subscribe({}, { e = it})
    e eq error
}

infix fun <T> Single<T>.errorsWith(error: Throwable) {
    var ex : Throwable? = null
    try {
        this.toBlocking().value()
    } catch (e: RuntimeException) {
        ex = e.cause
    }
    ex eq error
}

fun <T> stubMethod(methodCall: T, result: T)
        = whenever(methodCall).thenReturn(result)
