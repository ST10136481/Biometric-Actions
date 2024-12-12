package com.example.biometricactions.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.biometricactions.R
import com.example.biometricactions.util.PreferencesManager

class ActionAdapter(
    private val onActionClick: (PreferencesManager.Action) -> Unit,
    private val selectedActionId: Int = -1
) : RecyclerView.Adapter<ActionAdapter.ActionViewHolder>() {

    private var actions: List<ActionItem> = emptyList()

    fun submitList(newList: List<ActionItem>) {
        actions = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_action, parent, false)
        return ActionViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActionViewHolder, position: Int) {
        val action = actions[position]
        holder.bind(action)
        holder.itemView.setOnClickListener {
            PreferencesManager.Action.values()
                .find { it.id == action.id }
                ?.let { onActionClick(it) }
        }
    }

    override fun getItemCount(): Int = actions.size

    inner class ActionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.textTitle)
        private val descriptionText: TextView = itemView.findViewById(R.id.textDescription)
        private val checkbox: CheckBox = itemView.findViewById(R.id.checkbox)

        fun bind(action: ActionItem) {
            titleText.text = action.title
            descriptionText.text = action.description
            checkbox.isChecked = action.id == selectedActionId
        }
    }

    data class ActionItem(
        val id: Int,
        val title: String,
        val description: String
    )
} 