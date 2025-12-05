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
        }
    }
}
