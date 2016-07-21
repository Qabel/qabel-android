package de.qabel.qabelbox

inline fun <T:Any> checkNull(value: T?, lazyMessage: () -> Any) {
    if (value != null) {
        val message = lazyMessage()
        throw IllegalStateException(message.toString())
    }
}

fun <T:Any> checkNull(value: T?) = checkNull(value) { "Checked nullable value was not null." }

