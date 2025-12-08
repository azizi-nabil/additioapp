package com.example.additioapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.additioapp.AdditioApplication
import com.example.additioapp.R
import com.example.additioapp.ui.AdditioViewModelFactory
import com.example.additioapp.ui.adapters.SearchResultAdapter
import com.example.additioapp.ui.viewmodel.GlobalSearchViewModel

class GlobalSearchFragment : Fragment() {
    
    private lateinit var viewModel: GlobalSearchViewModel
    private lateinit var searchInput: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var adapter: SearchResultAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_global_search, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize ViewModel
        val repository = (requireActivity().application as AdditioApplication).repository
        val factory = AdditioViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[GlobalSearchViewModel::class.java]
        
        // Initialize views
        searchInput = view.findViewById(R.id.searchInput)
        recyclerView = view.findViewById(R.id.recyclerSearchResults)
        emptyView = view.findViewById(R.id.textEmptyResults)
        
        // Setup RecyclerView
        adapter = SearchResultAdapter(
            onStudentClick = { student ->
                // Navigate to student's class with student filter
                val bundle = Bundle().apply {
                    putLong("classId", student.classId)
                    putLong("studentId", student.id)
                    putString("studentName", student.displayNameFr)
                }
                androidx.navigation.Navigation.findNavController(requireView())
                    .navigate(R.id.classDetailFragment, bundle)
            },
            onClassClick = { classEntity ->
                // Navigate to class detail
                val bundle = Bundle().apply {
                    putLong("classId", classEntity.id)
                }
                androidx.navigation.Navigation.findNavController(requireView())
                    .navigate(R.id.classDetailFragment, bundle)
            },
            onEventClick = { event ->
                // Navigate to planner tab (where events are shown)
                parentFragmentManager.popBackStack()
                // Could navigate to planner with specific date if needed
            },
            onTaskClick = { task ->
                // Navigate to planner tab (where tasks are shown)
                parentFragmentManager.popBackStack()
                // Could navigate to planner with specific tab if needed
            }
        )
        
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        
        // Setup search
        searchInput.addTextChangedListener { text ->
            val query = text.toString().trim()
            if (query.length >= 2) {
                viewModel.search(query)
            } else {
                adapter.submitList(emptyList())
                updateEmptyView(true)
            }
        }
        
        // Observe search results
        viewModel.searchResults.observe(viewLifecycleOwner) { results ->
            adapter.submitList(results)
            updateEmptyView(results.isEmpty() && searchInput.text.toString().length >= 2)
        }
        
        // Back button
        view.findViewById<android.widget.ImageButton>(R.id.btnBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        
        // Auto-focus search input
        searchInput.requestFocus()
    }
    
    private fun updateEmptyView(isEmpty: Boolean) {
        if (isEmpty) {
            emptyView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }
}
