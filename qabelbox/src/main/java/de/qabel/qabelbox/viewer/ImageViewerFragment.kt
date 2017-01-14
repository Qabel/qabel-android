package de.qabel.qabelbox.viewer

import android.app.Fragment
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import de.qabel.qabelbox.R
import de.qabel.qabelbox.helper.ExternalApps
import de.qabel.qabelbox.ui.extensions.setVisibleOrGone
import kotlinx.android.synthetic.main.fragment_imageviewer.view.*
import org.jetbrains.anko.runOnUiThread

class ImageViewerFragment : Fragment() {

    private lateinit var uri: Uri
    private lateinit var type: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        // Retain this fragment across configuration changes.
        retainInstance = true
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.ab_imageviewer, menu)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_imageviewer_edit -> {
                ExternalApps.openInExternalApp(activity, uri, type, Intent.ACTION_EDIT)
                return true
            }
            R.id.action_imageviewer_open -> {
                ExternalApps.openInExternalApp(activity, uri, type, Intent.ACTION_VIEW)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_imageviewer, container, false)
        runOnUiThread {
            loadImage(view)
        }
        return view
    }

    private fun loadImage(view: View) {
        view.pb_loading.setVisibleOrGone(true)
        Picasso.with(activity)
                .load(uri)
                .resize(4096, 4096)
                .onlyScaleDown()
                .centerInside()
                .error(R.drawable.message_alert_white)
                .into(view.image, object : Callback {
                    override fun onSuccess() {
                        view.pb_loading.setVisibleOrGone(false)
                    }

                    override fun onError() {
                        view.pb_loading.setVisibleOrGone(false)
                    }
                })
    }

    companion object {

        @JvmStatic
        fun newInstance(uri: Uri, type: String): ImageViewerFragment {
            val fragment = ImageViewerFragment()
            fragment.uri = uri
            fragment.type = type
            return fragment
        }
    }

}
