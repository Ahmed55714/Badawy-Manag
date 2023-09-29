package com.projemanag.activity

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import com.projemanag.R
import com.projemanag.firebase.FirestoreClass
import com.projemanag.model.Board
import com.projemanag.model.User
import com.projemanag.utils.Constants
import kotlinx.android.synthetic.main.activity_members.rv_members_list
import kotlinx.android.synthetic.main.activity_members.toolbar_members_activity
import kotlinx.android.synthetic.main.dialog_search_member.et_email_search_member
import kotlinx.android.synthetic.main.dialog_search_member.tv_add
import kotlinx.android.synthetic.main.dialog_search_member.tv_cancel
import org.json.JSONObject
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

class MembersActivity : BaseActivity() {

    private lateinit var boardDetails: Board
    private lateinit var assignedMembersList: ArrayList<User>
    private var anyChangesMade: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_members)

        if(intent.hasExtra(Constants.BOARD_DETAIL)){
            boardDetails = intent.getParcelableExtra<Board>(Constants.BOARD_DETAIL)!!
        }

        setupActionBar()
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().getAssignedMembersListDetails(this, boardDetails.assignedTo)
    }

    fun setupMembersList(list: ArrayList<User>){

        assignedMembersList = list
        hideProgressDialog()
        rv_members_list.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        rv_members_list.setHasFixedSize(true)
        val adapter = com.projemanag.adapters.MemberListItemsAdapter(this, list)
        rv_members_list.adapter = adapter
    }
    fun memberDetails(user: User){
        boardDetails.assignedTo.add(user.id)
        FirestoreClass().assignMemberToBoard(this, boardDetails, user)
    }


    private fun setupActionBar(){
        setSupportActionBar(toolbar_members_activity)
        val actionBar = supportActionBar
        if(actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_black_color_back_24dp)
            actionBar.title = resources.getString(R.string.members)
        }
        toolbar_members_activity.setNavigationOnClickListener{ onBackPressed()}
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_add_member, menu)
        return super.onCreateOptionsMenu(menu)

    }
   override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.action_add_members -> {
                dialogSearchMember()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
    private fun dialogSearchMember(){
        val dailog = Dialog(this)
        dailog.setContentView(R.layout.dialog_search_member)
        dailog.tv_add.setOnClickListener{
            val email = dailog.et_email_search_member.text.toString()
            if(email.isNotEmpty()){
                dailog.dismiss()
                showProgressDialog(resources.getString(R.string.please_wait))
                FirestoreClass().getMembersDetails(this, email)
            }else{
                showErrorSnackBar("Please enter members email address")
            }

        }
        dailog.tv_cancel.setOnClickListener{
            dailog.dismiss()
        }
        dailog.show()
    }

    override fun onBackPressed() {
        if(anyChangesMade){
            setResult(Activity.RESULT_OK)
        }
        super.onBackPressed()
    }

    fun memberAssignSuccess(user: User){
        hideProgressDialog()
        assignedMembersList.add(user)

        anyChangesMade = true

        setupMembersList(assignedMembersList)
        SendNotificationToUserAsyncTask(boardDetails.name, user.fcmToken).execute()
    }

    private inner class SendNotificationToUserAsyncTask(val boardName: String, val token: String) : AsyncTask<Any, Void, String>() {
        override fun onPreExecute(){
            super.onPreExecute()
        }
        override fun doInBackground(vararg p0: Any?): String {

            var result: String
            var connection: HttpURLConnection? = null

            try {
                val url = URL(Constants.FCM_BASE_URL)
                connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.doOutput = true
                connection.instanceFollowRedirects = false
                connection.requestMethod = "POST"


                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("charset", "utf-8")
                connection.setRequestProperty("Accept", "application/json")

                connection.setRequestProperty(
                    Constants.FCM_AUTHORIZATION,
                    "${Constants.FCM_KEY}=${Constants.FCM_SERVER_KEY}"
                )
                connection.useCaches = false

                val wr = DataOutputStream(connection.outputStream)
                val jsonRequest = JSONObject()
                val dataObject = JSONObject()
                dataObject.put(Constants.FCM_KEY_TITLE, "Assigned to the board $boardName")
                dataObject.put(
                    Constants.FCM_KEY_MESSAGE,
                    "You have been assigned to the new board by ${assignedMembersList[0].name}"
                )

                jsonRequest.put(Constants.FCM_KEY_DATA, dataObject)
                jsonRequest.put(Constants.FCM_KEY_TO, token)

                wr.writeBytes(jsonRequest.toString())
                wr.flush()
                wr.close()

                val httpResult: Int = connection.responseCode
                if (httpResult == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val reader = java.io.BufferedReader(InputStreamReader(inputStream))
                    val sb = StringBuilder()
                    var line: String?
                    try {
                        while (reader.readLine().also { line = it } != null) {
                            sb.append(line + "\n")
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    } finally {
                        try {
                            inputStream.close()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                    result = sb.toString()
                }else {
                    result = connection.responseMessage
                }
            }catch (e: SocketTimeoutException){
                result = "Connection Timeout"
            }catch (e: Exception){
                result = "Error: ${e.message}"
            }finally {
                connection?.disconnect()
            }
            return result
        }
        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            hideProgressDialog()
           // Log.e("JSON Response Result", result)
        }

    }

}
