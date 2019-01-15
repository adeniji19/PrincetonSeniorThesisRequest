package thesis.ogunlana.com.dronerequest;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Text;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener, View.OnClickListener, ValueEventListener {

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

    private FusedLocationProviderClient mFusedLocationClient;
    private String netid, pickUpLoc;
    private GoogleApiClient mGoogleApiClient;
    private TextView statusText, netidText, locaitonText, deliveryText;
    private TextView idNumText, buildingText, roomNumText, droneStatusText;
    private SignInButton signInButton;
    private Button signOutButton, requestBtn, receivedBtn;
    private RadioButton proxBtn, packageBtn;
    private LinearLayout signInLayout, signOutLayout, dataLayout;
    private LinearLayout proxLayout, packageLayout, confirmLayout;
    private RadioGroup deliveryType;
    private String code = "null";

    private static final String TAG = "SignInActivity";
    private static final int RC_SIGN_IN = 9001;

    private FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
    private DatabaseReference mRootReference = firebaseDatabase.getReference();
    private DatabaseReference deliveryRequest = mRootReference.child("users");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.v("testing", "beginning");
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        netid = null;
        statusText = (TextView) findViewById(R.id.statusText);
        netidText = (TextView) findViewById(R.id.netidText);
        locaitonText = (TextView) findViewById(R.id.gpsText);
        idNumText = (TextView) findViewById(R.id.idNumText);
        buildingText = (TextView) findViewById(R.id.buildingText);
        roomNumText = (TextView) findViewById(R.id.roomNumText);
        deliveryText = (TextView) findViewById(R.id.deliveryIdText);
        droneStatusText = (TextView) findViewById(R.id.droneStatusText);
        signInLayout = (LinearLayout) findViewById(R.id.signInLayout);
        signOutLayout = (LinearLayout) findViewById(R.id.signOutLayout);
        dataLayout = (LinearLayout) findViewById(R.id.dataLayout);
        proxLayout = (LinearLayout) findViewById(R.id.proxLayout);
        packageLayout = (LinearLayout) findViewById(R.id.packageLayout);
        confirmLayout = (LinearLayout) findViewById(R.id.confirmDelivLayout);
        deliveryType = (RadioGroup) findViewById(R.id.deliveryType);
        proxBtn = (RadioButton) findViewById(R.id.proxBtn);
        proxBtn.setOnClickListener(this);
        packageBtn = (RadioButton) findViewById(R.id.packageBtn);
        packageBtn.setOnClickListener(this);
        requestBtn = (Button) findViewById(R.id.requestBtn);
        requestBtn.setOnClickListener(this);
        receivedBtn = (Button) findViewById(R.id.receivedBtn);
        receivedBtn.setOnClickListener(this);
        signInButton = (SignInButton) findViewById(R.id.signIn);
        signInButton.setOnClickListener(this);

        signOutButton = (Button) findViewById(R.id.signOut);
        signOutButton.setOnClickListener(this);

    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.signIn:
                signIn();
                break;
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
        }
    }

    private void received() {
        if(!deliveryText.getText().toString().equals(code)) {
            Toast.makeText(this, "Incorrect Delivery ID. Try Again.", Toast.LENGTH_LONG).show();
            return;
        }
        locaitonText.setText("");
        deliveryText.setText("");

        String identity;
        if(proxLayout.getVisibility() == View.VISIBLE && proxLayout.isShown()) {
            identity = buildingText.getText().toString() + " " + roomNumText.getText().toString();
            //deliveryRequest.child(netid).setValue("0000, netid: " + netid + ", identity: " + roomAddress + ", " + "location: " + pickUpLoc);
        }
        else {
            identity = idNumText.getText().toString();
            //deliveryRequest.child(netid).setValue("0000, netid: " + netid + ", identity: " + proxid + ", location: " + pickUpLoc);
        }
        createRequestNode(netid, "0000", identity, pickUpLoc);
        droneStatusText.setText("Status: Drone Returning To Sender");
    }

    private void signIn() {
        Log.d(TAG, "sign in clicked!");
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "requestCode " + requestCode);

        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    private void handleSignInResult(GoogleSignInResult result) {
        Log.d(TAG, "handleSignInResult: " + result.isSuccess());
        if (result.isSuccess()) {
            GoogleSignInAccount account = result.getSignInAccount();
            netid = account.getEmail();

            if (netid.contains("@"))
                netid = netid.substring(0, netid.indexOf('@'));
            statusText.setText("Hello, " + account.getDisplayName());
            netidText.setText("Netid: " + netid);


            signInLayout.setVisibility(View.GONE);
            netidText.setVisibility(View.VISIBLE);
            signOutLayout.setVisibility(View.VISIBLE);
            deliveryType.setVisibility(View.VISIBLE);
            dataLayout.setVisibility(View.VISIBLE);
            if(proxBtn.isChecked()) {
                proxLayout.setVisibility(View.VISIBLE);
                packageLayout.setVisibility(View.GONE);
            }
            else {
                proxLayout.setVisibility(View.GONE);
                packageLayout.setVisibility(View.VISIBLE);
            }

        } else {
            Log.d(TAG, "Failed to login");
        }
    }

    private void signOut() {
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status) {
                if (status.isSuccess()) {
                    statusText.setText("Sign into your Princeton email below.");
                    netidText.setText(" ");
                    signInLayout.setVisibility(View.VISIBLE);
                    netidText.setVisibility(View.GONE);
                    signOutLayout.setVisibility(View.GONE);
                    deliveryType.setVisibility(View.GONE);
                    dataLayout.setVisibility(View.GONE);
                    proxLayout.setVisibility(View.GONE);
                    packageLayout.setVisibility(View.GONE);
                    confirmLayout.setVisibility(View.GONE);
                    locaitonText.setText(" ");
                } else {
                    statusText.setText("Failed to Sign Out.");
                    netidText.setText(" ");
                }
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void droneRequest() {
        Log.d(TAG, "request made");
        locaitonText.setText("Getting location...");
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            pickUpLoc = getPickUpLocation(location);
                            locaitonText.setText("Pick up location: " + pickUpLoc);
                            droneStatusText.setText("Status: Request Not Seen Yet");
                            // ADD IN THE OTHER INFO TO BE SENT TO THE COMMAND APP

                            String identity;
                            if(proxLayout.getVisibility() == View.VISIBLE && proxLayout.isShown()) {
                                identity = buildingText.getText().toString() + " " + roomNumText.getText().toString();
                                //deliveryRequest.child(netid).setValue("netid: " + netid + ", identity: " + roomAddress + ", " + "location: " + pickUpLoc);
                            }
                            else {
                                identity = idNumText.getText().toString();
                                //deliveryRequest.child(netid).setValue("netid: " + netid + ", identity: " + proxid + ", location: " + pickUpLoc);
                            }
                            createRequestNode(netid, "null", identity, pickUpLoc);
                        }
                    }
                });
    }

    private void createRequestNode(String netid, String code, String identity, String location) {
        netid = netid.replaceAll("\\.","");
        deliveryRequest.child(netid).child("code").setValue(code);
        deliveryRequest.child(netid).child("netid").setValue(netid);
        deliveryRequest.child(netid).child("identity").setValue(identity);
        deliveryRequest.child(netid).child("location").setValue(location);
    }

    private String getPickUpLocation(Location location) {
        double lat = location.getLatitude();
        double lon = location.getLongitude();
        String dropPoint = "Location not found.";
        double minDist = Double.MAX_VALUE;
        double currDist;

        for(String[] loc : pickUpSpots)
        {
            currDist = dist(lat, lon, Double.parseDouble(loc[1]), Double.parseDouble(loc[2]));
            if(currDist < minDist) {
                minDist = currDist;
                dropPoint = loc[0];
            }
        }

        return dropPoint;
    }

    private double dist(double x1, double y1, double x2, double y2) {
        return Math.sqrt( (x1-x2)*(x1-x2) + (y1-y2)*(y1-y2) );
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
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "connection failed");
    }

    @Override
    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
        // update student about drone status - only their netid should be monitored
        HashMap<String, HashMap<String, String>> users;

        if(dataSnapshot.getKey() != null)
        {
            users = (HashMap<String, HashMap<String, String>>) dataSnapshot.getValue();
            if(users == null)
                return;
            for (Map.Entry<String, HashMap<String, String>> entry : users.entrySet()) {
                HashMap<String, String> user = entry.getValue();

                if(user != null && user.containsKey("netid") && user.containsKey("code") && user.containsKey("identity") && user.containsKey("location")) {
                    Log.d("DATA_STUFF", "onDataChange B4: " + user);
                    if (user.get("netid").equals(netid) && user.get("code").equals("0000")) {
                        Log.d("DATA_STUFF", "onDataChange: " + user);
                        droneStatusText.setText("Status: Drone Returning To Sender");
                        confirmLayout.setVisibility(View.GONE);
                        return;
                    } else if (user.get("netid").equals(netid) && !user.get("code").equals("null")) {
                        Log.d("DATA_STUFF", "onDataChange: " + user);
                        code = user.get("code");
                        droneStatusText.setText("Status: Acknowledged & Sending Drone");
                        confirmLayout.setVisibility(View.VISIBLE);
                    }
                }
            }

        }





        //for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

            /*if(data.get("netid").equals(netid) && data.get("code").equals("0000")) {
                droneStatusText.setText("Status: Drone Returning To Sender");
                confirmLayout.setVisibility(View.GONE);
                return;
            }
            else if(data.get("netid").equals(netid) && !data.get("code").equals("null")) {
                droneStatusText.setText("Status: Acknowledged & Sending Drone");
                confirmLayout.setVisibility(View.VISIBLE);
            }*/

            /*
            if(android.text.TextUtils.isDigitsOnly(data.substring(0,4)) && data.substring(13,data.indexOf(",", 5)).equals(netid)) {
                code = data.substring(0,4);
                if(code.equals("0000")) {
                    droneStatusText.setText("Status: Drone Returning To Sender");
                    confirmLayout.setVisibility(View.GONE);
                    return;
                }
                droneStatusText.setText("Status: Acknowledged & Sending Drone");
                confirmLayout.setVisibility(View.VISIBLE);

            }
            else if(data.substring(7,data.indexOf(",", 5)).equals(netid)) {
                droneStatusText.setText("Status: Request Not Seen Yet");
            }*/
        //}
    }

    @Override
    public void onCancelled(@NonNull DatabaseError databaseError) {

    }


    protected void onStart() {
        super.onStart();
        deliveryRequest.addValueEventListener(this);
    }
}