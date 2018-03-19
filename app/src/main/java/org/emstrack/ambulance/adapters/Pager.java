package org.emstrack.ambulance.adapters;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.ViewGroup;

import org.emstrack.ambulance.fragments.AmbulanceFragment;
import org.emstrack.ambulance.fragments.DispatcherFragment;
import org.emstrack.ambulance.fragments.HospitalFragment;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mauricio on 2/21/18.
 */

public class Pager extends FragmentStatePagerAdapter {

    Map<String,Fragment> registeredFragmentsByClassName;
    Fragment[] registeredFragments;
    int numberOfTabs;

    public Pager(FragmentManager fm, int numberOfTabs) {

        super(fm);

        this.numberOfTabs = numberOfTabs;
        this.registeredFragments = new Fragment[numberOfTabs];
        this.registeredFragmentsByClassName = new HashMap<String,Fragment>();

    }

    @Override
    public Fragment getItem(int position) {

        switch (position) {

            case 0:
                return new AmbulanceFragment();

            case 1:
                return new HospitalFragment();

            case 2:
                return new DispatcherFragment();

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

        // instantiate fragment
        Fragment fragment = (Fragment) super.instantiateItem(container, position);

        // store fragments for later consultation
        registeredFragments[position] = fragment;
        registeredFragmentsByClassName.put(fragment.getClass().getName(), fragment);

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

    public Fragment getRegisteredFragment(Class cls) {
        return registeredFragmentsByClassName.get(cls.getName());
    }

}