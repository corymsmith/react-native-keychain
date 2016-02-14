package com.oblador.keychain;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.facebook.android.crypto.keychain.SharedPrefsBackedKeyChain;
import com.facebook.crypto.Crypto;
import com.facebook.crypto.Entity;
import com.facebook.crypto.exception.CryptoInitializationException;
import com.facebook.crypto.exception.KeyChainException;
import com.facebook.crypto.util.SystemNativeCryptoLibrary;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.Charset;

public class KeychainModule extends ReactContextBaseJavaModule {

    public static final String REACT_CLASS = "RNKeychainManager";
    public static final String KEYCHAIN_DATA = "RN_KEYCHAIN";

    private final Crypto crypto;
    private final SharedPreferences prefs;

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    public KeychainModule(ReactApplicationContext reactContext) {
        super(reactContext);
        crypto = new Crypto(
                new SharedPrefsBackedKeyChain(getReactApplicationContext()),
                new SystemNativeCryptoLibrary());
        prefs = this.getReactApplicationContext().getSharedPreferences(KEYCHAIN_DATA, Context.MODE_PRIVATE);
    }

    @ReactMethod
    public void setGenericPasswordForService(String service, String username, String password, Callback callback) {
        if (!crypto.isAvailable()) {
            Log.e("Crypto", "Crypto is missing");
        }

        Entity entity = new Entity(KEYCHAIN_DATA + ":" + service);

        try {
            String encryptedUsername = encryptWithEntity(username, entity);
            String encryptedPassword = encryptWithEntity(password, entity);

            SharedPreferences.Editor prefsEditor = prefs.edit();
            prefsEditor.putString(service + ":u", encryptedUsername);
            prefsEditor.putString(service + ":p", encryptedPassword);
            prefsEditor.apply();
            Log.e("Crypto Encrypted", encryptedUsername +":"+encryptedPassword);
            callback.invoke(null);
        } catch (Exception e) {
            callback.invoke(e);
        }
    }

    private String encryptWithEntity(String string, Entity entity) {
        try {
            byte[] encrypted = new byte[0];
            byte[] bytes = string.getBytes(Charset.forName("UTF-8"));
            return new String(crypto.encrypt(bytes, entity));
        } catch (Exception e) {
            //callback.invoke(e);
            return null;
        }
    }

    @ReactMethod
    public void getGenericPasswordForService(String service, Callback callback) {

        String username = prefs.getString(service + ":u", "");
        String password = prefs.getString(service + ":p", "");

        Entity entity = new Entity(KEYCHAIN_DATA + ":" + service + ":" + password);

        try {
            byte[] decryptedUsername = crypto.decrypt(username.getBytes(), entity);
            byte[] decryptedPass = crypto.decrypt(password.getBytes(), entity);
            Log.e("Crypto Decrypted u: ", new String(decryptedUsername));
            Log.e("Crypto Decrypted p: ", new String(decryptedPass));
            callback.invoke(null, new String(decryptedUsername), new String(decryptedPass));
        } catch (Exception e) {
            e.printStackTrace();
            callback.invoke(e);
        }
    }

    @ReactMethod
    public void resetGenericPasswordForService(String service) {
        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.remove(service + ":u");
        prefsEditor.remove(service + ":p");
        prefsEditor.apply();
    }


//    @ReactMethod
//    public void setInternetCredentialsForServer(String server, String username, String password, Callback callback) {
//
//    }
//
//    @ReactMethod
//    public void getInternetCredentialsForServer(String server, Callback callback) {
//
//    }
//
//    @ReactMethod
//    public void resetInternetCredentialsForServer(String server, Callback callback) {
//
//    }


}
