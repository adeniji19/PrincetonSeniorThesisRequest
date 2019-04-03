package thesis.ogunlana.com.dronerequest;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends FragmentActivity implements View.OnClickListener, GoogleMap.OnMapClickListener, OnMapReadyCallback, ValueEventListener {

    private static String[][] pickUpSpots = new String[][]{
            new String[]{"Scully Courtyard", "40.344517", "-74.654794"},
            new String[]{"Yoseloff Courtyard", "40.344065", "-74.656240"},
            new String[]{"1963 Courtyard", "40.343804", "-74.657956"},
            new String[]{"Fisher Hall Courtyard", "40.344387", "-74.658086"},
            new String[]{"Behind Spellman", "40.344788", "-74.659599"},
            new String[]{"Pyne Courtyard", "40.345447", "-74.659644"},
            new String[]{"Henry Courtyard", "40.346085", "-74.660556"},
            new String[]{"Lockhart/Foulke Courtyard", "40.346832", "-74.660791"},
            new String[]{"Little Courtyard", "40.347043", "-74.659890"},
            new String[]{"Mathey Courtard", "40.347747", "-74.661598"},
            new String[]{"Rockefeller Courtyard", "40.348558", "-74.661565"},
            new String[]{"Hamilton Courtyard", "40.348329", "-74.662161"},
            new String[]{"Wilson Courtyard", "40.345336", "-74.656016"},
            new String[]{"1903 Courtyard", "40.346023", "-74.657142"}
    };

    private int idAttemptCount = 0;
    private FusedLocationProviderClient mFusedLocationClient;
    private String netid, pickUpLoc, displayName;
    private TextView statusText, locationText, deliveryText;
    private TextView idNumText, buildingText, roomNumText, droneStatusText;
    private Button signOutButton, requestBtn, receivedBtn, confBtn;
    private RadioButton proxBtn, packageBtn, lineBtn, triBtn, sqBtn, zzBtn;
    private LinearLayout confirmShapeLayout;
    private LinearLayout proxLayout, packageLayout, confirmLayout;
    private RadioGroup deliveryType;
    private String code = "null", shape = "null";
    private LatLng droneLocation;
    private GoogleMap gMap;
    private Marker droneMarker = null;

    private static final String TAG = "SignInActivity";

    private FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
    private DatabaseReference mRootReference = firebaseDatabase.getReference();
    private DatabaseReference deliveryRequest = mRootReference.child("users");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.v("testing", "beginning");
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        Bundle extras = getIntent().getExtras();
        netid = extras.getString("netid");
        netid = netid.replaceAll("\\.", "");
        displayName = extras.getString("displayname");

        statusText = (TextView) findViewById(R.id.statusText);
        locationText = (TextView) findViewById(R.id.gpsText);
        idNumText = (TextView) findViewById(R.id.idNumText);
        buildingText = (TextView) findViewById(R.id.buildingText);
        roomNumText = (TextView) findViewById(R.id.roomNumText);
        deliveryText = (TextView) findViewById(R.id.deliveryIdText);
        droneStatusText = (TextView) findViewById(R.id.droneStatusText);
        confirmShapeLayout = (LinearLayout) findViewById(R.id.confirmShapeLayout);
        proxLayout = (LinearLayout) findViewById(R.id.proxLayout);
        packageLayout = (LinearLayout) findViewById(R.id.packageLayout);
        confirmLayout = (LinearLayout) findViewById(R.id.confirmDelivLayout);
        deliveryType = (RadioGroup) findViewById(R.id.deliveryType);
        proxBtn = (RadioButton) findViewById(R.id.proxBtn);
        proxBtn.setOnClickListener(this);
        packageBtn = (RadioButton) findViewById(R.id.packageBtn);
        packageBtn.setOnClickListener(this);
        lineBtn = (RadioButton) findViewById(R.id.radioButton1);
        triBtn = (RadioButton) findViewById(R.id.radioButton2);
        sqBtn = (RadioButton) findViewById(R.id.radioButton3);
        zzBtn = (RadioButton) findViewById(R.id.radioButton4);
        requestBtn = (Button) findViewById(R.id.requestBtn);
        requestBtn.setOnClickListener(this);
        receivedBtn = (Button) findViewById(R.id.receivedBtn);
        receivedBtn.setOnClickListener(this);
        confBtn = (Button) findViewById(R.id.confBtn);
        confBtn.setOnClickListener(this);

        signOutButton = (Button) findViewById(R.id.signOut);
        signOutButton.setOnClickListener(this);

        statusText.setText("Hello, " + displayName);

        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        droneLocation = null;
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.signOut:
                signOut();
                break;
            case R.id.requestBtn:
                droneRequest();
                break;
            case R.id.packageBtn:
                packageSecurity();
                break;
            case R.id.proxBtn:
                proxSecurity();
                break;
            case R.id.receivedBtn:
                received();
                break;
            case R.id.confBtn:
                confirmShape();
                break;
        }
    }

    private void confirmShape() {
        Log.d("ShapeID", shape + " " + idAttemptCount);
        if(shape.equals("CONFIRMED") || shape.equals("UNCONFIRMED"))
            return;
        idAttemptCount++;
        String chosenShape;
        if(lineBtn.isChecked()) {
            chosenShape = "LINE";
        } else if(triBtn.isChecked()) {
            chosenShape = "TRIANGLE";
        } else if(sqBtn.isChecked()) {
            chosenShape = "SQUARE";
        } else if(zzBtn.isChecked()) {
            chosenShape = "ZIGZAG";
        } else {
            idAttemptCount--;
            setResultToToast("What's the drone shape?");
            return;
        }
        Log.d("ShapeID", chosenShape + " " + shape + " " + idAttemptCount);

        if(shape.equals("null")) {
            idAttemptCount--;
        }
        if(chosenShape.equals(shape) && idAttemptCount <= 2) {
            deliveryRequest.child(netid).child("shape").setValue("CONFIRMED");
            setResultToToast("DroneID confirmed");
            confirmLayout.setVisibility(View.VISIBLE);
            confirmShapeLayout.setVisibility(View.GONE);
        }
        else if(idAttemptCount >= 2) {
            setResultToToast("Too many false IDs.");
            deliveryRequest.child(netid).child("shape").setValue("UNCONFIRMED");
        }
    }

    private void received() {
        if (!deliveryText.getText().toString().equals(code)) {
            setResultToToast("Incorrect Delivery ID. Try Again.");
            return;
        }
        locationText.setText("");
        deliveryText.setText("");

        String identity;
        if (proxLayout.getVisibility() == View.VISIBLE && proxLayout.isShown()) {
            identity = buildingText.getText().toString() + " " + roomNumText.getText().toString();
        } else {
            identity = idNumText.getText().toString();
        }
        createRequestNode(netid, "0000", identity, pickUpLoc, Long.toString(System.currentTimeMillis()));
        droneStatusText.setText("Status: Drone Returning To Sender");
    }

    private void signOut() {
        Intent myIntent = new Intent(MainActivity.this, OpeningActivity.class);
        myIntent.putExtra("close", true);
        MainActivity.this.startActivity(myIntent);
    }

    @SuppressLint("MissingPermission")
    private void droneRequest() {
        idAttemptCount = 0;
        Log.d("Drone Request", "request made");
        locationText.setText("Getting location...");
        mFusedLocationClient.getLastLocation()
            .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    // Got last known location. In some rare situations this can be null.
                    if (location != null) {
                        Log.d("Drone Request", location.getLatitude() + " " + location.getLongitude());
                        pickUpLoc = getPickUpLocation(location);
                        locationText.setText("Pick up location: " + pickUpLoc);
                        droneStatusText.setText("Status: Request Not Seen Yet");

                        String identity;
                        if (proxLayout.getVisibility() == View.VISIBLE && proxLayout.isShown()) {
                            identity = buildingText.getText().toString() + " " + roomNumText.getText().toString();
                        } else {
                            identity = idNumText.getText().toString();
                        }
                        createRequestNode(netid, "null", identity, pickUpLoc, Long.toString(System.currentTimeMillis()));
                    }
                }
            });
    }

    private void createRequestNode(String netid, String code, String identity, String location, String time) {
        netid = netid.replaceAll("\\.", "");
        deliveryRequest.child(netid).child("code").setValue(code);
        deliveryRequest.child(netid).child("netid").setValue(netid);
        deliveryRequest.child(netid).child("identity").setValue(identity);
        deliveryRequest.child(netid).child("droplocation").setValue(location);
        deliveryRequest.child(netid).child("dronelocationlat").setValue("null");
        deliveryRequest.child(netid).child("dronelocationlng").setValue("null");
        deliveryRequest.child(netid).child("time").setValue(time);
        deliveryRequest.child(netid).child("shape").setValue("null");
    }

    private String getPickUpLocation(Location location) {
        double lat = location.getLatitude();
        double lon = location.getLongitude();
        String dropPoint = "Location not found.";
        double minDist = Double.MAX_VALUE;
        double currDist;

        for (String[] loc : pickUpSpots) {
            currDist = dist(lat, lon, Double.parseDouble(loc[1]), Double.parseDouble(loc[2]));
            if (currDist < minDist) {
                minDist = currDist;
                dropPoint = loc[0];
            }
        }

        return dropPoint;
    }

    private double dist(double x1, double y1, double x2, double y2) {
        return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
    }

    private void packageSecurity() {
        Log.d(TAG, "package switch");
        packageLayout.setVisibility(View.VISIBLE);
        proxLayout.setVisibility(View.GONE);
    }

    private void proxSecurity() {
        Log.d(TAG, "prox switch");
        packageLayout.setVisibility(View.GONE);
        proxLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
        // update student about drone status - only their netid should be monitored
        HashMap<String, HashMap<String, String>> users;
        if (dataSnapshot.getKey() != null) {
            users = (HashMap<String, HashMap<String, String>>) dataSnapshot.getValue();
            if (users == null)
                return;
            for (Map.Entry<String, HashMap<String, String>> entry : users.entrySet()) {
                HashMap<String, String> user = entry.getValue();

                if (user != null && user.containsKey("netid") && user.get("netid").equals(netid)
                        && user.containsKey("code") && user.containsKey("identity") && user.containsKey("droplocation")) {

                    if(user.containsKey("dronelocationlat") && user.containsKey("dronelocationlng")) {
                        if(gMap != null && !(user.get("dronelocationlat").equals("null") || user.get("dronelocationlng").equals("null"))) {
                            droneLocation = new LatLng(Double.parseDouble(user.get("dronelocationlat")), Double.parseDouble(user.get("dronelocationlng")));
                            updateDroneLocation();
                        }
                    }
                    if(user.containsKey("shape"))
                        shape = user.get("shape");
                    if((user.containsKey("shape") && !user.get("shape").equals("CONFIRMED") && !user.get("shape").equals("UNCONFIRMED")) && !user.get("shape").equals("null")) {
                        confirmShapeLayout.setVisibility(View.VISIBLE);
                    }
                    if (user.containsKey("shape") && user.get("shape").equals("CONFIRMED") && user.get("code").equals("0000")) {
                        droneStatusText.setText("Status: Drone Will Return To Sender");
                        confirmLayout.setVisibility(View.GONE);
                        confirmShapeLayout.setVisibility(View.GONE);
                        return;
                    } else if (user.containsKey("shape") && user.get("shape").equals("CONFIRMED")) {
                        droneStatusText.setText("Status: Shape Confirmed");
                        confirmLayout.setVisibility(View.VISIBLE);
                        confirmShapeLayout.setVisibility(View.GONE);
                        return;
                    } else if (user.containsKey("shape") && !user.get("shape").equals("CONFIRMED") && user.containsKey("code") && !user.get("code").equals("null")) {
                        code = user.get("code");
                        droneStatusText.setText("Status: Acknowledged & Sending Drone");
                        confirmLayout.setVisibility(View.GONE);
                        confirmShapeLayout.setVisibility(View.VISIBLE);
                    }
                }
            }
        }
    }

    @Override
    public void onCancelled(@NonNull DatabaseError databaseError) {

    }


    protected void onStart() {
        super.onStart();
        deliveryRequest.addValueEventListener(this);
    }

    @Override
    public void onMapClick(LatLng latLng) {
        setResultToToast("Cannot Add Waypoint Manually");
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (gMap == null) {
            gMap = googleMap;
            setUpMap();
            float zoomlevel = (float) 15.0;
            CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(new LatLng(40.3468, -74.6552), zoomlevel);
            gMap.moveCamera(cu);
        }
    }

    private void setUpMap() {
        gMap.setOnMapClickListener(this);// add the listener for click for a map object
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        gMap.setMyLocationEnabled(true);
    }

    private void setResultToToast(final String string){
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, string, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Update the drone location on the map
    private void updateDroneLocation(){
        //Create MarkerOptions object
        final MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(droneLocation);
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.box_package));

        Log.d("DATA_STUFF", "inside update drone");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d("DATA_STUFF", "in runnable " + droneLocation.latitude + " " + droneLocation.longitude);
                if (droneMarker != null) {
                    droneMarker.remove();
                }

                if (checkGpsCoordination(droneLocation.latitude, droneLocation.longitude)) {
                    droneMarker = gMap.addMarker(markerOptions);
                }
            }
        });
    }

    private static boolean checkGpsCoordination(double latitude, double longitude) {
        return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
    }
}