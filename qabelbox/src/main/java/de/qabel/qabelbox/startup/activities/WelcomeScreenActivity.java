
package de.qabel.qabelbox.startup.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import de.qabel.qabelbox.R;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.startup.fragments.WelcomeDisclaimerFragment;
import de.qabel.qabelbox.startup.fragments.WelcomeTextFragment;
import de.qabel.qabelbox.ui.views.IconPageIndicator;
import de.qabel.qabelbox.ui.views.IconPagerAdapter;
import de.qabel.qabelbox.ui.views.ViewPagerParallax;

/**
 * Created by danny on 11.01.2016.
 */
public class WelcomeScreenActivity extends FragmentActivity implements ViewPager.OnPageChangeListener {

    private static final int NUM_PAGES = 5;
    private final Fragment[] fragments = new Fragment[NUM_PAGES];
    private final ButtonStates[] buttonStates = new ButtonStates[NUM_PAGES];


    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    private ViewPagerParallax mPager;

    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */
    private final WelcomeTextFragment.TextElement[] textElements = new WelcomeTextFragment.TextElement[NUM_PAGES - 1];
    private AppPreference prefs;
    private ScreenSlidePagerAdapter mPagerAdapter;
    private final String TAG = this.getClass().getSimpleName();
    private TextView leftButton;
    private TextView rightButton;
    private int mCurrentPage = 0;
    private WelcomeScreenActivity self;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        self = this;
        setContentView(R.layout.activity_welcomescreen);
        setupAppPreferences();
        createTextElements();
        createButtonStates();

        leftButton = (TextView) findViewById(R.id.ab_left);
        rightButton = (TextView) findViewById(R.id.ab_right);
        // Instantiate a ViewPager and a PagerAdapter.
        mPager = (ViewPagerParallax) findViewById(R.id.pager);
        mPager.set_max_pages(NUM_PAGES);
        mPager.setBackgroundAsset(R.drawable.welcome_big_bg);
        mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);
        mPager.addOnPageChangeListener(this);
        setClickListeners();
        IconPageIndicator titleIndicator = (IconPageIndicator) findViewById(R.id.titles);
        titleIndicator.setViewPager(mPager);
        onPageSelected(0);
    }

    private void setClickListeners() {
        findViewById(R.id.btn_show_sources).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = getResources().getString(R.string.github_url);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            }
        });
        leftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCurrentPage > 0) {
                    mPager.setCurrentItem(mCurrentPage - 1);
                }
            }
        });
        rightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCurrentPage < NUM_PAGES - 1) {
                    mPager.setCurrentItem(NUM_PAGES - 1);
                } else {
                    Fragment fragment = fragments[mCurrentPage];
                    if (fragment == null ||
                            ((WelcomeDisclaimerFragment) fragment).getCheckedState()) {
                        finish();
                        prefs.setWelcomeScreenShownAt(System.currentTimeMillis());
                        Intent intent = new Intent(self, CreateAccountActivity.class);
                        startActivity(intent);
                    }
                }
            }
        });
    }

    public void setRightButtonColor(int color) {
        rightButton.setTextColor(color);
    }

    private void createButtonStates() {
        buttonStates[0] = new ButtonStates(R.string.empty_text, R.string.btn_welcome_skip, View.INVISIBLE, View.VISIBLE);
        buttonStates[1] = new ButtonStates(R.string.btn_welcome_back, R.string.btn_welcome_skip, View.VISIBLE, View.VISIBLE);
        buttonStates[2] = new ButtonStates(R.string.btn_welcome_back, R.string.btn_welcome_skip, View.VISIBLE, View.VISIBLE);
        buttonStates[3] = new ButtonStates(R.string.btn_welcome_back, R.string.btn_welcome_skip, View.VISIBLE, View.VISIBLE);
        buttonStates[4] = new ButtonStates(R.string.btn_welcome_back, R.string.btn_welcome_accept, View.VISIBLE, View.VISIBLE);
    }

    private void createTextElements() {
        textElements[0] = new WelcomeTextFragment.TextElement(R.string.message_welcome_screen1, R.string.headline_welcome_screen1, R.string.empty_text);
        textElements[1] = new WelcomeTextFragment.TextElement(R.string.message_welcome_screen2, R.string.empty_text, R.string.headline_welcome_screen2);
        textElements[2] = new WelcomeTextFragment.TextElement(R.string.message_welcome_screen3, R.string.empty_text, R.string.headline_welcome_screen3);
        textElements[3] = new WelcomeTextFragment.TextElement(R.string.message_welcome_screen4, R.string.empty_text, R.string.headline_welcome_screen4);
    }

    private void setupAppPreferences() {
        prefs = new AppPreference(this);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {

        ButtonStates state = buttonStates[position];
        leftButton.setText(state.leftTextId);
        rightButton.setText(state.rightTextId);
        leftButton.setVisibility(state.leftTextVisibility);
        rightButton.setVisibility(state.rightTextVisibility);
        //last fragment contain dynamic style
        mCurrentPage = position;
        if (position < NUM_PAGES - 1) {
            setRightButtonColor(getResources().getColor(R.color.welcome_button_activated));
        } else {
            if (fragments[position] == null || !((WelcomeDisclaimerFragment) fragments[position]).getCheckedState()) {
                setRightButtonColor(getResources().getColor(R.color.welcome_button_deactivated));
            } else {
                setRightButtonColor(getResources().getColor(R.color.welcome_button_activated));
            }
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter implements IconPagerAdapter {

        public ScreenSlidePagerAdapter(FragmentManager fm) {

            super(fm);
        }

        @Override
        public Fragment getItem(int position) {

            if (position >= 0 && position < fragments.length) {
                if (fragments[position] == null) {
                    if (position >= 0 && position < NUM_PAGES - 1) {
                        fragments[position] = WelcomeTextFragment.newInstance(textElements[position]);
                    } else {
                        fragments[position] = new WelcomeDisclaimerFragment();
                    }
                }
                Log.d(TAG, fragments[position].toString());
                return fragments[position];
            } else {
                Log.e(TAG, "getItem is out of range: " + position);
                return null;
            }

        }

        @Override
        public int getIconResId(int index) {
            return R.drawable.welcome_vp_indicator;
        }

        @Override
        public int getCount() {

            return NUM_PAGES;
        }
    }

    @Override
    public void onBackPressed() {
        if (mCurrentPage > 0) {
            mPager.setCurrentItem(mCurrentPage - 1);
        } else {
            super.onBackPressed();
        }
    }

    class ButtonStates {
        final int leftTextId;
        final int rightTextId;
        final int leftTextVisibility;
        final int rightTextVisibility;

        ButtonStates(int leftId, int rightId, int leftVisibility, int rightVisibility) {
            leftTextId = leftId;
            rightTextId = rightId;
            leftTextVisibility = leftVisibility;
            rightTextVisibility = rightVisibility;
        }

    }

}
