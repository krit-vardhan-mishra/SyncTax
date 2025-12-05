package com.just_for_fun.synctax.presentation.components.utils

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.just_for_fun.synctax.R
import com.just_for_fun.synctax.data.model.Format
import com.just_for_fun.synctax.presentation.ui.adapter.FormatAdapter
import com.just_for_fun.synctax.presentation.viewmodels.FormatViewModel
import kotlinx.coroutines.launch

class FormatSelectionBottomSheetDialog : BottomSheetDialogFragment() {

    private val viewModel: FormatViewModel by lazy {
        ViewModelProvider(this)[FormatViewModel::class.java]
    }
    private lateinit var adapter: FormatAdapter

    private var url: String = ""
    private var onFormatSelected: ((Format) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.format_select_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView(view)
        setupClickListeners(view)
        observeViewModel()

        // Load formats if URL is provided
        if (url.isNotBlank()) {
            view.findViewById<TextView>(R.id.url).text = url
            viewModel.loadFormats(url)
        }
    }

    private fun setupRecyclerView(view: View) {
        adapter = FormatAdapter(object : FormatAdapter.OnItemClickListener {
            override fun onItemSelect(item: Format) {
                viewModel.selectFormat(item)
                view.findViewById<MaterialButton>(R.id.download_button).isEnabled = true
            }
        }, requireActivity())

        view.findViewById<RecyclerView>(R.id.recyclerView).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@FormatSelectionBottomSheetDialog.adapter
        }
    }

    private fun setupClickListeners(view: View) {
        view.findViewById<MaterialButton>(R.id.filter_button).setOnClickListener {
            viewModel.cycleCategory()
            updateFilterButtonText(view)
        }

        view.findViewById<MaterialButton>(R.id.refresh).setOnClickListener {
            if (url.isNotBlank()) {
                viewModel.refreshFormats(url)
            }
        }

        view.findViewById<MaterialButton>(R.id.download_button).setOnClickListener {
            viewModel.uiState.value.selectedFormat?.let { format ->
                onFormatSelected?.invoke(format)
                dismiss()
            }
        }
    }

    private fun updateFilterButtonText(view: View) {
        val categoryName = viewModel.getCategoryName()
        view.findViewById<TextView>(R.id.title).text = "Select Format ($categoryName)"
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                val view = requireView()

                // Update loading state
                view.findViewById<MaterialButton>(R.id.refresh).isEnabled = !state.isLoading

                // Update adapter data
                adapter.submitList(state.recyclerViewItems.toMutableList())

                // Update selected format in adapter
                adapter.selectedFormat = state.selectedFormat

                // Update download button
                view.findViewById<MaterialButton>(R.id.download_button).isEnabled = state.selectedFormat != null

                // Update filter category in title
                updateFilterButtonText(view)

                // Handle errors
                state.error?.let { error ->
                    // Show error (could use Snackbar)
                    view.findViewById<TextView>(R.id.title).text = "Error: $error"
                }
            }
        }
    }

    fun setUrl(url: String) {
        this.url = url
        if (isAdded) {
            requireView().findViewById<TextView>(R.id.url).text = url
            viewModel.loadFormats(url)
        }
    }

    fun setOnFormatSelectedListener(listener: (Format) -> Unit) {
        this.onFormatSelected = listener
    }

    companion object {
        const val TAG = "FormatSelectionBottomSheet"

        fun newInstance(): FormatSelectionBottomSheetDialog {
            return FormatSelectionBottomSheetDialog()
        }
    }
}
