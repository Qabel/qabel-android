package de.qabel.qabelbox.external

open class ExternalAction(val requestCode: Int, val actionType: Int?, val actionParam: String?) {
    constructor(requestCode: Int) : this(requestCode, null, null);
    constructor(requestCode: Int, actionType: Int?) : this(requestCode, actionType, null);
}
