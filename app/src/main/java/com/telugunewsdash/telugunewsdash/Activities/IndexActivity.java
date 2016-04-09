package com.telugunewsdash.telugunewsdash.Activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.app.Application;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.facebook.CallbackManager;
import com.facebook.appevents.AppEventsLogger;
import com.telugunewsdash.telugunewsdash.AnalyticsApplication;
import com.telugunewsdash.telugunewsdash.R;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class IndexActivity extends AppCompatActivity {

    private CallbackManager callbackManager;
    private ProgressBar spinner;
    private String name;
    private TextView fbInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        name = "Index Activity";
        //Initialize the facebook sdk before doing anything else
        FacebookSdk.sdkInitialize(getApplicationContext());

        setContentView(R.layout.activity_index);

        final SharedPreferences sharedPrefLogin = getSharedPreferences(getString(R.string.sharedPrefFileKey), Context.MODE_PRIVATE);

        callbackManager = CallbackManager.Factory.create();

        spinner = (ProgressBar)findViewById(R.id.progressBar1);
        spinner.setVisibility(View.GONE);

        LoginButton loginButton = (LoginButton) findViewById(R.id.fb_login_button);
        loginButton.setReadPermissions(Arrays.asList("public_profile, email"));
        fbInfo = (TextView) findViewById(R.id.fb_textview);

        if (sharedPrefLogin.contains("LoginStatus")){
            Log.d("Log : ", "LoginStatus True");

            // Share URI Data
            Uri data = getIntent().getData();
            if(data != null){
//                Log.d("Log : ", "Path : " + data.getPath());
//                Log.d("Log : ", "Scheme : " + data.getScheme());
//                Log.d("Log : ", "Host : " + data.getHost());
//                Log.d("Log : ", "Path Segments : " + Arrays.toString(data.getPathSegments().toArray()));

                // Redirect to Home Page
                Intent homeIntent = new Intent(IndexActivity.this, MainActivity.class);
                homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                homeIntent.putExtra("URIData", data); // Pass DATA URI
                startActivity(homeIntent);
                finish(); // call this to finish the current activity. Preventing Back buttton.

            } else {
//                Log.d("Log : ", "No URI Data Found");

                // Redirect to Home Page
                Intent homeIntent = new Intent(IndexActivity.this, MainActivity.class);
                homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(homeIntent);
                finish(); // call this to finish the current activity. Preventing Back buttton.
            }



        } else {
            Log.d("Log : ", "Does not Contain LoginStatus");

            loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
                @Override
                public void onSuccess(LoginResult loginResult) {
                    //fbInfo.setText("Login Successful");
                    //FB login successful, checking the same on the server.
                    //ServerFbTask fb = new ServerFbTask();
                    //fb.execute(loginResult.getAccessToken().getUserId(), loginResult.getAccessToken().getToken());
                    fbInfo.setText("Login Success.");
                    Log.d("Facebook SDK Login : ", "Success");
                    AlertDialog.Builder dlgAlert1  = new AlertDialog.Builder(IndexActivity.this);
                    dlgAlert1.setMessage("Please wait....");
                    dlgAlert1.setTitle("Login Successful");
                    dlgAlert1.setCancelable(true);
                    dlgAlert1.create().show();
                    spinner.setVisibility(View.VISIBLE);

                    String fb_id = loginResult.getAccessToken().getUserId();
                    String fb_accesstoken = loginResult.getAccessToken().getToken();

                    // Add code to print out the key hash
                    try {
                        PackageInfo info = getPackageManager().getPackageInfo(
                                "com.facebook.samples.hellofacebook",
                                PackageManager.GET_SIGNATURES);
                        for (Signature signature : info.signatures) {
                            MessageDigest md = MessageDigest.getInstance("SHA");
                            md.update(signature.toByteArray());
                            Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
                        }
                    } catch (PackageManager.NameNotFoundException e) {

                    } catch (NoSuchAlgorithmException e) {

                    }
                    final TextView mTextView = (TextView) findViewById(R.id.text);

                    // Instantiate the RequestQueue.
                    RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
                    String url = "http://dharanimanne.com/api/FacebookLogins/login?fb_id="+fb_id+"&fb_accesstoken="+fb_accesstoken;

                    // Request a string response from the provided URL.
                    StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    // Display the first 500 characters of the response string.
                                    mTextView.setText("Response is: " + response.substring(0, 100));

                                    try {
                                        JSONObject fbObject = new JSONObject(response);
                                        Log.d("Server Response : ", fbObject.toString());

                                        JSONObject fbObjectData = (JSONObject) fbObject.get("data");

                                        // On Successful Response from Blaffer API.
                                        SharedPreferences.Editor editor = sharedPrefLogin.edit();
                                        editor.putString("LoginStatus", "True");
                                        editor.putString("name",    fbObjectData.getString("name"));
                                        editor.putString("email",       fbObjectData.getString("email"));
                                        editor.putString("picture",     fbObjectData.getString("picture"));
                                        editor.putString("gender",      fbObjectData.getString("gender"));
                                        editor.putString("accesstoken", fbObjectData.getString("access_token"));
                                        editor.putString("userId",      fbObjectData.getString("id"));
                                        editor.commit();


                                        // Redirect to Home Page
                                        Intent homeIntent = new Intent(IndexActivity.this, MainActivity.class);
                                        homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(homeIntent);
                                        finish(); // call this to finish the current activity. Preventing Back buttton.
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                        Log.e("Error", "Could not parse malformed JSON String: " + response );
                                    }
                                }
                            }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            mTextView.setText("That didn't work!");
                        }
                    });
                    // Add the request to the RequestQueue.
                    queue.add(stringRequest);
                }

                @Override
                public void onCancel() {
                    fbInfo.setText("Login Cancelled. Please try again.");
                    Log.d("Facebook Login : ", "Login Cancel");
                }

                @Override
                public void onError(FacebookException e) {
                    fbInfo.setText("Login Error : " + e.toString());
                    Log.d("Facebook Login : ", "Login Error");
                    AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(IndexActivity.this);
                    dlgAlert.setMessage("Please check your network connection");
                    dlgAlert.setTitle("Login Error");
                    dlgAlert.setPositiveButton("OK", null);
                    dlgAlert.setCancelable(true);
                    dlgAlert.create().show();
                }
            });

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
        spinner.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Logs 'install' and 'app activate' App Events.
        AppEventsLogger.activateApp(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Logs 'app deactivate' App Event.
        AppEventsLogger.deactivateApp(this);
    }
}
