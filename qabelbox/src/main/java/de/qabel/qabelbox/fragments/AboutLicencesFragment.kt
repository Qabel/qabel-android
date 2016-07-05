package de.qabel.qabelbox.fragments

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.qabel.qabelbox.R
import de.qabel.qabelbox.adapter.JSONLicencesAdapter
import org.jetbrains.anko.ctx
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class AboutLicencesFragment : BaseFragment() {

    companion object {

        internal val TAG = "AboutLicencesFragment"

        fun newInstance(): AboutLicencesFragment {
            val fragment = AboutLicencesFragment()
            return fragment
        }
    }

    lateinit internal var licensesList: RecyclerView

    override fun getTitle(): String? = ctx.getString(R.string.action_about)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_aboutlicences, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        licensesList = view.findViewById(R.id.about_licences_list) as RecyclerView
        licensesList.adapter = JSONLicencesAdapter(activity,
                readJSONFromAssets("licences.json"),
                readUTF8FromAssets("qapl.txt"))

        licensesList.layoutManager = LinearLayoutManager(activity)
    }

    fun readUTF8FromAssets(filename: String): String {
        try {
            activity.assets.open(filename).use {
                stream ->
                stream.bufferedReader().use {
                    reader ->
                    return reader.readText();
                }
            }
        } catch (ex: IOException) {
            throw RuntimeException("Could not read licencing info", ex)
        }
    }

    @Throws(IOException::class, JSONException::class)
    fun readJSONFromAssets(filename: String): JSONObject {
        return JSONObject(readUTF8FromAssets(filename))
    }

}
