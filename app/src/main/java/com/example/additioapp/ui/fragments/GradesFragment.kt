package com.example.additioapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.additioapp.AdditioApplication
import com.example.additioapp.R
import com.example.additioapp.ui.AdditioViewModelFactory
import com.example.additioapp.ui.adapters.GradeItemAdapter
import com.example.additioapp.ui.dialogs.AddGradeItemDialog
import com.example.additioapp.ui.viewmodel.GradeViewModel
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

class GradesFragment : Fragment() {

    private var classId: Long = -1
    private val viewModel: GradeViewModel by viewModels {
        AdditioViewModelFactory((requireActivity().application as AdditioApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            classId = it.getLong("classId")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_grades, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewGradeItems)
        val fab = view.findViewById<ExtendedFloatingActionButton>(R.id.fabAddGradeItem)

        val adapter = GradeItemAdapter(
            onItemClick = { gradeItem ->
                val bundle = Bundle().apply {
                    putLong("gradeItemId", gradeItem.id)
                    putLong("classId", classId) // Pass classId for context if needed
                    putString("gradeItemName", gradeItem.name)
                }
                androidx.navigation.Navigation.findNavController(view).navigate(R.id.gradeEntryFragment, bundle)
            },
            onMoreClick = { gradeItem, anchor ->
                val popup = androidx.appcompat.widget.PopupMenu(requireContext(), anchor)
                popup.menu.add("Edit")
                popup.menu.add("Delete")
                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.title) {
                        "Edit" -> {
                            val dialog = AddGradeItemDialog(classId, gradeItem) { updatedItem ->
                                viewModel.insertGradeItem(updatedItem)
                            }
                            dialog.show(parentFragmentManager, "EditGradeItemDialog")
                            true
                        }
                        "Delete" -> {
                            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                .setTitle("Delete Grade Item")
                                .setMessage("Are you sure you want to delete '${gradeItem.name}'? All student grades for this item will be lost.")
                                .setPositiveButton("Delete") { _, _ ->
                                    viewModel.deleteGradeItem(gradeItem)
                                }
                                .setNegativeButton("Cancel", null)
                                .show()
                            true
                        }
                        else -> false
                    }
                }
                popup.show()
            }
        )

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        viewModel.getGradeItemsForClass(classId).observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
        }

        fab.setOnClickListener {
            val dialog = AddGradeItemDialog(classId, null) { newItem ->
                val itemWithClassId = newItem.copy(classId = classId)
                viewModel.insertGradeItem(itemWithClassId)
            }
            dialog.show(parentFragmentManager, "AddGradeItemDialog")
        }
    }

    companion object {
        fun newInstance(classId: Long) = GradesFragment().apply {
            arguments = Bundle().apply {
                putLong("classId", classId)
            }
        }
    }
}
