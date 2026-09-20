package com.example.myapplicationtoday

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class AchievementsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)
        view.findViewById<TextView>(R.id.tvHeader).text = "MEDALS &\nACHIEVEMENTS"
        view.findViewById<View>(R.id.cardTimer).visibility = View.GONE
        return view
    }
}
