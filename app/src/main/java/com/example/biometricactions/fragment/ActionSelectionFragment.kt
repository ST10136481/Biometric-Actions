package com.example.biometricactions.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.biometricactions.R
import com.example.biometricactions.adapter.ActionAdapter
import com.example.biometricactions.util.PreferencesManager
import com.google.android.material.appbar.MaterialToolbar

class ActionSelectionFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var actionAdapter: ActionAdapter
    private lateinit var preferencesManager: PreferencesManager
    private var actionKey: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionKey = arguments?.getString(ARG_ACTION_KEY)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_action_selection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        preferencesManager = PreferencesManager(requireContext())
        
        // Set up toolbar
        view.findViewById<MaterialToolbar>(R.id.toolbar).apply {
            setNavigationOnClickListener {
                parentFragmentManager.popBackStack()
            }
        }

        // Set up RecyclerView
        recyclerView = view.findViewById(R.id.recyclerViewActions)
        recyclerView.layoutManager = LinearLayoutManager(context)
        
        // Get currently selected action ID
        val currentActionId = actionKey?.let { key ->
            preferencesManager.getAction(key)?.id
        } ?: -1

        // Initialize adapter with click handler and current selection
        actionAdapter = ActionAdapter(
            onActionClick = { action ->
                actionKey?.let { key ->
                    preferencesManager.setAction(key, action)
                    parentFragmentManager.popBackStack()
                }
            },
            selectedActionId = currentActionId
        )
        
        recyclerView.adapter = actionAdapter

        // Load available actions
        val actions = PreferencesManager.Action.values()
            .filter { it != PreferencesManager.Action.NONE }
            .map { action ->
                ActionAdapter.ActionItem(
                    id = action.id,
                    title = action.displayName,
                    description = getActionDescription(action)
                )
            }
        actionAdapter.submitList(actions)
    }

    private fun getActionDescription(action: PreferencesManager.Action): String {
        return when (action) {
            PreferencesManager.Action.TOGGLE_FLASHLIGHT -> "Turn flashlight on/off"
            PreferencesManager.Action.OPEN_CAMERA -> "Launch camera app"
            PreferencesManager.Action.TOGGLE_SILENT_MODE -> "Toggle silent mode"
            PreferencesManager.Action.TAKE_SCREENSHOT -> "Take a screenshot"
            PreferencesManager.Action.NONE -> "No action assigned"
        }
    }

    companion object {
        private const val ARG_ACTION_KEY = "action_key"

        fun newInstance(actionKey: String): ActionSelectionFragment {
            return ActionSelectionFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ACTION_KEY, actionKey)
                }
            }
        }
    }
} 