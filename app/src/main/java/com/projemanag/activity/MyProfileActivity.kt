package com.projemanag.activity

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.MimeTypeFilter
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.projemanag.R
import com.projemanag.firebase.FirestoreClass
import com.projemanag.model.User
import com.projemanag.utils.Constants
import com.projemanag.utils.Constants.PICK_IMAGE_REQUEST_CODE
import com.projemanag.utils.Constants.READ_STORAGE_PERMISSION_CODE
import com.projemanag.utils.Constants.getFileExtension
import com.projemanag.utils.Constants.showImageChooser
import kotlinx.android.synthetic.main.activity_my_profile.btn_update
import kotlinx.android.synthetic.main.activity_my_profile.et_email
import kotlinx.android.synthetic.main.activity_my_profile.et_mobile
import kotlinx.android.synthetic.main.activity_my_profile.et_name
import kotlinx.android.synthetic.main.activity_my_profile.iv_profile_user_image
import kotlinx.android.synthetic.main.activity_my_profile.toolbar_my_profile_activity
import java.io.IOException


class MyProfileActivity : BaseActivity() {



    private var mSelectedImageFileUri: Uri? = null
    private lateinit var mUserDetails: User
    private var mProfileImageURL: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_profile)
        setupActionBar()

        FirestoreClass().signInUser(this@MyProfileActivity)

        iv_profile_user_image.setOnClickListener{
            iv_profile_user_image.setOnClickListener{
                if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED){
                    showImageChooser(this)
                }else {
                    // Requests permissions to be granted to this application at runtime
                   ActivityCompat.requestPermissions(
                       this,
                       arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                       READ_STORAGE_PERMISSION_CODE
                   )
                }
             }
            btn_update.setOnClickListener {
                if (mSelectedImageFileUri != null) {
                    uploadUserImage()
                }else{
                    showProgressDialog(resources.getString(R.string.please_wait))
                    updateUserProfileData()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == READ_STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showImageChooser(this)
            } else {
                Toast.makeText(
                    this,
                    "Oops, you just denied the permission for storage. You can also allow it from settings.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == PICK_IMAGE_REQUEST_CODE) {
            if(resultCode == RESULT_OK) {
                if(data!!.data != null) {
                    mSelectedImageFileUri = data.data
                    try {
                        Glide
                            .with(this@MyProfileActivity)
                            .load(mSelectedImageFileUri)
                            .centerCrop()
                            .placeholder(R.drawable.ic_user_place_holder)
                            .into(iv_profile_user_image)
                    }catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }


    private fun setupActionBar() {

        setSupportActionBar(toolbar_my_profile_activity)

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_black_color_white_24dp)
            actionBar.title = resources.getString(R.string.my_profile)
        }

        toolbar_my_profile_activity.setNavigationOnClickListener { onBackPressed() }
    }


    fun setUserDataInUI(user: User) {

        mUserDetails = user

        Glide
            .with(this@MyProfileActivity)
            .load(user.image)
            .centerCrop()
            .placeholder(R.drawable.ic_user_place_holder)
            .into(iv_profile_user_image)

        et_name.setText(user.name)
        et_email.setText(user.email)
        if (user.mobile != 0L) {
           et_mobile.setText(user.mobile.toString())
        }
    }

    private fun updateUserProfileData() {
        val userHashMap = HashMap<String, Any>()


        if(mProfileImageURL.isNotEmpty() && mProfileImageURL != mUserDetails.image) {
            userHashMap[Constants.IMAGE] = mProfileImageURL
        }

        if(et_name.text.toString() != mUserDetails.name) {
            userHashMap[Constants.NAME] = et_name.text.toString()
        }

        if(et_mobile.text.toString() != mUserDetails.mobile.toString()) {
            userHashMap[Constants.MOBILE] = et_mobile.text.toString().toLong()
        }

        FirestoreClass().updateUserProfileData(this, userHashMap)
    }

    private fun uploadUserImage() {
        showProgressDialog(resources.getString(R.string.please_wait))

        if(mSelectedImageFileUri != null) {
            val sRef : StorageReference = FirebaseStorage.getInstance().reference.child(
                "USER_IMAGE" + System.currentTimeMillis() + "." + getFileExtension(this, mSelectedImageFileUri))
            sRef.putFile(mSelectedImageFileUri!!).addOnSuccessListener {
                taskSnapshot ->

                taskSnapshot.metadata!!.reference!!.downloadUrl.addOnSuccessListener {
                    uri ->
                        mProfileImageURL = uri.toString()
                        updateUserProfileData()
                    hideProgressDialog()
                    Toast.makeText(this@MyProfileActivity,
                        "Image uploaded successfully",
                        Toast.LENGTH_LONG).show()

                }
            }.addOnFailureListener {
                exception ->
                Toast.makeText(this@MyProfileActivity,
                    exception.message,
                    Toast.LENGTH_LONG).show()
                hideProgressDialog()
            }
        }
    }


    fun profileUpdateSuccess() {
        hideProgressDialog()
        setResult(Activity.RESULT_OK)
        finish()
    }

}