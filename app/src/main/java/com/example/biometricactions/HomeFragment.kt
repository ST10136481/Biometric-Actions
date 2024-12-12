package com.example.biometricactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.biometricactions.fragment.ActionSelectionFragment
import com.example.biometricactions.util.PreferencesManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class HomeFragment : Fragment() {
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var singleTapCard: MaterialCardView
    private lateinit var doubleTapCard: MaterialCardView
    private lateinit var longPressCard: MaterialCardView
    private lateinit var buttonLightTheme: MaterialButton
    private lateinit var buttonDarkTheme: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        preferencesManager = PreferencesManager(requireContext())

        // Initialize views
        singleTapCard = view.findViewById(R.id.cardSingleTap)
        doubleTapCard = view.findViewById(R.id.cardDoubleTap)
        longPressCard = view.findViewById(R.id.cardLongPress)
        buttonLightTheme = view.findViewById(R.id.buttonLightTheme)
        buttonDarkTheme = view.findViewById(R.id.buttonDarkTheme)

        setupThemeButtons()
        setupActionCards()
        updateActionDisplays()
    }

    private fun setupThemeButtons() {
        buttonLightTheme.setOnClickListener {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        buttonDarkTheme.setOnClickListener {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }

    private fun setupActionCards() {
        singleTapCard.setOnClickListener {
            showActionSelection(PreferencesManager.SINGLE_TAP_KEY)
        }

        doubleTapCard.setOnClickListener {
            showActionSelection(PreferencesManager.DOUBLE_TAP_KEY)
        }

        longPressCard.setOnClickListener {
            showActionSelection(PreferencesManager.LONG_PRESS_KEY)
        }
    }

    private fun showActionSelection(actionKey: String) {
        val fragment = ActionSelectionFragment.newInstance(actionKey)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun updateActionDisplays() {
        // Update the display of currently selected actions
        updateActionDisplay(PreferencesManager.SINGLE_TAP_KEY, R.id.textSingleTapAction)
        updateActionDisplay(PreferencesManager.DOUBLE_TAP_KEY, R.id.textDoubleTapAction)
        updateActionDisplay(PreferencesManager.LONG_PRESS_KEY, R.id.textLongPressAction)
    }

    private fun updateActionDisplay(actionKey: String, textViewId: Int) {
        val action = preferencesManager.getAction(actionKey)
        view?.findViewById<TextView>(textViewId)?.text = action?.displayName ?: "No action selected"
    }

    override fun onResume() {
        super.onResume()
        updateActionDisplays()
    }
} 