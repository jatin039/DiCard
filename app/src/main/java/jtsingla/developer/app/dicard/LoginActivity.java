package jtsingla.developer.app.dicard;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.support.annotation.BoolRes;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.app.LoaderManager.LoaderCallbacks;

import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v7.widget.LinearLayoutCompat;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;

import static android.Manifest.permission.READ_CONTACTS;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity implements LoaderCallbacks<Cursor> {

    /**
     * Id to identity READ_CONTACTS permission request.
     */
    private static final int REQUEST_READ_CONTACTS = 0;

    /**
     * A dummy authentication store containing known user names and passwords.
     * TODO: remove after connecting to a real authentication system.
     */
    private static final String[] DUMMY_CREDENTIALS = new String[]{
            "foo@example.com:hello", "bar@example.com:world"
    };
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private EditText mFirstNameView, mLastNameView, mPhoneNumberView;
    private OnClickListener attemptRegisterListener, attemptLoginListener;
    private int ERR_INVALID = 101;
    private int ERR_EMPTY = 102;
    private int ERR_VALID = 100;
    private int RC_SIGN_IN = 300;
    private static CallbackManager callbackManager;
    private static GoogleApiClient mGoogleApiClient;
    private static TextView mStatusTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*
        Instantiate sign in via Facebook.
        */
        callbackManager = FacebookLogin.FacebookLogin(getApplicationContext());

        /*
        Instantiate Sign in via Google.
         */
        mGoogleApiClient = GoogleLogin.GoogleLogin(this, RC_SIGN_IN);

        setContentView(R.layout.activity_login);
        AppEventsLogger.activateApp(this);

        // Set up the login form.
        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);
        populateAutoComplete();

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptRegister();
                    return true;
                }
                return false;
            }
        });

        mFirstNameView = (EditText) findViewById(R.id.first_name);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        mLastNameView = (EditText) findViewById(R.id.last_name);
        mPhoneNumberView = (EditText) findViewById(R.id.phone_number);

        attemptRegisterListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptRegister();
            }
        };

        attemptLoginListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptLogin();
            }
        };

        Button mEmailRegisterButton = (Button) findViewById(R.id.email_register_button);
        mEmailRegisterButton.setOnClickListener(attemptRegisterListener);

        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(attemptLoginListener);

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private void handleSignInResult(GoogleSignInResult result) {
        Log.d("sign_in", "handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();
            mStatusTextView.setText(getString(R.string.signed_in_fmt, acct.getDisplayName()));
            updateUI(true);
        } else {
            // Signed out, show unauthenticated UI.
            updateUI(false);
        }
    }

    private void updateUI(boolean signedIn) {
        if (signedIn) {
            findViewById(R.id.sign_in_button).setVisibility(View.GONE);
            findViewById(R.id.sign_out_button).setVisibility(View.VISIBLE);
        } else {
            mStatusTextView.setText(R.string.signed_out);

            findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
            findViewById(R.id.sign_out_button).setVisibility(View.GONE);
        }
    }

    private void populateAutoComplete() {
        if (!mayRequestContacts()) {
            return;
        }

        getLoaderManager().initLoader(0, null, this);
    }

    private boolean mayRequestContacts() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
            Snackbar.make(mEmailView, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        @TargetApi(Build.VERSION_CODES.M)
                        public void onClick(View v) {
                            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
                        }
                    });
        } else {
            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
        }
        return false;
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                populateAutoComplete();
            }
        }
    }

    private void attemptLogin() {
        Log.e("error", "entered attempt Login");
        setViewForLogin();
    }

    private void setViewForLogin() {
        //set visibility of not required fields as gone.
        EditText editText = (EditText) findViewById(R.id.first_name);
        editText.setVisibility(View.GONE);
        editText = (EditText) findViewById(R.id.last_name);
        editText.setVisibility(View.GONE);
        editText = (EditText) findViewById(R.id.phone_number);
        editText.setVisibility(View.GONE);
        Button button = (Button) findViewById(R.id.email_sign_in_button);
        button.setText(R.string.action_register);
        button.setOnClickListener(attemptRegisterListener);
        button = (Button) findViewById(R.id.email_register_button);
        button.setText(R.string.sign_in_button);
        button.setOnClickListener(attemptLoginListener);
    }

    private void setViewForRegistration() {
        EditText editText = (EditText) findViewById(R.id.first_name);
        editText.setVisibility(View.VISIBLE);
        editText = (EditText) findViewById(R.id.last_name);
        editText.setVisibility(View.VISIBLE);
        editText = (EditText) findViewById(R.id.phone_number);
        editText.setVisibility(View.VISIBLE);
        Button button = (Button) findViewById(R.id.email_sign_in_button);
        button.setText(R.string.action_sign_in);
        button.setOnClickListener(attemptLoginListener);
        button = (Button) findViewById(R.id.email_register_button);
        button.setText(R.string.action_register);
        button.setOnClickListener(attemptRegisterListener);
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptRegister() {
        Log.e("error", "entered attempt Register");
        setViewForRegistration();

        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);
        mPhoneNumberView.setError(null);
        mFirstNameView.setError(null);
        mLastNameView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();
        String firstName = mFirstNameView.getText().toString();
        String lastName = mLastNameView.getText().toString();
        String phoneNumber = mPhoneNumberView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        int check_password = checkPassword(password);
        if (check_password == ERR_INVALID) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
            focusView.requestFocus();
        } else if (check_password == ERR_EMPTY) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
            focusView.requestFocus();
        }

        int check_email = checkEmail(email);
        if (check_email == ERR_INVALID) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
            focusView.requestFocus();
        } else if (check_email == ERR_EMPTY) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
            focusView.requestFocus();
        }

        int check_first_name = checkFirstName(firstName);
        if (check_first_name == ERR_INVALID) {
            mFirstNameView.setError(getString(R.string.error_invalid_field));
            focusView = mFirstNameView;
            cancel = true;
            focusView.requestFocus();
        } else if (check_first_name == ERR_EMPTY) {
            mFirstNameView.setError(getString(R.string.error_field_required));
            focusView = mFirstNameView;
            cancel = true;
            focusView.requestFocus();
        }

        int check_last_name = checkLastName(lastName);
        if (check_last_name == ERR_INVALID) {
            mLastNameView.setError(getString(R.string.error_invalid_field));
            focusView = mLastNameView;
            cancel = true;
            focusView.requestFocus();
        } else if (check_last_name == ERR_EMPTY) {
            // Last name can be empty. Not a required field
        }

        int check_phone_number = checkPhoneNumber(phoneNumber);
        if (check_phone_number == ERR_INVALID) {
            mPhoneNumberView.setError(getString(R.string.error_invalid_number));
            focusView = mPhoneNumberView;
            cancel = true;
            focusView.requestFocus();
        } else if (check_phone_number == ERR_EMPTY) {
            mPhoneNumberView.setError(getString(R.string.error_field_required));
            focusView = mPhoneNumberView;
            cancel = true;
            focusView.requestFocus();
        }

        if (cancel == false) {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(email, password);
            mAuthTask.execute((Void) null);
        }
    }

    private int checkPassword(String password) {
        if (password.isEmpty()) {
            return ERR_EMPTY;
        }

        if (password.length() < 4) {
            return ERR_INVALID;
        }
        return ERR_VALID;
    }

    private int checkEmail(String email) {
        if (email.isEmpty()) {
            return ERR_EMPTY;
        }

        if (!email.contains("@")) {
            return ERR_INVALID;
        }
        return ERR_VALID;
    }

    private int checkFirstName(String name) {
        if (name.isEmpty()) {
            return ERR_EMPTY;
        }

        if (!(name.matches("[a-zA-Z]+"))) {
            return ERR_INVALID;
        }
        return ERR_VALID;
    }

    private int checkLastName(String name) {
        if (name.isEmpty()) {
            return ERR_EMPTY;
        }
        if (!(name.matches("[a-zA-Z]+"))) {
            return ERR_INVALID;
        }
        return ERR_VALID;
    }

    private int checkPhoneNumber(String number) {
        if (number.isEmpty()) {
            return ERR_EMPTY;
        }

        if (!(number.matches("[0-9]+"))) {
            return ERR_INVALID;
        }

        if (!(number.length() == 10)) {
            return ERR_INVALID;
        }
        return ERR_VALID;
    }
    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> emails = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }

        addEmailsToAutoComplete(emails);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(LoginActivity.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        mEmailView.setAdapter(adapter);
    }


    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        private final String mPassword;

        UserLoginTask(String email, String password) {
            mEmail = email;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.

            try {
                // Simulate network access.
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                return false;
            }

            for (String credential : DUMMY_CREDENTIALS) {
                String[] pieces = credential.split(":");
                if (pieces[0].equals(mEmail)) {
                    // Account exists, return true if the password matches.
                    return pieces[1].equals(mPassword);
                }
            }

            // TODO: register the new account here.
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success) {
                finish();
            } else {
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}

