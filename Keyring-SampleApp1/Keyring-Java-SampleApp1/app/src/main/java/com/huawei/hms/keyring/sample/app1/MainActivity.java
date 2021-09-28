/*
 * Copyright 2021. Huawei Technologies Co., Ltd. All rights reserved.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package com.huawei.hms.keyring.sample.app1;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.google.android.material.snackbar.Snackbar;
import com.huawei.hms.support.api.keyring.credential.AndroidAppIdentity;
import com.huawei.hms.support.api.keyring.credential.AppIdentity;
import com.huawei.hms.support.api.keyring.credential.CredentialType;
import com.huawei.hms.support.api.keyring.credential.Credential;
import com.huawei.hms.support.api.keyring.credential.CredentialCallback;
import com.huawei.hms.support.api.keyring.credential.CredentialClient;
import com.huawei.hms.support.api.keyring.credential.CredentialManager;

/**
 * Keyring Demo MainActivity
 *
 * @author Huawei HMS
 * @since 2021-07-12
 */
public class MainActivity extends AppCompatActivity {
    private EditText mUsername;
    private EditText mPassword;
    private boolean mShowPassword = false;
    private CredentialClient mCredentialClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Spanned title = Html.fromHtml("<font color=\"black\">" + getString(R.string.app_name) + "</font>",
                Html.FROM_HTML_MODE_LEGACY);
        getSupportActionBar().setTitle(title);

        mCredentialClient = CredentialManager.getCredentialClient(this);

        mUsername = findViewById(R.id.edit_username);
        mPassword = findViewById(R.id.edit_password);

        ImageView showPassword = findViewById(R.id.show_password);
        showPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mShowPassword = !mShowPassword;
                if (mShowPassword) {
                    mPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                } else {
                    mPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                }
                mPassword.setSelection(mPassword.getText().length());
            }
        });

        Button loginButton = findViewById(R.id.button_login);
        loginButton.setOnClickListener(this::login);

        Button deleteButton = findViewById(R.id.button_reset);
        deleteButton.setOnClickListener(this::delete);
    }

    private void showMessage(int resId) {
        showMessage(getString(resId));
    }

    private void showMessage(String message) {
        SpannableString spannedMsg = new SpannableString(message);
        Snackbar hint = Snackbar.make(getWindow().getDecorView(), spannedMsg, Snackbar.LENGTH_SHORT);
        hint.getView().setBackgroundResource(R.drawable.snackbar);
        hint.getView().setTranslationY(-120);

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) hint.getView().getLayoutParams();
        params.gravity = Gravity.BOTTOM | Gravity.CENTER;
        params.width = ViewGroup.LayoutParams.WRAP_CONTENT;

        hint.show();
    }

    private boolean checkInput() {
        String username = mUsername.getText().toString().trim();
        String password = mPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showMessage(R.string.invalid_input);
            return false;
        } else {
            return true;
        }
    }

    private void login(View view) {
        if (!checkInput()) {
            return;
        }
        String username = mUsername.getText().toString().trim();
        String password = mPassword.getText().toString().trim();

        // connect server to login.


        saveCredential(username, password,
                "app2", "com.huawei.hms.keyring.sample.app2",
                "XX:XX:XX:XX:XX:XX",
                true);
    }

    private void saveCredential(String username, String password,
                                String sharedToAppName, String sharedToAppPackage,
                                String sharedToAppCertHash, boolean userAuth) {
        AndroidAppIdentity app2 = new AndroidAppIdentity(sharedToAppName,
                sharedToAppPackage, sharedToAppCertHash);
        List<AppIdentity> sharedAppList = new ArrayList<>();
        sharedAppList.add(app2);

        Credential credential = new Credential(username,
                CredentialType.PASSWORD, userAuth,
                password.getBytes());
        credential.setDisplayName("nickname_" + username);
        credential.setSharedWith(sharedAppList);
        credential.setSyncable(true);

        mCredentialClient.saveCredential(credential, new CredentialCallback<Void>() {
            @Override
            public void onSuccess(Void unused) {
                showMessage(R.string.save_credential_ok);
            }

            @Override
            public void onFailure(long errorCode, CharSequence description) {
                showMessage(R.string.save_credential_failed + " " + errorCode + ":" + description);
            }
        });
    }

    private void deleteCredential(Credential credential) {
        mCredentialClient.deleteCredential(credential, new CredentialCallback<Void>() {
            @Override
            public void onSuccess(Void unused) {
                String hint = String.format(getResources().getString(R.string.delete_ok),
                        credential.getUsername());
                showMessage(hint);
            }

            @Override
            public void onFailure(long errorCode, CharSequence description) {
                String hint = String.format(getResources().getString(R.string.delete_failed),
                        description);
                showMessage(hint);
            }
        });
    }

    private void delete(View view) {
        String username = mUsername.getText().toString().trim();
        if (username.isEmpty()) {
            return;
        }

        List<AppIdentity> trustedAppList = new ArrayList<>();
        mCredentialClient.findCredential(trustedAppList, new CredentialCallback<List<Credential>>() {
            @Override
            public void onSuccess(List<Credential> credentials) {
                if (credentials.isEmpty()) {
                    showMessage(R.string.no_available_credential);
                } else {
                    for (Credential credential : credentials) {
                        if (credential.getUsername().equals(username)) {
                            deleteCredential(credential);
                            break;
                        }
                    }
                }
            }

            @Override
            public void onFailure(long errorCode, CharSequence description) {
                showMessage(R.string.query_credential_failed);
            }
        });
    }
}
