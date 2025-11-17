package com.example.cinephile.ui.watchlists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.setFragmentResult
import com.example.cinephile.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class RenameWatchlistBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_rename_watchlist, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val inputLayout = view.findViewById<TextInputLayout>(R.id.text_input_layout)
        val editText = view.findViewById<TextInputEditText>(R.id.edit_text_name)
        val buttonCancel = view.findViewById<MaterialButton>(R.id.button_cancel)
        val buttonRename = view.findViewById<MaterialButton>(R.id.button_rename)

        val watchlistId = requireArguments().getLong(ARG_ID)
        val currentName = requireArguments().getString(ARG_CURRENT_NAME).orEmpty()

        editText.setText(currentName)
        editText.setSelection(currentName.length)
        buttonRename.isEnabled = currentName.isNotBlank()

        editText.addTextChangedListener {
            val text = it?.toString().orEmpty()
            val isValid = text.isNotBlank()
            buttonRename.isEnabled = isValid
            inputLayout.error = if (isValid) null else getString(R.string.validation_required)
        }

        buttonCancel.setOnClickListener { dismiss() }

        buttonRename.setOnClickListener {
            val name = editText.text?.toString()?.trim().orEmpty()
            if (name.isBlank()) {
                inputLayout.error = getString(R.string.validation_required)
                return@setOnClickListener
            }
            setFragmentResult(RESULT_KEY, bundleOf(ARG_ID to watchlistId, ARG_NAME to name))
            dismiss()
        }
    }

    companion object {
        const val RESULT_KEY = "rename_watchlist_result"
        private const val ARG_ID = "id"
        const val ARG_NAME = "name"
        private const val ARG_CURRENT_NAME = "current_name"

        fun newInstance(id: Long, currentName: String): RenameWatchlistBottomSheet {
            return RenameWatchlistBottomSheet().apply {
                arguments = bundleOf(ARG_ID to id, ARG_CURRENT_NAME to currentName)
            }
        }
    }
}


