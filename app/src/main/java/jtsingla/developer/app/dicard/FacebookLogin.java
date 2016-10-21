package jtsingla.developer.app.dicard;

import android.content.Context;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

/**
 * Created by jssingla on 10/20/16.
 */

public class FacebookLogin {
    public static CallbackManager FacebookLogin(Context context) {
        FacebookSdk.sdkInitialize(context);
        CallbackManager callbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {

            }

            @Override
            public void onCancel() {

            }

            @Override
            public void onError(FacebookException error) {

            }
        });
        return callbackManager;
    }
}
