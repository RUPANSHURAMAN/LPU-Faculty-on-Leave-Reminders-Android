package com.raman.myapplication4;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class LoginActivity extends AppCompatActivity implements View.OnKeyListener {

    SharedPreferences sharedPref;
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;
    // UI references.
    private EditText mRegistrationNumber;
    private EditText mPasswordView;
    private View mLoginFormView;
    private LinearLayout mProgressLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_login);
        // Set up the login form.
        TextView tvPrivacyPolicy = (TextView) findViewById(R.id.tvPrivacyPolicy);
        tvPrivacyPolicy.setMovementMethod(LinkMovementMethod.getInstance());
        mRegistrationNumber = (EditText) findViewById(R.id.registration_number);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String reg = sharedPref.getString(getString(R.string.key_registration_number), "a");
        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnKeyListener(this);
        if (!reg.equals("a")) {
            mRegistrationNumber.setText(reg);
            TextInputLayout textInputLayout = (TextInputLayout) findViewById(R.id.textInputRegistrationNumber);
            textInputLayout.setVisibility(View.GONE);
            tvPrivacyPolicy.setVisibility(View.GONE);
        }
        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressLayout = (LinearLayout) findViewById(R.id.progress_login_layout);
    }


    private void closeKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow((null == getCurrentFocus()) ? null : getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    @Override
    public boolean onKey(View view, int i, KeyEvent keyEvent) {
        if (i == KeyEvent.KEYCODE_ENTER && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
            attemptLogin();
        }
        return false;
    }

    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }
        closeKeyboard();

        // Reset errors.
        mRegistrationNumber.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String registrationNumber = mRegistrationNumber.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password) || isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(registrationNumber)) {
            mRegistrationNumber.setError(getString(R.string.error_field_required));
            focusView = mRegistrationNumber;
            cancel = true;
        } else if (!isRegistrationNumberValid(registrationNumber)) {
            mRegistrationNumber.setError(getString(R.string.error_invalid_registration_number));
            focusView = mRegistrationNumber;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(registrationNumber, password);
            mAuthTask.execute((Void) null);
        }
    }

    private boolean isRegistrationNumberValid(String registrationNumber) {
        return registrationNumber.matches("\\d+");
    }

    private boolean isPasswordValid(String password) {
        return password.length() < 5;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        mProgressLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private class UserLoginTask extends AsyncTask<Void, Void, Boolean> {
        private final String mRegistrationNumber;
        private final String mPassword;
        String topName = sharedPref.getString(getString(R.string.key_display_name), "User");
        private String absUrl;

        UserLoginTask(String registrationNumber, String password) {
            mRegistrationNumber = registrationNumber;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                // Simulate network access.
                Connection.Response response = Jsoup.connect(getString(R.string.ums_login_url))
                        .method(Connection.Method.GET)
                        .execute();
                String viewstate = response.parse().select("input[name=__VIEWSTATE]").first().attr("value");
                if (viewstate == null)
                    Log.d("LA", "view is NULL");
                if (response.statusCode() == 302 || response.parse().text().contains(getString(R.string.oops_ums_server_down_error)) || viewstate == null) {
                    Snackbar.make(findViewById(android.R.id.content), getString(R.string.ums_server_busy), Snackbar.LENGTH_LONG).show();
                    return false;
                }
                response =
                        Jsoup.connect(getString(R.string.ums_login_url))
                                .method(Connection.Method.POST)
                                .data(getString(R.string.__LASTFOCUS), getString(R.string.__LASTFOCUS_value))
                                .data(getString(R.string.__EVENTTARGET), getString(R.string.__EVENTTARGET_value))
                                .data(getString(R.string.__EVENTARGUMENT), getString(R.string.__EVENTARGUMENT_value))
                                .data(getString(R.string.__VIEWSTATE), viewstate)
                                .data(getString(R.string.__VIEWSTATEGENERATOR), getString(R.string.__VIEWSTATEGENERATOR_value))
                                .data(getString(R.string.__SCROLLPOSITIONX), getString(R.string.__SCROLLPOSITIONX_value))
                                .data(getString(R.string.__SCROLLPOSITIONY), getString(R.string.__SCROLLPOSITIONY_value))
                                .data(getString(R.string.__VIEWSTATEENCRYPTED), getString(R.string.__VIEWSTATEENCRYPTED_value))
                                .data(getString(R.string.__USERNAME), mRegistrationNumber)
                                .data(getString(R.string.__PASSWORD), mPassword)
                                .data(getString(R.string.__DropDownList1), getString(R.string.__DropDownList1_value))
                                .data(getString(R.string.__ddlStartWith), getString(R.string.__ddlStartWith_value))
                                .data(getString(R.string.__iBtnLogin_x), getString(R.string.__iBtnLogin_x_value))
                                .data(getString(R.string.__iBtnLogin_y), getString(R.string.__iBtnLogin_y_value))
                                .cookies(response.cookies())
                                .followRedirects(true)
                                .execute();
                if (response.parse().text().contains(getString(R.string.oops_ums_server_down_error))) {
                    Snackbar.make(findViewById(android.R.id.content), R.string.try_again, Snackbar.LENGTH_LONG).show();
                    return false;
                }
                if (response.parse().title().equals(getString(R.string.ums_landing_page_title))) {
                    if (topName.equals("User"))
                        topName = response.parse().getElementById(getString(R.string.ums_landing_page_photo_id)).text();
                    Element img = response.parse().select("img").first();
                    absUrl = img.absUrl("src").replaceAll(" ", "%20");
                    Log.d("LA", "abs url is" + absUrl);
                    Snackbar.make(findViewById(android.R.id.content), getString(R.string.success), Snackbar.LENGTH_LONG).show();
                    return true;
                }
                if (response.parse().title().equals(getString(R.string.ums_login_page_title))) {
                    Snackbar.make(findViewById(android.R.id.content), R.string.incorrect_registration_number_password_or_app_update_needed, Snackbar.LENGTH_LONG).show();
                    return false;
                }
                return true;
            } catch (SocketTimeoutException timeout) {
                Snackbar.make(findViewById(android.R.id.content), getString(R.string.ums_server_busy), Snackbar.LENGTH_LONG).show();
                timeout.printStackTrace();
                return false;
            } catch (UnknownHostException uhe) {
                Snackbar.make(findViewById(android.R.id.content), getString(R.string.no_internet_access), Snackbar.LENGTH_LONG).show();
                uhe.printStackTrace();
                return false;
            } catch (IOException e) {
                Snackbar.make(findViewById(android.R.id.content), getString(R.string.network_error), Snackbar.LENGTH_LONG).show();
                e.printStackTrace();
                return false;
            } catch (NullPointerException npe) {
                Snackbar.make(findViewById(android.R.id.content), getString(R.string.try_again), Snackbar.LENGTH_LONG).show();
                npe.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);
            if (success) {
                sharedPref.edit().putString(getString(R.string.key_registration_number), mRegistrationNumber)
                        .putString(getString(R.string.key_password), mPassword)
                        .putString(getString(R.string.key_display_name), topName)
                        .apply();//1 for sunday till sat=7
                Log.d("LA", "MA starting");
                startActivity(new Intent(LoginActivity.this, MainActivity.class).putExtra("absUrl", absUrl));
                finish();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }

}
