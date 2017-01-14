package de.qabel.qabelbox.contacts

private val splitRegex: Regex by lazy { " ".toRegex() }

fun String.toInitials(): String = split(splitRegex).filter {
    it.isNotEmpty() && it.first().isLetterOrDigit()
}.take(2).map {
    it.take(1).toUpperCase()
}.joinToString("")


