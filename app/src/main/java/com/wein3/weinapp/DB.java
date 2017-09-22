package com.wein3.weinapp;

import android.content.Context;
import android.support.v4.app.NavUtils;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.support.design.widget.TabLayout;

import com.wein3.weinapp.R;

public class DB extends AppCompatActivity implements TabLayout.OnTabSelectedListener {

    public TabLayout tabLayout;
    public ViewPager viewPager;

    /**
     * create Activity
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_db);

        //Adding toolbar to the activity
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Initializing the tablayout
        tabLayout = (TabLayout) findViewById(R.id.tab_layout);

        //Adding the tabs using addTab() method
        tabLayout.addTab(tabLayout.newTab().setText("Overview"));
        tabLayout.addTab(tabLayout.newTab().setText("Details"));
        tabLayout.addTab(tabLayout.newTab().setText("Action"));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        //Initializing viewPager
        viewPager = (ViewPager) findViewById(R.id.pager);

        //Creating pager adapter
        PagerAdapter adapter = new PagerAdapter() {
            /**
             * get number of available views
             *
             * @return 0
             */
            @Override
            public int getCount() {
                return 0;
            }

            /**
             *checks if a pageview is associated with key object
             *
             * @param view pageview to check for association with object
             * @param object object to check association with pageview
             * @return true if they are associated
             */
            @Override
            public boolean isViewFromObject(View view, Object object) {
                return false;
            }
        };

        //Adding adapter to pager
        viewPager.setAdapter(adapter);

        //Adding onTabSelectedListener to swipe views
        tabLayout.addOnTabSelectedListener(this);
    }

    /**
     * is called when the return/home/up button is selected
     * finishes this view and leads back to basic activity
     *
     * @param item up/home/back button
     * @return true if button is selected
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * called, when a tab enters the tab selected status
     *
     * @param tab which is selected
     */
    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        viewPager.setCurrentItem(tab.getPosition());
    }

    /**
     * called, when tab exists tab selected status
     *
     * @param tab corresponding tab
     */
    @Override
    public void onTabUnselected(TabLayout.Tab tab) {

    }

    /**
     * called, when tab, which already has selected status, is selected again
     *
     * @param tab which is selected
     */
    @Override
    public void onTabReselected(TabLayout.Tab tab) {

    }
}
