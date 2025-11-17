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
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.button.MaterialButton

class CreateWatchlistBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_create_watchlist, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val inputLayout = view.findViewById<TextInputLayout>(R.id.text_input_layout)
        val editText = view.findViewById<TextInputEditText>(R.id.edit_text_name)
        val buttonCancel = view.findViewById<MaterialButton>(R.id.button_cancel)
        val buttonCreate = view.findViewById<MaterialButton>(R.id.button_create)

        editText.addTextChangedListener {
            val text = it?.toString().orEmpty()
            val isValid = text.isNotBlank()
            buttonCreate.isEnabled = isValid
            inputLayout.error = if (isValid) null else getString(R.string.validation_required)
        }

        buttonCancel.setOnClickListener { dismiss() }

        buttonCreate.setOnClickListener {
            val name = editText.text?.toString()?.trim().orEmpty()
            if (name.isBlank()) {
                inputLayout.error = getString(R.string.validation_required)
                return@setOnClickListener
            }
            setFragmentResult(RESULT_KEY, bundleOf(ARG_NAME to name))
            dismiss()
        }
    }

    companion object {
        const val RESULT_KEY = "create_watchlist_result"
        const val ARG_NAME = "name"
        fun newInstance(): CreateWatchlistBottomSheet = CreateWatchlistBottomSheet()
    }
}


