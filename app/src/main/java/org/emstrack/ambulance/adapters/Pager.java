package org.emstrack.ambulance.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.ViewGroup;

import org.emstrack.ambulance.fragments.DispatcherFragment;
import org.emstrack.ambulance.fragments.GPSFragment;
import org.emstrack.ambulance.fragments.HospitalFragment;

/**
 * Created by mauricio on 2/21/18.
 */

public class Pager extends FragmentStatePagerAdapter {

    Fragment[] registeredFragments;
    int numberOfTabs;

    public Pager(FragmentManager fm, int numberOfTabs) {

        super(fm);

        this.numberOfTabs = numberOfTabs;
        this.registeredFragments = new Fragment[numberOfTabs];

    }

    @Override
    public Fragment getItem(int position) {

        switch (position) {

            case 0:
                return new DispatcherFragment();

            case 1:
                return new HospitalFragment();

            case 2:
                return new GPSFragment();

            default:
                return null;

        }
    }

    @Override
    public int getCount() {
        return numberOfTabs;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Fragment fragment = (Fragment) super.instantiateItem(container, position);
        registeredFragments[position] = fragment;
        return fragment;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        registeredFragments[position] = null;
        super.destroyItem(container, position, object);
    }

    /**
     * Retrieve fragment by position. Inspired on:
     * https://stackoverflow.com/questions/8785221/retrieve-a-fragment-from-a-viewpager/15261142#15261142
     * @param position
     * @return fragment
     */
    public Fragment getRegisteredFragment(int position) {
        return registeredFragments[position];
    }

}