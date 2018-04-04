package org.emstrack.ambulance.adapters;

import android.content.res.Configuration;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.ViewGroup;

import org.emstrack.ambulance.fragments.AmbulanceFragment;
import org.emstrack.ambulance.fragments.MapFragment;
import org.emstrack.ambulance.fragments.HospitalFragment;

/**
 * Created by mauricio on 2/21/18.
 */

public class FragmentPager extends FragmentStatePagerAdapter {

    private final CharSequence[] titles;
    Fragment[] fragments;
    int numberOfTabs;

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
    public CharSequence getPageTitle(int position) { return (position < numberOfTabs ? titles[position] : ""); }

    @Override
    public int getCount() {
        return numberOfTabs;
    }

}