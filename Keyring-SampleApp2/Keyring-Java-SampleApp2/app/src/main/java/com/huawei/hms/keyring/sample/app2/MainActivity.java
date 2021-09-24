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

package com.huawei.hms.keyring.sample.app2;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.huawei.hms.support.api.keyring.credential.AndroidAppIdentity;
import com.huawei.hms.support.api.keyring.credential.AppIdentity;
import com.huawei.hms.support.api.keyring.credential.Credential;
import com.huawei.hms.support.api.keyring.credential.CredentialClient;
import com.huawei.hms.support.api.keyring.credential.CredentialManager;
import com.huawei.hms.support.api.keyring.credential.CredentialCallback;

/**
 * Keyring Demo MainActivity
 *
 * @author Huawei HMS
 * @since 2021-07-12
 */
public class MainActivity extends AppCompatActivity {
    private CredentialAdapter mCredentialAdapter;
    private RecyclerView mCredentialList;
    private Credential mChooseCredential;
    private CredentialClient mCredentialClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Spanned title = Html.fromHtml("<font color=\"black\">" + getString(R.string.app_name) + "</font>",
                Html.FROM_HTML_MODE_LEGACY);
        getSupportActionBar().setTitle(title);

        mCredentialClient = CredentialManager.getCredentialClient(this);

        mCredentialList = findViewById(R.id.credential_list);
        mCredentialList.setLayoutManager(new LinearLayoutManager(this));
        DividerItemDecoration divider = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        mCredentialList.addItemDecoration(divider);

        Button loginButton = findViewById(R.id.button_login);
        loginButton.setOnClickListener(this::login);

        queryCredential();
    }

    private void uncheckAllChooseButton(CredentialHolder exclude) {
        if (mCredentialAdapter == null) {
            return;
        }

        int count = mCredentialAdapter.getItemCount();
        for (int i = 0 ; i < count; i++) {
            CredentialHolder holder = (CredentialHolder) mCredentialList.findViewHolderForAdapterPosition(i);
            if (holder == null) {
                continue;
            }

            if (holder != exclude) {
                holder.uncheckChooseButton();
            }
        }
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

    private class CredentialHolder extends RecyclerView.ViewHolder {
        private final TextView mAccount;
        private final TextView mHint;
        private final RadioButton mChoose;
        private Credential mCredential;

        public CredentialHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.credential_item, parent, false));

            mAccount = itemView.findViewById(R.id.text_account);
            mHint = itemView.findViewById(R.id.text_hint);
            mChoose = itemView.findViewById(R.id.radio_choose);
            mChoose.setOnClickListener(this::choose);
        }

        public void uncheckChooseButton() {
            mChoose.setChecked(false);
        }

        private void choose(View v) {
            uncheckAllChooseButton(this);
            mChooseCredential = mCredential;
        }

        private void bind(Credential credential) {
            mCredential = credential;

            String usernameHint = String.format(getResources().getString(R.string.hint_account),
                    mCredential.getUsername());
            mAccount.setText(usernameHint);

            AndroidAppIdentity owner = (AndroidAppIdentity) mCredential.getOwner();
            String ownerPackageName = owner.getPackageName();
            int lastSepPos = ownerPackageName.lastIndexOf(".");
            String appName = ownerPackageName;
            if (lastSepPos > 0) {
                appName = ownerPackageName.substring(lastSepPos + 1);
            }
            String loginHint = String.format(getResources().getString(R.string.login_hint),
                    appName);
            mHint.setText(loginHint);
        }
    }

    private class CredentialAdapter extends RecyclerView.Adapter<CredentialHolder> {
        private final List<Credential> mCredentialList;

        CredentialAdapter(List<Credential> credentialList) {
            mCredentialList = credentialList;
        }

        @Override
        public CredentialHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(MainActivity.this);
            return new CredentialHolder(layoutInflater, parent);
        }

        @Override
        public void onBindViewHolder(@NonNull MainActivity.CredentialHolder holder, int position) {
            holder.bind(mCredentialList.get(position));
        }

        @Override
        public int getItemCount() {
            return mCredentialList.size();
        }
    }

    private void login(View v) {
        if (mChooseCredential == null) {
            showMessage(R.string.please_choose_account);
            return;
        }
        mChooseCredential.getContent(new CredentialCallback<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                // String password = new String(bytes);
                showMessage(R.string.get_password_ok);
            }

            @Override
            public void onFailure(long l, CharSequence charSequence) {
                showMessage(R.string.get_password_failed);
            }
        });
    }

    private void setCredentialList(List<Credential> credentialList) {
        mCredentialAdapter = new CredentialAdapter(credentialList);
        mCredentialList.setAdapter(mCredentialAdapter);
    }

    private void queryCredential() {
        final AndroidAppIdentity app1 = new AndroidAppIdentity("app1",
                "com.huawei.hms.keyring.sample.app1",
                "XX:XX:XX:XX:XX:XX"
        );
        List<AppIdentity> trustedOwnerList = new ArrayList<>();
        trustedOwnerList.add(app1);

        mCredentialClient.findCredential(trustedOwnerList, new CredentialCallback<List<Credential>>() {
            @Override
            public void onSuccess(List<Credential> credentials) {
                if (credentials.isEmpty()) {
                    noAvailableCredential();
                } else {
                    MainActivity.this.setCredentialList(credentials);
                }
            }

            @Override
            public void onFailure(long errorCode, CharSequence description) {
                noAvailableCredential();
            }

            private void noAvailableCredential() {
                showMessage(R.string.no_shared_credential);
            }
        });
    }
}
