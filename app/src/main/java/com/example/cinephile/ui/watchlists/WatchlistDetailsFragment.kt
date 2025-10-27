package com.example.cinephile.ui.watchlists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.cinephile.databinding.FragmentWatchlistDetailsBinding

class WatchlistDetailsFragment : Fragment() {
    private var _binding: FragmentWatchlistDetailsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWatchlistDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.textWatchlistDetails.text = "Watchlist Details Fragment"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
