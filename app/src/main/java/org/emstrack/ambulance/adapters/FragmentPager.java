package org.emstrack.ambulance.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mauricio on 2/21/18.
 */

public class FragmentPager extends FragmentStateAdapter {

    private final int[] icons;
    private final int customViewId;
    private final Fragment[] fragments;
    private final List<Integer> visibleTabs;

    public FragmentPager(FragmentManager fm,
                         Lifecycle lifeCycle,
                         Fragment[] fragments,
                         int[] icons,
                         int customViewId) {

        super(fm, lifeCycle);

        assert icons.length == fragments.length;
        this.fragments = fragments;
        this.icons = icons;
        this.customViewId = customViewId;

        // create visibility indices
        this.visibleTabs = new ArrayList<>();
        int numberOfTabs = fragments.length;
        for (int i = 0; i < numberOfTabs; i++)
            this.visibleTabs.add(i);
    }

    public void setIcons(TabLayout.Tab tab, int index) {
        int position = this.visibleTabs.get(index);
        tab.setIcon(icons[position]).setCustomView(this.customViewId);
    }

    public void setTabLayoutMediator(TabLayout tabLayout, ViewPager2 viewPager) {
        assert viewPager.getAdapter() == this;
        TabLayoutMediator tabLayoutMediator = new TabLayoutMediator(tabLayout, viewPager, true, this::setIcons);
        tabLayoutMediator.attach();
    }

    @Override
    public int getItemCount() {
        return this.visibleTabs.size();
    }

    @NonNull
    @Override
    public Fragment createFragment(int index) {
        int position = this.visibleTabs.get(index);
        return fragments[position];
    }

    public void hideTab(int position) {
        int index = visibleTabs.indexOf(position);
        if (index == -1) {
            // tab is already not visible
            return;
        }
        // remove from index
        this.visibleTabs.remove(index);
        notifyItemRemoved(index);
    }

    public void addTab(int position) {
        int index = visibleTabs.indexOf(position);
        if (index != -1) {
            // tab is already visible
            return;
        }
        // add to index
        this.visibleTabs.add(position);
        notifyItemInserted(this.visibleTabs.size()-1);
    }

}