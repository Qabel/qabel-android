package de.qabel.qabelbox.external

class ExternalFileAction(requestCode: Int, actionType: Int?, actionParam: String?, var accessMode : String) :
        ExternalAction(requestCode, actionType, actionParam) {
    constructor(requestCode: Int, accessMode : String) : this(requestCode, null, null, accessMode)

}
