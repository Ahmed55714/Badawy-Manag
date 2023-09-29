package com.projemanag.adapters

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.projemanag.R
import com.projemanag.activity.TaskListActivity
import com.projemanag.model.Task
import kotlinx.android.synthetic.main.item_task.view.cv_add_card
import kotlinx.android.synthetic.main.item_task.view.cv_add_task_list_name
import kotlinx.android.synthetic.main.item_task.view.cv_edit_task_list_name
import kotlinx.android.synthetic.main.item_task.view.et_card_name
import kotlinx.android.synthetic.main.item_task.view.et_edit_task_list_name
import kotlinx.android.synthetic.main.item_task.view.et_task_list_name
import kotlinx.android.synthetic.main.item_task.view.ib_close_editable_view
import kotlinx.android.synthetic.main.item_task.view.ib_close_list_name
import kotlinx.android.synthetic.main.item_task.view.ib_delete_list
import kotlinx.android.synthetic.main.item_task.view.ib_done_card_name
import kotlinx.android.synthetic.main.item_task.view.ib_done_edit_list_name
import kotlinx.android.synthetic.main.item_task.view.ib_done_list_name
import kotlinx.android.synthetic.main.item_task.view.ll_task_item
import kotlinx.android.synthetic.main.item_task.view.ll_title_view
import kotlinx.android.synthetic.main.item_task.view.rv_card_list
import kotlinx.android.synthetic.main.item_task.view.tv_add_card
import kotlinx.android.synthetic.main.item_task.view.tv_add_task_list
import kotlinx.android.synthetic.main.item_task.view.tv_task_list_title
import java.util.Collections

open class TaskListItemsAdapter(
    private val context: Context,
    private var list: ArrayList<Task>)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var PositionDraggedFrom = -1
    private var PositionDraggedTo = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : RecyclerView.ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_task, parent, false)
        val layoutParams = RecyclerView.LayoutParams((parent.width * 0.7).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        layoutParams.setMargins((15.toDp()).toPx(), 0, (40.toDp()).toPx(), 0)
        view.layoutParams = layoutParams
        return MyViewHolder(view)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val model = list[position]
        if(holder is MyViewHolder){
            if(position == list.size - 1){
                holder.itemView.tv_add_task_list.visibility = View.VISIBLE
                holder.itemView.ll_task_item.visibility = View.GONE
        }else{
                holder.itemView.tv_add_task_list.visibility = View.GONE
                holder.itemView.ll_task_item.visibility = View.VISIBLE
            }

            holder.itemView.tv_task_list_title.text = model.title
            holder.itemView.tv_add_task_list.setOnClickListener {
                holder.itemView.tv_add_task_list.visibility = View.GONE
                holder.itemView.cv_add_task_list_name.visibility = View.VISIBLE
            }
            holder.itemView.ib_close_list_name.setOnClickListener {
                holder.itemView.tv_add_task_list.visibility = View.VISIBLE
                holder.itemView.cv_add_task_list_name.visibility = View.GONE
            }
            holder.itemView.ib_done_list_name.setOnClickListener {
                val listName = holder.itemView.et_task_list_name.text.toString()
                if(listName.isNotEmpty()){
                    if(context is TaskListActivity){
                        context.createTaskList(listName)
                    }
                }else{
                    Toast.makeText(context, "Please enter list name.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        holder.itemView.ib_done_edit_list_name.setOnClickListener {
            holder.itemView.et_edit_task_list_name.setText(model.title)
            holder.itemView.ll_title_view.visibility = View.GONE
            holder.itemView.cv_edit_task_list_name.visibility = View.VISIBLE
        }

        holder.itemView.ib_close_editable_view.setOnClickListener {
            holder.itemView.ll_title_view.visibility = View.VISIBLE
            holder.itemView.cv_edit_task_list_name.visibility = View.GONE
        }

        holder.itemView.ib_done_edit_list_name.setOnClickListener {
            val listName = holder.itemView.et_edit_task_list_name.text.toString()
            if(listName.isNotEmpty()){
                if(context is TaskListActivity){
                    context.updateTaskList(position, listName, model)
                }
            }else{
                Toast.makeText(context, "Please enter list name.", Toast.LENGTH_SHORT).show()
            }
        }

        holder.itemView.ib_delete_list.setOnClickListener {
            alertDialogForDeleteList(position, model.title)
        }

        holder.itemView.tv_add_card.setOnClickListener{
            holder.itemView.tv_add_card.visibility = View.GONE
            holder.itemView.cv_add_card.visibility = View.VISIBLE
        }

        holder.itemView.ib_close_list_name.setOnClickListener {
            holder.itemView.tv_add_card.visibility = View.VISIBLE
            holder.itemView.cv_add_card.visibility = View.GONE
        }

        holder.itemView.ib_done_card_name.setOnClickListener {
            val cardName = holder.itemView.et_card_name.text.toString()
            if(cardName.isNotEmpty()){
                if(context is TaskListActivity){
                    context.addCardToTaskList(position, cardName)
                }
            }else{
                Toast.makeText(context, "Please enter a card name.", Toast.LENGTH_SHORT).show()
            }
        }

        holder.itemView.rv_card_list.layoutManager = LinearLayoutManager(context)
        holder.itemView.rv_card_list.setHasFixedSize(true)

        val adapter = CardListItemsAdapter(context, model.cards)
        holder.itemView.rv_card_list.adapter = adapter

        adapter.setOnClickListener(
            object : CardListItemsAdapter.OnClickListener{
                override fun onClick(cardPosition: Int) {
                    if(context is TaskListActivity){
                        context.cardDetails(position, cardPosition)
                    }
                }
            }
        )

        val dividerItemDecoration = androidx.recyclerview.widget.DividerItemDecoration(context, androidx.recyclerview.widget.DividerItemDecoration.VERTICAL)
        holder.itemView.rv_card_list.addItemDecoration(dividerItemDecoration)

        val helper = androidx.recyclerview.widget.ItemTouchHelper(
            object : androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(
                androidx.recyclerview.widget.ItemTouchHelper.UP or androidx.recyclerview.widget.ItemTouchHelper.DOWN, 0
            ){
                override fun onMove(
                    recyclerView: androidx.recyclerview.widget.RecyclerView,
                    viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
                    target: androidx.recyclerview.widget.RecyclerView.ViewHolder
                ): Boolean {
                    val draggedPosition = viewHolder.adapterPosition
                    val targetPosition = target.adapterPosition

                        if(PositionDraggedFrom == -1){
                            PositionDraggedFrom = draggedPosition
                        }
                        PositionDraggedTo = targetPosition
                        Collections.swap(list[position].cards, draggedPosition, targetPosition)
                        adapter.notifyItemMoved(draggedPosition, targetPosition)
                    return false
                }


                override fun onSwiped(
                    viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
                    direction: Int
                ) {

                }

                override fun clearView(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder
                ) {
                    super.clearView(recyclerView, viewHolder)

                    if(PositionDraggedFrom != -1 && PositionDraggedTo != -1 && PositionDraggedFrom != PositionDraggedTo){
                        (context as TaskListActivity).updateCardsInTaskList(position, list[position].cards)
                    }
                    PositionDraggedFrom = -1
                    PositionDraggedTo = -1
                }
            }
        )
        helper.attachToRecyclerView(holder.itemView.rv_card_list)

    }

    private fun alertDialogForDeleteList(position: Int, title: String){
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Alert")
        builder.setMessage("Are you sure you want to delete $title.")
        builder.setIcon(android.R.drawable.ic_dialog_alert)
        builder.setPositiveButton("Yes") { dialogInterface, which ->
            dialogInterface.dismiss()
            if(context is TaskListActivity){
                context.deleteTaskList(position)
            }
        }
        builder.setNegativeButton("No") { dialogInterface, which ->
            dialogInterface.dismiss()
        }
        val alertDialog: AlertDialog = builder.create()
        alertDialog.setCancelable(false)
        alertDialog.show()
    }

    private fun Int.toDp(): Int = (this / context.resources.displayMetrics.density).toInt()

    private fun Int.toPx(): Int = (this * context.resources.displayMetrics.density).toInt()

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view)



    }