package thesis.ogunlana.com.dronerequest;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

public class OpeningActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {

    private static final String TAG = "OPENING ACTIVITY";
    private static final int RC_SIGN_IN = 9001;

    private String netid;
    private GoogleApiClient mGoogleApiClient;
    private TextView statusText;
    private SignInButton signInButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.opening);

        statusText = (TextView) findViewById(R.id.statusText);
        signInButton = (SignInButton) findViewById(R.id.signIn);
        signInButton.setOnClickListener(this);

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        Bundle extras = getIntent().getExtras();
        if(extras != null && extras.getBoolean("close"))
        {
            // signOut();
        }
        else
        {

        }
        signInButton.setVisibility(View.VISIBLE);
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

            if (netid.contains("@")) {
                netid = netid.substring(0, netid.indexOf('@'));
            }
            //statusText.setText("Hello, " + account.getDisplayName());

            // START NEW ACTIVITY HERE, pass netid
            Intent myIntent = new Intent(OpeningActivity.this, MainActivity.class);
            myIntent.putExtra("netid", netid);
            myIntent.putExtra("displayname", account.getDisplayName());
            OpeningActivity.this.startActivity(myIntent);

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
                } else {
                    statusText.setText("Failed to Sign Out.");
                }
            }
        });
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "connection failed");
    }

    @Override
    public void onClick(View view) {

        switch (view.getId()) {
            case R.id.signIn:
                signIn();
                break;
            case R.id.signOut:
                signOut();
                break;
        }
    }
}
