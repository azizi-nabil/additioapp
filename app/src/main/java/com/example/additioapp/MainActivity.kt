package com.example.additioapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setupWithNavController(navController)

        // Handle reselection - pop back stack to the root of that tab
        bottomNavigationView.setOnItemReselectedListener { item ->
            // Pop back stack to the destination
            navController.popBackStack(item.itemId, inclusive = false)
        }

        // Handle selection - always navigate fresh, clearing back stack
        bottomNavigationView.setOnItemSelectedListener { item ->
            // If already on a destination with the same id, don't navigate again
            if (navController.currentDestination?.id == item.itemId) {
                return@setOnItemSelectedListener true
            }

            // Pop back to start destination first to clear nested fragments
            navController.popBackStack(navController.graph.startDestinationId, inclusive = false)
            
            // Then navigate to the selected item
            if (item.itemId != navController.graph.startDestinationId) {
                navController.navigate(item.itemId)
            }
            true
        }

        // Keep bottom nav in sync when navigating via back button
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Update selected item based on current destination
            when (destination.id) {
                R.id.homeFragment -> bottomNavigationView.menu.findItem(R.id.homeFragment)?.isChecked = true
                R.id.plannerFragment -> bottomNavigationView.menu.findItem(R.id.plannerFragment)?.isChecked = true
                R.id.classesFragment -> bottomNavigationView.menu.findItem(R.id.classesFragment)?.isChecked = true
                R.id.analyticsFragment -> bottomNavigationView.menu.findItem(R.id.analyticsFragment)?.isChecked = true
                R.id.settingsFragment -> bottomNavigationView.menu.findItem(R.id.settingsFragment)?.isChecked = true
            }
            
            // Hide FAB on planner, search, and settings screens (Planner has its own FAB)
            // Also check user preference for showing global search
            val fabSearch = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabSearch)
            val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
            val showGlobalSearch = prefs.getBoolean("pref_show_global_search", true)
            
            when {
                !showGlobalSearch -> fabSearch.hide() // User disabled global search
                destination.id in listOf(R.id.plannerFragment, R.id.globalSearchFragment, R.id.settingsFragment) -> fabSearch.hide()
                else -> fabSearch.show()
            }
        }

        // Search FAB
        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabSearch).setOnClickListener {
            android.util.Log.d("MainActivity", "Search FAB clicked!")
            android.util.Log.d("MainActivity", "Current destination: ${navController.currentDestination?.id}")
            android.util.Log.d("MainActivity", "NavController: $navController")
            
            try {
                android.util.Log.d("MainActivity", "Attempting to navigate to globalSearchFragment...")
                navController.navigate(R.id.globalSearchFragment)
                android.util.Log.d("MainActivity", "Navigation successful!")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed to navigate to global search", e)
                android.util.Log.e("MainActivity", "Exception type: ${e.javaClass.name}")
                android.util.Log.e("MainActivity", "Exception message: ${e.message}")
            }
        }
    }
}
