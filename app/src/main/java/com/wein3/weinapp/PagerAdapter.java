package com.wein3.weinapp;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

public class PagerAdapter extends FragmentStatePagerAdapter {

    //integer to count number of tabs
    int tabCount;

    /**
     * constructor
     *
     * @param fm fragmentmanager
     * @param tabCount number of tabs
     */
    public PagerAdapter (FragmentManager fm, int tabCount) {
        super(fm);
        //Initializing tab count
        this.tabCount= tabCount;
    }

    /**
     * overriding method getItem()
     *
     * @param position integer which indicates position of tab
     * @return current tabs
     */
    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                Tab1 tab1 = new Tab1();
                return tab1;
            case 1:
                Tab2 tab2 = new Tab2();
                return tab2;
            case 2:
                Tab3 tab3 = new Tab3();
                return tab3;
            default:
                return null;
        }
    }

    /**
     * overriding method getCount()
     *
     * @return number of tabs
     */
    @Override
    public int getCount() {
        return tabCount;
    }

}
