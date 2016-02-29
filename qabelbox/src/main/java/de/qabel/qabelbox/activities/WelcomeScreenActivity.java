package de.qabel.qabelbox.activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;

import de.qabel.qabelbox.R;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.fragments.WelcomeDisclaimerFragment;
import de.qabel.qabelbox.fragments.WelcomeTextFragment;
import de.qabel.qabelbox.views.IconPageIndicator;
import de.qabel.qabelbox.views.IconPagerAdapter;
import de.qabel.qabelbox.views.ViewPagerParallax;

/**
 * Created by danny on 11.01.2016.
 */
public class WelcomeScreenActivity extends FragmentActivity {

    private static final int NUM_PAGES = 5;
    Fragment[] fragments = new Fragment[NUM_PAGES];
    int[] messageIds = {R.string.message_welcome_screen1, R.string.message_welcome_screen2, R.string.message_welcome_screen3, R.string.message_welcome_screen4};
    /**
     * The number of pages (wizard steps) to show in this demo.
     */


    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    private ViewPagerParallax mPager;

    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */

    private AppPreference prefs;
    private ScreenSlidePagerAdapter mPagerAdapter;
    private String TAG = this.getClass().getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcomescreen);
        setupAppPreferences();
        // Instantiate a ViewPager and a PagerAdapter.
        mPager = (ViewPagerParallax) findViewById(R.id.pager);
        mPager.set_max_pages(NUM_PAGES);
        mPager.setBackgroundAsset(R.drawable.welcome_big_bg);
        mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);
        //Bind the title indicator to the adapter
        IconPageIndicator titleIndicator = (IconPageIndicator) findViewById(R.id.titles);
        titleIndicator.setViewPager(mPager);
        //titleIndicator.setOnPageChangeListener(mPageChangeListener);
    }

    private void setupAppPreferences() {

        prefs = new AppPreference(this);
    }

    /**
     * A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in
     * sequence.
     */
    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter implements IconPagerAdapter {

        public ScreenSlidePagerAdapter(FragmentManager fm) {

            super(fm);
        }

        @Override
        public Fragment getItem(int position) {

            Log.v(TAG, "fpa: getItem: " + position);
            if (position >= 0 && position < fragments.length) {
                if (fragments[position] == null) {
                    if (position >= 0 && position < 4) {
                        fragments[position] = WelcomeTextFragment.newInstance(messageIds[position]);
                    } else if (position == 4) {
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
            //return R.drawable.welcome_vpi_unselected;
            return R.drawable.welcome_vp_indicator;
        }

        @Override
        public int getCount() {

            return NUM_PAGES;
        }
    }
}
