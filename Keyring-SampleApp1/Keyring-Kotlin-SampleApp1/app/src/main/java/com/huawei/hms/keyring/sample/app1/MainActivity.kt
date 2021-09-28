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
package com.huawei.hms.keyring.sample.app1

import android.os.Bundle
import android.text.Html
import android.text.SpannableString
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
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
    private var mUsername: EditText? = null
    private var mPassword: EditText? = null
    private var mShowPassword = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val title = Html.fromHtml("<font color=\"black\">" + getString(R.string.app_name) + "</font>",
                Html.FROM_HTML_MODE_LEGACY)
        supportActionBar!!.setTitle(title)
        mUsername = findViewById(R.id.edit_username)
        mPassword = findViewById(R.id.edit_password)
        val showPassword = findViewById<ImageView>(R.id.show_password)
        showPassword.setOnClickListener {
            mShowPassword = !mShowPassword
            if (mShowPassword) {
                mPassword?.setTransformationMethod(HideReturnsTransformationMethod.getInstance())
            } else {
                mPassword?.setTransformationMethod(PasswordTransformationMethod.getInstance())
            }
            mPassword?.setSelection(mPassword?.getText()?.length!!)
        }
        val resetButton = findViewById<Button>(R.id.button_reset)
        resetButton.setOnClickListener { view: View -> delete(view) }
        val loginButton = findViewById<Button>(R.id.button_login)
        loginButton.setOnClickListener { view: View -> login(view) }
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

    private fun checkInput(): Boolean {
        val username = mUsername!!.text.toString().trim { it <= ' ' }
        val password = mPassword!!.text.toString().trim { it <= ' ' }
        return if (username.isEmpty() || password.isEmpty()) {
            showMessage(R.string.invalid_input)
            false
        } else {
            true
        }
    }

    private fun login(view: View) {
        if (!checkInput()) {
            return
        }
        val username = mUsername!!.text.toString().trim { it <= ' ' }
        val password = mPassword!!.text.toString().trim { it <= ' ' }
        saveCredential(username, password,
                "app2", "com.huawei.hms.keyring.sample.app2",
                "XX:XX:XX:XX:XX:XX",
                true)
    }

    private fun saveCredential(username: String, password: String,
                               sharedToAppName: String, sharedToAppPackage: String,
                               sharedToAppCertHash: String, userAuth: Boolean) {
        val app2 = AndroidAppIdentity(sharedToAppName,
                sharedToAppPackage, sharedToAppCertHash)
        val sharedAppList: MutableList<AppIdentity> = ArrayList()
        sharedAppList.add(app2)
        val credential = Credential(username, CredentialType.PASSWORD, userAuth, password.toByteArray())
        credential.setDisplayName("nickname_" + username)
        credential.setSharedWith(sharedAppList)
        credential.syncable = true
        val credentialClient = CredentialManager.getCredentialClient(this)
        credentialClient.saveCredential(credential, object : CredentialCallback<Void?> {
            override fun onSuccess(unused: Void?) {
                showMessage(R.string.save_credential_ok)
            }

            override fun onFailure(errorCode: Long, description: CharSequence) {
                showMessage(R.string.save_credential_failed.toString() + " " + errorCode + ":" + description)
            }
        })
    }

    private fun deleteCredential(credential: Credential) {
        val credentialClient = CredentialManager.getCredentialClient(this)
        credentialClient.deleteCredential(credential, object : CredentialCallback<Void?> {
            override fun onSuccess(unused: Void?) {
                val hint = String.format(resources.getString(R.string.delete_ok),
                        credential.getUsername())
                showMessage(hint)
            }

            override fun onFailure(errorCode: Long, description: CharSequence) {
                val hint = String.format(resources.getString(R.string.delete_failed),
                        description)
                showMessage(hint)
            }
        })
    }

    private fun delete(view: View) {
        val username = mUsername!!.text.toString().trim { it <= ' ' }
        if (username.isEmpty()) {
            return
        }
        val credentialClient = CredentialManager.getCredentialClient(this)
        val trustedAppList: List<AppIdentity> = ArrayList()
        credentialClient.findCredential(trustedAppList, object : CredentialCallback<List<Credential>> {
            override fun onSuccess(credentials: List<Credential>) {
                if (credentials.isEmpty()) {
                    showMessage(R.string.no_available_credential)
                } else {
                    for (credential in credentials) {
                        if (credential.getUsername() == username) {
                            deleteCredential(credential)
                            break
                        }
                    }
                }
            }

            override fun onFailure(errorCode: Long, description: CharSequence) {
                showMessage(R.string.query_credential_failed)
            }
        })
    }
}
