package de.qabel.qabelbox.ui.views

import de.qabel.qabelbox.fragments.BaseFragment
import org.jetbrains.anko.AnkoLogger

class FileBrowserFragment: FileBrowserView, BaseFragment(), AnkoLogger {

    companion object {
        fun newInstance() = FileBrowserFragment()
    }

}

