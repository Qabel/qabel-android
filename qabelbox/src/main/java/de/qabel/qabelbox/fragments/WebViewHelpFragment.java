package de.qabel.qabelbox.fragments;

import android.os.Bundle;
import android.view.*;
import android.webkit.WebView;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.helper.FileHelper;

public class WebViewHelpFragment extends BaseFragment {

    public static final int MODE_DATA_POLICY = 0;
    public static final int MODE_TOU = 1;
    public static final int MODE_ABOUT_US = 2;
    private static final String PARAM_MODE = "mode";
    private int mode;

    @Override
    public String getTitle() {

        return getResources().getStringArray(R.array.help_headlines)[mode];
    }

    public static WebViewHelpFragment newInstance(int mode) {

        final WebViewHelpFragment fragment = new WebViewHelpFragment();
        Bundle b = new Bundle();
        b.putInt(PARAM_MODE, mode);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        Bundle arguments = getArguments();
        if (arguments != null) {
            mode = arguments.getInt(PARAM_MODE);
        } else {
            new Throwable("Fragment have no arguments");
        }
        setHasOptionsMenu(true);
        mActivity.toggle.setDrawerIndicatorEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);
        setActionBarBackListener();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        menu.clear();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_webview, container, false);
        WebView webView = (WebView) view.findViewById(R.id.webview);
        String file = getResources().getStringArray(R.array.help_asset_filenames)[mode];

        webView.loadDataWithBaseURL("file:///android_asset/", FileHelper.loadFileFromAssets(getActivity(), "html/help/" + file),
                "text/html", "utf-8", null);

        configureWebView(webView);

        return view;
    }

    private void configureWebView(WebView webView) {

    }

    @Override
    public boolean supportBackButton() {

        return true;
    }

    @Override
    public boolean isFabNeeded() {

        return false;
    }
}
