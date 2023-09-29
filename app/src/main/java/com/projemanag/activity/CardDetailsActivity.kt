package com.projemanag.activity

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import com.projemanag.R
import com.projemanag.adapters.CardMemberListItemsAdapter
import com.projemanag.dialogs.LabelColorListDialog
import com.projemanag.dialogs.MembersListDialog
import com.projemanag.firebase.FirestoreClass
import com.projemanag.model.Board
import com.projemanag.model.Card
import com.projemanag.model.SelectedMembers
import com.projemanag.model.Task
import com.projemanag.model.User
import com.projemanag.utils.Constants
import kotlinx.android.synthetic.main.activity_card_details.btn_update_card_details
import kotlinx.android.synthetic.main.activity_card_details.et_name_card_details
import kotlinx.android.synthetic.main.activity_card_details.rv_selected_members_list
import kotlinx.android.synthetic.main.activity_card_details.toolbar_card_details_activity
import kotlinx.android.synthetic.main.activity_card_details.tv_select_due_date
import kotlinx.android.synthetic.main.activity_card_details.tv_select_label_color
import kotlinx.android.synthetic.main.activity_card_details.tv_select_members
import java.util.Locale

class CardDetailsActivity : BaseActivity() {

    private lateinit var boardDetails: Board
    private var taskListPosition = -1
    private var cardPosition = -1
    private var selectedColor = ""
    private lateinit var membersDetailList: ArrayList<User>
    private var selectedDueDateMilliSeconds: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_details)
        getIntentData()
        setupActionBar()

        et_name_card_details.setText( boardDetails.taskList[taskListPosition].cards[cardPosition].name)
        et_name_card_details.setSelection(et_name_card_details.text.toString().length)

        selectedColor = boardDetails.taskList[taskListPosition].cards[cardPosition].labelColor
        if(selectedColor.isNotEmpty()){
            setColor()
        }

        btn_update_card_details.setOnClickListener {
            if(et_name_card_details.text.toString().isNotEmpty()){
                updateCardDetails()
            }else{
                showErrorSnackBar("Please enter a card name")
            }
        }
        tv_select_label_color.setOnClickListener {
            labelColorsList()
        }

        tv_select_members.setOnClickListener {
            membersListDialog()
        }

        setupSelectedMembersList()

        selectedDueDateMilliSeconds = boardDetails.taskList[taskListPosition].cards[cardPosition].dueDate

        if(selectedDueDateMilliSeconds > 0){
            val simpleDateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
            val selectedDate = simpleDateFormat.format(selectedDueDateMilliSeconds)
            tv_select_due_date.text = selectedDate
        }

        tv_select_due_date.setOnClickListener {
            showDataPicker()
        }

    }

    fun addUpdateTaskListSuccess(){
        hideProgressDialog()

        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun setupActionBar(){
        setSupportActionBar(toolbar_card_details_activity)
        val actionBar = supportActionBar
        if(actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_black_color_back_24dp)
            actionBar.title = boardDetails.taskList[taskListPosition].cards[cardPosition].name
        }
        toolbar_card_details_activity.setNavigationOnClickListener { onBackPressed() }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_delete_card, menu)
        return super.onCreateOptionsMenu(menu)
    }

    private fun  colorsList(): ArrayList<String>{
        val colorsList: ArrayList<String> = ArrayList()
        colorsList.add("#43C86F")
        colorsList.add("#0C90F1")
        colorsList.add("#F72400")
        colorsList.add("#7A8089")
        colorsList.add("#D57C1D")
        colorsList.add("#770000")
        colorsList.add("#0022F8")

        return colorsList
    }
    private fun setColor(){
        tv_select_label_color.text = ""
        tv_select_label_color.setBackgroundColor(Color.parseColor(selectedColor))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.action_delete_card ->{
                alertDialogForDeleteCard(boardDetails.taskList[taskListPosition].cards[cardPosition].name)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getIntentData(){
        if(intent.hasExtra(Constants.BOARD_DETAIL)){
            boardDetails = intent.getParcelableExtra<Board>(Constants.BOARD_DETAIL)!!
        }
        if(intent.hasExtra(Constants.TASK_LIST_ITEM_POSITION)){
            taskListPosition = intent.getIntExtra(Constants.TASK_LIST_ITEM_POSITION, -1)
        }
        if(intent.hasExtra(Constants.CARD_LIST_ITEM_POSITION)){
            cardPosition = intent.getIntExtra(Constants.CARD_LIST_ITEM_POSITION, -1)
        }
        if(intent.hasExtra(Constants.BOARD_MEMBERS_LIST)){
            membersDetailList = intent.getParcelableArrayListExtra(Constants.BOARD_MEMBERS_LIST)!!
       }
    }

    private fun membersListDialog(){
        var cardAssignedMembersList = boardDetails.taskList[taskListPosition].cards[cardPosition].assignedTo
        if(cardAssignedMembersList.size > 0){
            for(i in membersDetailList.indices){
                for(j in cardAssignedMembersList){
                    if(membersDetailList[i].id == j){
                        membersDetailList[i].selected = true
                    }
                }
            }
        }else{
            for(i in membersDetailList.indices){
                membersDetailList[i].selected = false
            }
        }

        val listDialog = object: MembersListDialog(this, membersDetailList,
            resources.getString(R.string.str_select_member)){
            override fun onItemSelected(user: User, action: String) {
                if(action == Constants.SELECT){
                    if(!boardDetails.taskList[taskListPosition].cards[cardPosition].assignedTo.contains(user.id)){
                        boardDetails.taskList[taskListPosition].cards[cardPosition].assignedTo.add(user.id)
                    }
                }else{
                    boardDetails.taskList[taskListPosition].cards[cardPosition].assignedTo.remove(user.id)
                    for(i in membersDetailList.indices){
                        if(membersDetailList[i].id == user.id){
                            membersDetailList[i].selected = false
                        }
                    }
                }
                setupSelectedMembersList()
            }
        }
        listDialog.show()
    }

    private fun setupSelectedMembersList(){
        val cardAssignedMemberList =
            boardDetails.taskList[taskListPosition].cards[cardPosition].assignedTo
        val selectedMembersList: ArrayList<SelectedMembers> = ArrayList()

        for(i in membersDetailList.indices){
            for(j in cardAssignedMemberList){
                if(membersDetailList[i].id == j){
                    val selectedMember = SelectedMembers(
                        membersDetailList[i].id,
                        membersDetailList[i].image
                    )
                    selectedMembersList.add(selectedMember)
                }
            }
        }

        if (selectedMembersList.size > 0){
            selectedMembersList.add(SelectedMembers("", ""))
            tv_select_members.visibility = View.GONE
            rv_selected_members_list.visibility = View.VISIBLE

            rv_selected_members_list.layoutManager = GridLayoutManager(this, 6)
            val adapter = CardMemberListItemsAdapter(this, selectedMembersList, true)
            rv_selected_members_list.adapter = adapter
            adapter.setOnClickListener(object :
                CardMemberListItemsAdapter.OnClickListener {
                override fun onClick() {
                    membersListDialog()
                }
            })
        }else{
            tv_select_members.visibility = View.VISIBLE
            rv_selected_members_list.visibility = View.GONE
        }

    }
    private fun showDataPicker(){
        val c = java.util.Calendar.getInstance()
        val year = c.get(java.util.Calendar.YEAR)
        val month = c.get(java.util.Calendar.MONTH)
        val dayOfMonth = c.get(java.util.Calendar.DAY_OF_MONTH)

        val datePickerDialog = android.app.DatePickerDialog(this,
            android.app.DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                val sDayOfMonth = if(dayOfMonth < 10) "0$dayOfMonth" else "$dayOfMonth"
                val sMonthOfYear = if((monthOfYear + 1) < 10) "0${monthOfYear + 1}" else "${monthOfYear + 1}"
                val selectedDate = "$sDayOfMonth/$sMonthOfYear/$year"
                tv_select_due_date.text = selectedDate

                val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
                val theDate = sdf.parse(selectedDate)
                selectedDueDateMilliSeconds = theDate!!.time
            },
            year,
            month,
            dayOfMonth
        )
        datePickerDialog.show()
    }


    private fun updateCardDetails(){
       val card = Card(
           et_name_card_details.text.toString(),
              boardDetails.taskList[taskListPosition].cards[cardPosition].createdBy,
                boardDetails.taskList[taskListPosition].cards[cardPosition].assignedTo,
                selectedColor,
                selectedDueDateMilliSeconds
       )

        val taskList: ArrayList<Task> = boardDetails.taskList
        taskList.removeAt(taskList.size - 1)

        boardDetails.taskList[taskListPosition].cards[cardPosition] = card

        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().addUpdateTaskList(this@CardDetailsActivity, boardDetails)
    }

    private fun alertDialogForDeleteCard(cardName: String){
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Alert")
        builder.setMessage("Are you sure you want to delete $cardName")
        builder.setIcon(android.R.drawable.ic_dialog_alert)
        builder.setPositiveButton("Yes"){ dialogInterface, which ->
            dialogInterface.dismiss()
            deleteCard()
        }
        builder.setNegativeButton("No"){ dialogInterface, which ->
            dialogInterface.dismiss()
        }
        val alertDialog: android.app.AlertDialog = builder.create()
        alertDialog.setCancelable(false)
        alertDialog.show()
    }

    private fun deleteCard(){
        val cardsList: ArrayList<Card> = boardDetails.taskList[taskListPosition].cards
        cardsList.removeAt(cardPosition)
        val taskList: ArrayList<Task> = boardDetails.taskList
        taskList.removeAt(taskList.size - 1)
        taskList[taskListPosition].cards = cardsList

        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().addUpdateTaskList(this@CardDetailsActivity, boardDetails)
    }

    private fun labelColorsList(): ArrayList<String>{
        val colorsList: ArrayList<String> = colorsList()
        val listDialog = object: LabelColorListDialog(this, colorsList,
            resources.getString(R.string.str_select_label_color), selectedColor){
           override fun onItemSelected(color: String) {
                selectedColor = color
                setColor()
            }
        }
        listDialog.show()
        return colorsList
    }

}

