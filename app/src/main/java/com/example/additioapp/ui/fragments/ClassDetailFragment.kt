package com.example.additioapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.additioapp.R
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

import androidx.fragment.app.viewModels
import com.example.additioapp.AdditioApplication
import com.example.additioapp.data.model.ClassEntity
import com.example.additioapp.ui.AdditioViewModelFactory
import com.example.additioapp.ui.viewmodel.ClassViewModel
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController

class ClassDetailFragment : Fragment() {

    private var classId: Long = -1
    private var studentId: Long = -1L
    private var studentName: String? = null
    private var currentClass: ClassEntity? = null
    
    private val viewModel: ClassViewModel by viewModels {
        AdditioViewModelFactory((requireActivity().application as AdditioApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            classId = it.getLong("classId")
            studentId = it.getLong("studentId", -1L)
            studentName = it.getString("studentName")
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_class_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        val viewPager = view.findViewById<ViewPager2>(R.id.viewPager)
        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)

        val textTabName = view.findViewById<android.widget.TextView>(R.id.textTabName)

        // Setup Toolbar
        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        val adapter = ClassDetailPagerAdapter(this, classId, studentId)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            // Set icons instead of text
            tab.icon = when (position) {
                0 -> androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.ic_school_24dp)
                1 -> androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.ic_calendar_today_24dp)
                2 -> androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.ic_grade)
                3 -> androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.ic_thumb_up_24dp)
                else -> null
            }
        }.attach()

        // Update header text on tab selection
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                textTabName.text = when (tab?.position) {
                    0 -> getString(R.string.students_title)
                    1 -> getString(R.string.attendance_title)
                    2 -> getString(R.string.grades_title)
                    3 -> getString(R.string.behavior_title)
                    else -> ""
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
        
        // Set initial text
        textTabName.text = getString(R.string.students_title)

        // Fetch class details
        lifecycleScope.launch {
             viewModel.allClasses.observe(viewLifecycleOwner) { classes ->
                currentClass = classes.find { it.id == classId }
                // Update title
                currentClass?.let {
                    toolbar.title = it.name
                }
            }
        }
    }

    // Removed onCreateOptionsMenu and onOptionsItemSelected as we use Toolbar directly

    class ClassDetailPagerAdapter(
        fragment: Fragment, 
        private val classId: Long,
        private val studentId: Long = -1L
    ) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 4

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> StudentsFragment.newInstance(classId, studentId)
                1 -> AttendanceHistoryFragment.newInstance(classId)
                2 -> GradesFragment.newInstance(classId)
                3 -> BehaviorFragment.newInstance(classId)
                else -> Fragment() // Placeholder for other tabs
            }
        }
    }
}
