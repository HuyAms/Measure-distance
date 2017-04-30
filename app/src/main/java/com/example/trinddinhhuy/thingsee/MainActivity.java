package com.example.trinddinhhuy.thingsee;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;

import android.content.SharedPreferences;
import android.location.Location;
import android.os.AsyncTask;

import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.example.trinddinhhuy.adapter.CustomAdapter;
import com.example.trinddinhhuy.adapter.CustomInfoAdapter;
import com.example.trinddinhhuy.model.Environment;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.Inflater;

import static com.example.trinddinhhuy.thingsee.R.styleable.MenuItem;
import static com.google.android.gms.maps.GoogleMap.MAP_TYPE_HYBRID;
import static com.google.android.gms.maps.GoogleMap.MAP_TYPE_NONE;
import static com.google.android.gms.maps.GoogleMap.MAP_TYPE_NORMAL;
import static com.google.android.gms.maps.GoogleMap.MAP_TYPE_SATELLITE;
import static com.google.android.gms.maps.GoogleMap.MAP_TYPE_TERRAIN;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final int MAXPOSITIONS = 20;
    private static final String PREFERENCEID = "Credentials";
    private static final int SLEEP_TIME = 10000; //10s

    private String username, password;
    private ArrayList<Location> locationList;
    private List<Environment> environmentList;
    private CustomAdapter myAdapter;
    private TabHost tabHost;
    private Button btnStart, btnEnd, btnSwitchAccount;
    private TextView txtStartingPosition, txtEndPosition, txtDistance, txtAverageSpeed;
    private double latitude;
    private double longitude;
    private float distance;
    private GoogleMap mMap;
    private Timer timer;
    private TimerTask t;
    private Chronometer chronometer;
    private Environment environment;
    private boolean isNewAccount;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestData();

        checkAccount();

        addControls();

        addListeners();

    }

    private void requestData() {
        //Timer task
        timer = new Timer();
        t = new TimerTask() {
            public void run() {
                Log.i("TIMER", "timer");
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.cancel();
                        // we make the request to the Thingsee cloud server in backgroud
                        // (AsyncTask) so that we don't block the UI (to prevent ANR state, Android Not Responding)
                        // This code will always run on the UI thread, therefore is safe to modify UI elements.
                        new TalkToThingsee().execute("QueryState"); //request data from thing see
                    }
                });

            }

        };
        timer.scheduleAtFixedRate(t, SLEEP_TIME, SLEEP_TIME);
    }

    private void checkAccount() {
        // check that we know username and password for the Thingsee cloud
        SharedPreferences prefGet = getSharedPreferences(PREFERENCEID, Activity.MODE_PRIVATE);
        username = prefGet.getString("username", "");
        password = prefGet.getString("password", "");
        if (username.length() == 0 || password.length() == 0)
            // no, ask them from the user
            queryDialog(this, getResources().getString(R.string.prompt));
    }


    private void addListeners() {
        //Button switch account
        btnSwitchAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("USR", "Button pressed");
                //switch accout then request data
                isNewAccount = true;
                queryDialog(MainActivity.this, getResources().getString(R.string.prompt));

            }
        });

        //Button start
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txtStartingPosition.setText(" (" + latitude + " , " +
                        longitude + ")");
                distance = 0;
                txtEndPosition.setText("");

                //disable switch account util end button is clicked
                btnSwitchAccount.setEnabled(false);

                //Set chronometer
                chronometer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
                    @Override
                    public void onChronometerTick(Chronometer cArg) {
                        long time = SystemClock.elapsedRealtime() - cArg.getBase();
                        int h = (int) (time / 3600000);
                        int m = (int) (time - h * 3600000) / 60000;
                        int s = (int) (time - h * 3600000 - m * 60000) / 1000;
                        String hh = h < 10 ? "0" + h : h + "";
                        String mm = m < 10 ? "0" + m : m + "";
                        String ss = s < 10 ? "0" + s : s + "";
                        cArg.setText(hh + ":" + mm + ":" + ss);
                    }
                });
                chronometer.setBase(SystemClock.elapsedRealtime());
                chronometer.start();

                //disable start button util end button is clicked
                btnStart.setEnabled(false);

                //Enable button end
                btnEnd.setEnabled(true);

            }
        });

        //Button end
        btnEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txtEndPosition.setText(" (" + latitude + " , " +
                        longitude + ")");
                txtDistance.setText(Float.toString(distance) + " m");

                //Set chronmeter
                chronometer.stop();

                //Enable start and switch account button
                btnStart.setEnabled(true);
                btnSwitchAccount.setEnabled(true);

                //Calculate average speed
                long time = (SystemClock.elapsedRealtime() - chronometer.getBase()) / 1000; //seconds
                long averageSpeed = (long) (distance / time);
                txtAverageSpeed.setText(averageSpeed + " m/s");

                //Disable button end util button start is pressed
                btnEnd.setEnabled(false);

            }
        });

        //Listen to Tab host Map
        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                if (tabId.equals("tab3")) {
                    //Google map
                    // Obtain the SupportMapFragment and get notified when the map is ready to be used.
                    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                            .findFragmentById(R.id.map);
                    mapFragment.getMapAsync(MainActivity.this);
                }
            }
        });

    }


    private void addControls() {
        environment = new Environment();

        //Connect view
        btnEnd = (Button) findViewById(R.id.btnEnd);
        btnStart = (Button) findViewById(R.id.btnStart);
        btnSwitchAccount = (Button) findViewById(R.id.btnSwitchAccount);
        txtStartingPosition = (TextView) findViewById(R.id.txtStartingPosition);
        txtEndPosition = (TextView) findViewById(R.id.txtEndingPosition);
        txtDistance = (TextView) findViewById(R.id.txtDistance);
        txtAverageSpeed = (TextView) findViewById(R.id.txtAverageSpeed);
        chronometer = (Chronometer) findViewById(R.id.chronmeter);

        //Progress dialog
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setTitle("Notification");
        String message = "Data is loading. Please wait ...";
        SpannableString ss1=  new SpannableString(message);
        ss1.setSpan(new RelativeSizeSpan(1.3f), 0, ss1.length(), 0);
        progressDialog.setMessage(ss1);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();

        // Set up tab host
        tabHost = (TabHost) findViewById(R.id.tabHost);
        tabHost.setup();
        //Tab request data
        TabHost.TabSpec tab1 = tabHost.newTabSpec("tab1");
        tab1.setIndicator("Data");
        tab1.setContent(R.id.tab1);
        tabHost.addTab(tab1);
        //Tab Distance
        TabHost.TabSpec tab2 = tabHost.newTabSpec("tab2");
        tab2.setIndicator("Distance");
        tab2.setContent(R.id.tab2);
        tabHost.addTab(tab2);
        //Tab Map
        TabHost.TabSpec tab3 = tabHost.newTabSpec("tab3");
        tab3.setIndicator("Map");
        tab3.setContent(R.id.tab3);
        tabHost.addTab(tab3);


        // initialize location and environment list
        locationList = new ArrayList<>();
        environmentList = new ArrayList<>();

        // setup the adapter for the array
        myAdapter = new CustomAdapter(MainActivity.this, R.layout.custom_listview, locationList);

        // then connect it to the list in application's layout
        ListView listView = (ListView) findViewById(R.id.mylist);
        listView.setAdapter(myAdapter);


    }

    private void queryDialog(Context context, String msg) {
        // get prompts.xml view
        LayoutInflater li = LayoutInflater.from(context);
        View promptsView = li.inflate(R.layout.credentials_dialog, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);

        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);

        final TextView dialogMsg = (TextView) promptsView.findViewById(R.id.textViewDialogMsg);
        final EditText dialogUsername = (EditText) promptsView.findViewById(R.id.editTextDialogUsername);
        final EditText dialogPassword = (EditText) promptsView.findViewById(R.id.editTextDialogPassword);

        dialogMsg.setText(msg);

        if (isNewAccount) {
            dialogUsername.setText("");
            dialogPassword.setText("");
        } else {
            dialogUsername.setText(username);
            dialogPassword.setText(password);
        }
        isNewAccount = false;

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("Log In",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // get user input and set it to result
                                username = dialogUsername.getText().toString();
                                password = dialogPassword.getText().toString();

                                SharedPreferences prefPut = getSharedPreferences(PREFERENCEID, Activity.MODE_PRIVATE);
                                SharedPreferences.Editor prefEditor = prefPut.edit();
                                prefEditor.putString("username", username);
                                prefEditor.putString("password", password);
                                prefEditor.commit();

                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }


    //Google map
    @Override
    public void onMapReady(GoogleMap googleMap) {
        //Create and clear the map
        mMap = googleMap;
        mMap.clear();
        //Add marker for specific location
        LatLng location = new LatLng(latitude, longitude);
        Marker marker = mMap.addMarker(new MarkerOptions().position(location).title("You are here"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 16));
        mMap.setInfoWindowAdapter(new CustomInfoAdapter(MainActivity.this, environment));
        marker.showInfoWindow();
    }

    //Create option menu for setting map view


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        switch (item.getItemId()) {
            case R.id.normal:
                item.setChecked(true);
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                return true;
            case R.id.satellite:
                if (!item.isChecked()) {
                    item.setChecked(true);
                    mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                }
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }

    }

    /* This class communicates with the ThingSee client on a separate thread (background processing)
             * so that it does not slow down the user interface (UI)
             */
    private class TalkToThingsee extends AsyncTask<String, Environment, String> {
        ThingSee thingsee;
        List<Location> coordinates = new ArrayList<Location>();

        @Override
        protected String doInBackground(String... params) {

            String result = "NOT OK";

            // here we make the request to the cloud server for MAXPOSITION number of coordinates
            try {

                thingsee = new ThingSee(username, password);

                JSONArray events = thingsee.Events(thingsee.Devices(), MAXPOSITIONS);
                //System.out.println(events);
                coordinates = thingsee.getPath(events);
                environmentList = thingsee.getEnvironment(events);
                //Log.i("ENVIRONMENT", environmentList.size()+"");

//                for (Location coordinate: coordinates)
//                    System.out.println(coordinate);
                result = "OK";

                // publishProgress(result);

            } catch (Exception e) {
                Log.d("NET", "Communication error: " + e.getMessage());
            }

            return result;

        }

        @Override
        protected void onPostExecute(String result) {
            // check that the background communication with the client was succesfull
            if (result.equals("OK")) {
                // now the environmentList variable has those coordinates
                // elements of these environmentList is the Environmentn object who has
                // fields for temperature, humidity and airPressure when the position was fixed
                for (int i = 0; i < environmentList.size(); i++) {
                    environment = environmentList.get(i);
                    Log.i("ENVIRONMENT", environment.toString());
                }

                // now the coordinates variable has those coordinates
                // elements of these coordinates is the Location object who has
                // fields for longitude, latitude and time when the position was fixed
                for (int i = 0; i < coordinates.size(); i++) {
                    Location loc = coordinates.get(i);

                    //measure distance
                    if (i > 0) {
                        Location previousLoc = coordinates.get(i - 1);
                        //get previous time and time
                        long previousTime = previousLoc.getTime();
                        long time = loc.getTime();

                        //calculate distance from location to previous location
                        float newDistance = previousLoc.distanceTo(loc);

                        //Toast.makeText(MainActivity.this, newDistance + "", Toast.LENGTH_SHORT).show();
                        //add distance only if new distance>1 and time is different form previous time
                        if (newDistance > 1 && previousTime != time)
                            distance = distance + newDistance;
                    }

                    //set latitude and longitude
                    latitude = coordinates.get(0).getLatitude();
                    longitude = coordinates.get(0).getLongitude();

                    //add location to location list
                    locationList.add(loc);

                    myAdapter.notifyDataSetChanged();
                }

            } else {
                // no, tell that to the user and ask a new username/password pair
                queryDialog(MainActivity.this, getResources().getString(R.string.info_prompt));
            }
            myAdapter.notifyDataSetChanged();
        }

        @Override
        protected void onPreExecute() {
            // first clear the previous entries (if they exist)
            locationList.clear();
            myAdapter.notifyDataSetChanged();
        }

        @Override
        protected void onProgressUpdate(Environment... params) {

        }
    }
}
