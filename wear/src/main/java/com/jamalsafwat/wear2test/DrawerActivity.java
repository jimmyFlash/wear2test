package com.jamalsafwat.wear2test;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.drawer.WearableActionDrawer;
import android.support.wearable.view.drawer.WearableDrawerLayout;
import android.support.wearable.view.drawer.WearableNavigationDrawer;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.jamalsafwat.wear2test.pojo.Planet;

import java.util.ArrayList;

public class DrawerActivity extends WearableActivity implements WearableActionDrawer.OnMenuItemClickListener {

    private TextView mTextView;
    private WearableDrawerLayout mWearableDrawerLayout;
    private WearableNavigationDrawer mWearableNavigationDrawer;
    private WearableActionDrawer mWearableActionDrawer;

    private ArrayList<Planet> mSolarSystem;
    private int mSelectedPlanet;

    private PlanetFragment mPlanetFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawer);

        setAmbientEnabled();

        mSolarSystem = initializeSolarSystem();
        mSelectedPlanet = 0;

        // Initialize content to first planet.
        mPlanetFragment = new PlanetFragment();
        Bundle args = new Bundle();

        int imageId = getResources().getIdentifier(mSolarSystem.get(mSelectedPlanet).getImage(), "drawable", getPackageName());


        args.putInt(PlanetFragment.ARG_PLANET_IMAGE_ID, imageId);
        mPlanetFragment.setArguments(args);
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, mPlanetFragment).commit();

        // Main Wearable Drawer Layout that wraps all content
        mWearableDrawerLayout = (WearableDrawerLayout) findViewById(R.id.drawer_layout);

        // Top Navigation Drawer
        mWearableNavigationDrawer = (WearableNavigationDrawer) findViewById(R.id.top_navigation_drawer);
        mWearableNavigationDrawer.setAdapter(new NavigationAdapter(this));

        // Bottom Action Drawer
        mWearableActionDrawer =
                (WearableActionDrawer) findViewById(R.id.bottom_action_drawer);

        mWearableActionDrawer.setOnMenuItemClickListener(this);

        // Temporarily peeks the navigation and action drawers to ensure the user is aware of them.
        ViewTreeObserver observer = mWearableDrawerLayout.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mWearableDrawerLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                mWearableDrawerLayout.peekDrawer(Gravity.TOP);
                mWearableDrawerLayout.peekDrawer(Gravity.BOTTOM);
            }
        });

        /* Action Drawer Tip: If you only have a single action for your Action Drawer, you can use a
         * (custom) View to peek on top of the content by calling
         * mWearableActionDrawer.setPeekContent(View). Make sure you set a click listener to handle
         * a user clicking on your View.
         */

    }


    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        final int itemId = menuItem.getItemId();

        String toastMessage = "";

        switch (itemId) {
            case R.id.menu_planet_name:
                toastMessage = mSolarSystem.get(mSelectedPlanet).getName();
                break;
            case R.id.menu_number_of_moons:
                toastMessage = mSolarSystem.get(mSelectedPlanet).getMoons();
                break;
            case R.id.menu_volume:
                toastMessage = mSolarSystem.get(mSelectedPlanet).getVolume();
                break;
            case R.id.menu_surface_area:
                toastMessage = mSolarSystem.get(mSelectedPlanet).getSurfaceArea();
                break;
        }

        mWearableDrawerLayout.closeDrawer(mWearableActionDrawer);

        if (toastMessage.length() > 0) {
            Toast toast = Toast.makeText(
                    getApplicationContext(),
                    toastMessage,
                    Toast.LENGTH_SHORT);
            toast.show();
            return true;
        } else {
            return false;
        }
    }



    private ArrayList<Planet> initializeSolarSystem() {
        ArrayList<Planet> solarSystem = new ArrayList<Planet>();
        String[] planetArrayNames = getResources().getStringArray(R.array.planets_array_names);

        for (int i = 0; i < planetArrayNames.length; i++) {
            String planet = planetArrayNames[i];
            int planetResourceId =
                    getResources().getIdentifier(planet, "array", getPackageName());
            String[] planetInformation = getResources().getStringArray(planetResourceId);

            solarSystem.add(new Planet(
                    planetInformation[0],   // Name
                    planetInformation[1],   // Navigation icon
                    planetInformation[2],   // Image icon
                    planetInformation[3],   // Moons
                    planetInformation[4],   // Volume
                    planetInformation[5])); // Surface area
        }

        return solarSystem;
    }


    /**
     * Fragment that appears in the "content_frame", just shows the currently selected planet.
     */
    public static class PlanetFragment extends Fragment {
        public static final String ARG_PLANET_IMAGE_ID = "planet_image_id";

        private ImageView mImageView;

        public PlanetFragment() {
            // Empty constructor required for fragment subclasses
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_planet, container, false);

            mImageView = ((ImageView) rootView.findViewById(R.id.image));

            int imageIdToLoad = getArguments().getInt(ARG_PLANET_IMAGE_ID);
            mImageView.setImageResource(imageIdToLoad);

            return rootView;
        }

        public void updatePlanet(int imageId) {
            mImageView.setImageResource(imageId);
        }
    }


    private final class NavigationAdapter extends WearableNavigationDrawer.WearableNavigationDrawerAdapter {

        private final Context mContext;

        public NavigationAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getCount() {
            return mSolarSystem.size();
        }

        @Override
        public void onItemSelected(int position) {
            Log.d("PPPPPPPPPP", "WearableNavigationDrawerAdapter.onItemSelected(): " + position);
            mSelectedPlanet = position;

            String selectedPlanetImage = mSolarSystem.get(mSelectedPlanet).getImage();
            int drawableId =
                    getResources().getIdentifier(selectedPlanetImage, "drawable", getPackageName());
            mPlanetFragment.updatePlanet(drawableId);
        }

        @Override
        public String getItemText(int pos) {
            return mSolarSystem.get(pos).getName();
        }

        @Override
        public Drawable getItemDrawable(int pos) {
            String navigationIcon = mSolarSystem.get(pos).getNavigationIcon();

            int drawableNavigationIconId =
                    getResources().getIdentifier(navigationIcon, "drawable", getPackageName());

            return mContext.getDrawable(drawableNavigationIconId);
        }
    }


}
