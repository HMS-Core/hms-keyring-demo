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
package com.huawei.hms.keyring.sample.app2

import android.os.Bundle
import android.text.Html
import android.text.SpannableString
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.huawei.hms.support.api.keyring.credential.*
import java.util.*

/**
 * Keyring Demo MainActivity
 *
 * @author Huawei HMS
 * @since 2021-07-12
 */
class MainActivity : AppCompatActivity() {
    private var mCredentialAdapter: CredentialAdapter? = null
    private var mCredentialList: RecyclerView? = null
    private var mChooseCredential: Credential? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val title = Html.fromHtml("<font color=\"black\">" + getString(R.string.app_name) + "</font>",
                Html.FROM_HTML_MODE_LEGACY)
        supportActionBar!!.setTitle(title)
        mCredentialList = findViewById(R.id.credential_list)
        mCredentialList?.setLayoutManager(LinearLayoutManager(this))
        val divider = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        mCredentialList?.addItemDecoration(divider)
        val loginButton = findViewById<Button>(R.id.button_login)
        loginButton.setOnClickListener { v: View -> login(v) }
        queryCredential()
    }

    private fun login(v: View) {
        if (mChooseCredential == null) {
            showMessage(R.string.please_choose_account)
            return
        }
        mChooseCredential!!.getContent(object : CredentialCallback<ByteArray?> {
            override fun onSuccess(bytes: ByteArray?) {
                // String password = new String(bytes);
                showMessage(R.string.get_password_ok)
            }

            override fun onFailure(l: Long, charSequence: CharSequence) {
                showMessage(R.string.get_password_failed)
            }
        })
    }

    private fun uncheckAllChooseButton(exclude: CredentialHolder) {
        if (mCredentialAdapter == null) {
            return
        }
        val count = mCredentialAdapter!!.itemCount
        for (i in 0 until count) {
            val holder = mCredentialList!!.findViewHolderForAdapterPosition(i) as CredentialHolder?
                    ?: continue
            if (holder !== exclude) {
                holder.uncheckChooseButton()
            }
        }
    }

    private fun showMessage(resId: Int) {
        showMessage(getString(resId))
    }

    private fun showMessage(message: String) {
        val spannedMsg = SpannableString(message)
        val hint = Snackbar.make(window.decorView, spannedMsg, Snackbar.LENGTH_SHORT)
        hint.view.setBackgroundResource(R.drawable.snackbar)
        hint.view.translationY = -120f
        val params = hint.view.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.BOTTOM or Gravity.CENTER
        params.width = ViewGroup.LayoutParams.WRAP_CONTENT
        hint.show()
    }

    private inner class CredentialHolder(inflater: LayoutInflater, parent: ViewGroup?) : RecyclerView.ViewHolder(inflater.inflate(R.layout.credential_item, parent, false)) {
        private val mAccount: TextView
        private val mHint: TextView
        private val mChoose: RadioButton
        private var mCredential: Credential? = null
        fun uncheckChooseButton() {
            mChoose.isChecked = false
        }

        private fun choose(v: View) {
            uncheckAllChooseButton(this)
            mChooseCredential = mCredential
        }

        fun bind(credential: Credential) {
            mCredential = credential
            val usernameHint = String.format(resources.getString(R.string.hint_account),
                    mCredential!!.getUsername())
            mAccount.text = usernameHint
            val owner = mCredential!!.getOwner() as AndroidAppIdentity
            val ownerPackageName = owner.getPackageName()
            val lastSepPos = ownerPackageName.lastIndexOf(".")
            var appName = ownerPackageName
            if (lastSepPos > 0) {
                appName = ownerPackageName.substring(lastSepPos + 1)
            }
            val loginHint = String.format(resources.getString(R.string.login_hint),
                    appName)
            mHint.text = loginHint
        }

        init {
            mAccount = itemView.findViewById(R.id.text_account)
            mHint = itemView.findViewById(R.id.text_hint)
            mChoose = itemView.findViewById(R.id.radio_choose)
            mChoose.setOnClickListener { v: View -> choose(v) }
        }
    }

    private inner class CredentialAdapter internal constructor(private val mCredentialList: List<Credential>) : RecyclerView.Adapter<CredentialHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CredentialHolder {
            val layoutInflater = LayoutInflater.from(this@MainActivity)
            return CredentialHolder(layoutInflater, parent)
        }

        override fun onBindViewHolder(holder: CredentialHolder, position: Int) {
            holder.bind(mCredentialList[position])
        }

        override fun getItemCount(): Int {
            return mCredentialList.size
        }
    }

    private fun setCredentialList(credentialList: List<Credential>) {
        mCredentialAdapter = CredentialAdapter(credentialList)
        mCredentialList!!.adapter = mCredentialAdapter
    }

    private fun queryCredential() {
        val app1 = AndroidAppIdentity("app1",
                "com.huawei.hms.keyring.sample.app1",
                "XX:XX:XX:XX:XX:XX"
        )
        val trustedOwnerList: MutableList<AppIdentity> = ArrayList()
        trustedOwnerList.add(app1)
        val credentialClient = CredentialManager.getCredentialClient(this)
        credentialClient.findCredential(trustedOwnerList, object : CredentialCallback<List<Credential>> {
            override fun onSuccess(credentials: List<Credential>) {
                if (credentials.isEmpty()) {
                    noAvailableCredential()
                } else {
                    setCredentialList(credentials)
                }
            }

            override fun onFailure(errorCode: Long, description: CharSequence) {
                noAvailableCredential()
            }

            private fun noAvailableCredential() {
                showMessage(R.string.no_shared_credential)
            }
        })
    }
}