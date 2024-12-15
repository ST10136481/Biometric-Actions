package com.example.biometricactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.biometricactions.databinding.FragmentHomeBinding
import com.example.biometricactions.fragment.ActionSelectionFragment
import com.example.biometricactions.util.PreferencesManager
import com.google.android.material.card.MaterialCardView

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var preferencesManager: PreferencesManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        preferencesManager = PreferencesManager(requireContext())
        setupViews()
        updateActionDisplays()
    }

    private fun setupViews() {
        // Theme switch setup
        binding.themeSwitch.apply {
            isChecked = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_NO
            setThumbIconResource(if (isChecked) R.drawable.ic_light_mode else R.drawable.ic_dark_mode)
            
            setOnCheckedChangeListener { _, isChecked ->
                // Enable cross-fade animation
                requireActivity().window.setWindowAnimations(R.style.WindowAnimationTransition)
                
                AppCompatDelegate.setDefaultNightMode(
                    if (isChecked) AppCompatDelegate.MODE_NIGHT_NO
                    else AppCompatDelegate.MODE_NIGHT_YES
                )
                setThumbIconResource(if (isChecked) R.drawable.ic_light_mode else R.drawable.ic_dark_mode)
            }
        }

        // Action cards setup
        binding.cardSingleTap.setOnClickListener {
            showActionSelection(PreferencesManager.SINGLE_TAP_KEY)
        }

        binding.cardDoubleTap.setOnClickListener {
            showActionSelection(PreferencesManager.SHAKE_KEY)
        }

        binding.cardLongPress.setOnClickListener {
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
        updateActionDisplay(PreferencesManager.SINGLE_TAP_KEY, binding.textSingleTapAction)
        updateActionDisplay(PreferencesManager.SHAKE_KEY, binding.textDoubleTapAction)
        updateActionDisplay(PreferencesManager.LONG_PRESS_KEY, binding.textLongPressAction)
    }

    private fun updateActionDisplay(actionKey: String, textView: TextView) {
        val action = preferencesManager.getAction(actionKey)
        textView.text = action?.displayName ?: "No action selected"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        updateActionDisplays()
    }
} 