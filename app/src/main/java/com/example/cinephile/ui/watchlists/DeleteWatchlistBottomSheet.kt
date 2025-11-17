package com.example.cinephile.ui.watchlists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.example.cinephile.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton

class DeleteWatchlistBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_delete_watchlist, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val id = requireArguments().getLong(ARG_ID)
        val buttonCancel = view.findViewById<MaterialButton>(R.id.button_cancel)
        val buttonDelete = view.findViewById<MaterialButton>(R.id.button_delete)

        buttonCancel.setOnClickListener {
            dismiss()
        }

        buttonDelete.setOnClickListener {
            setFragmentResult(RESULT_KEY, bundleOf(ARG_ID to id))
            dismiss()
        }
    }

    companion object {
        const val RESULT_KEY = "delete_watchlist_result"
        const val ARG_ID = "id"

        fun newInstance(id: Long): DeleteWatchlistBottomSheet {
            return DeleteWatchlistBottomSheet().apply {
                arguments = bundleOf(ARG_ID to id)
            }
        }
    }
}

