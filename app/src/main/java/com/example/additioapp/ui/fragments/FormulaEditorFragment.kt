package com.example.additioapp.ui.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.additioapp.R
import com.example.additioapp.ui.adapters.TokenAdapter
import com.google.android.material.tabs.TabLayout

class FormulaEditorFragment : DialogFragment() {

    private var existingFormula: String? = null
    private var variableNames: ArrayList<String> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, com.google.android.material.R.style.Theme_Material3_DayNight_NoActionBar)
        arguments?.let {
            existingFormula = it.getString(ARG_FORMULA)
            variableNames = it.getStringArrayList(ARG_VARIABLES) ?: arrayListOf()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_formula_editor, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        val editFormula = view.findViewById<EditText>(R.id.editFormula)
        val btnApply = view.findViewById<Button>(R.id.btnApply)
        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewTokens)
        val layoutOperators = view.findViewById<LinearLayout>(R.id.layoutOperators)

        // Setup Toolbar
        toolbar.setNavigationOnClickListener { dismiss() }
        
        // Populate existing formula
        existingFormula?.let {
            editFormula.setText(it)
            editFormula.setSelection(it.length)
        }

        // Apply Button
        btnApply.setOnClickListener {
            val result = editFormula.text.toString()
            parentFragmentManager.setFragmentResult(
                REQUEST_KEY,
                Bundle().apply { putString(RESULT_FORMULA, result) }
            )
            dismiss()
        }

        // Setup Operators
        val operators = listOf("+", "-", "*", "/", "(", ")", ".", ",", "<", ">", "<=", ">=", "==")
        operators.forEach { op ->
            val btn = com.google.android.material.button.MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle)
            btn.text = op
            btn.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = 8 }
            btn.minWidth = 0
            btn.minimumWidth = 0
            btn.setPadding(32, 0, 32, 0)
            btn.setOnClickListener { insertText(editFormula, op) }
            layoutOperators.addView(btn)
        }

        // Setup RecyclerView
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        val adapter = TokenAdapter { token -> insertText(editFormula, token) }
        recyclerView.adapter = adapter

        // Setup Tabs
        val tabs = listOf(
            getString(R.string.tab_variables),
            getString(R.string.tab_functions),
            getString(R.string.tab_system)
        )
        tabs.forEach { tabLayout.addTab(tabLayout.newTab().setText(it)) }

        // Initial Data (Variables)
        val variables = variableNames
        val functions = listOf("avg(", "max(", "min(", "if(")
        val system = listOf("abs_td", "abs_tp", "just_td", "just_tp", "pres_c", "tot_td", "tot_tp", "tot_c", "pos", "neg")

        fun updateList(position: Int) {
            when (position) {
                0 -> adapter.submitList(variables)
                1 -> adapter.submitList(functions)
                2 -> adapter.submitList(system)
            }
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.position?.let { updateList(it) }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Load initial
        updateList(0)
    }

    private fun insertText(editText: EditText, text: String) {
        val start = Math.max(editText.selectionStart, 0)
        val end = Math.max(editText.selectionEnd, 0)
        editText.text.replace(Math.min(start, end), Math.max(start, end), text, 0, text.length)
    }

    companion object {
        const val TAG = "FormulaEditorFragment"
        const val REQUEST_KEY = "request_formula_result"
        const val RESULT_FORMULA = "result_formula"
        private const val ARG_FORMULA = "arg_formula"
        private const val ARG_VARIABLES = "arg_variables"

        fun newInstance(formula: String?, variables: ArrayList<String>) = FormulaEditorFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_FORMULA, formula)
                putStringArrayList(ARG_VARIABLES, variables)
            }
        }
    }
}
