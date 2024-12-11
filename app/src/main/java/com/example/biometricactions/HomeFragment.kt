package com.example.biometricactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupButtons(view)
    }

    private fun setupButtons(view: View) {
        view.findViewById<MaterialButton>(R.id.singleTapButton).setOnClickListener {
            // TODO: Handle single tap action
        }

        view.findViewById<MaterialButton>(R.id.doubleTapButton).setOnClickListener {
            // TODO: Handle double tap action
        }

        view.findViewById<MaterialButton>(R.id.longPressButton).setOnClickListener {
            // TODO: Handle long press action
        }
    }
} 