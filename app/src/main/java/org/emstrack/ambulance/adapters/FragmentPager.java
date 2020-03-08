package org.emstrack.ambulance.adapters;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

/**
 * Created by mauricio on 2/21/18.
 */

public class FragmentPager extends FragmentStatePagerAdapter {

    private final CharSequence[] titles;
    Fragment[] fragments;
    int numberOfTabs;
    boolean showTitle = false;

    public FragmentPager(FragmentManager fm,
                         Fragment[] fragments,
                         CharSequence[] titles) {

        super(fm);

        this.fragments = fragments;
        this.numberOfTabs = fragments.length;
        this.titles = titles;

    }

    @Override
    public Fragment getItem(int position) { return (position < numberOfTabs ? fragments[position] : null); }

    @Override
    public CharSequence getPageTitle(int position) { return (showTitle ?
            (position < numberOfTabs ? titles[position] : "") : null); }

    @Override
    public int getCount() {
        return numberOfTabs;
    }

}