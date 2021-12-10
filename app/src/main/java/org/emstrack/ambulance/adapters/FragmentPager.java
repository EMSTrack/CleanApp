package org.emstrack.ambulance.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/**
 * Created by mauricio on 2/21/18.
 */

public class FragmentPager extends FragmentStateAdapter {

    Fragment[] fragments;
    int numberOfTabs;
    boolean showTitle = false;

    public FragmentPager(FragmentManager fm,
                         Lifecycle lifeCycle,
                         Fragment[] fragments) {

        super(fm, lifeCycle);

        this.fragments = fragments;
        this.numberOfTabs = fragments.length;

    }

    @Override
    public int getItemCount() {
        return numberOfTabs;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return fragments[position];
    }

}