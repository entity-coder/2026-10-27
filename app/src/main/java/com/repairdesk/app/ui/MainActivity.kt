package com.repairdesk.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.repairdesk.app.R
import com.repairdesk.app.databinding.ActivityMainBinding

/**
 * Single-activity shell: three tabs, switched with fragment transactions.
 * A shop phone is used one-handed at the counter, so bottom navigation —
 * not a drawer — is the right call.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            showFragment(DashboardFragment())
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> showFragment(DashboardFragment())
                R.id.nav_jobs -> showFragment(JobsFragment())
                R.id.nav_new_job -> showFragment(NewJobFragment())
                else -> false
            }
            true
        }
    }

    private fun showFragment(fragment: Fragment): Boolean {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
        return true
    }

    /** Called after a job is created, so the user lands on the job list. */
    fun goToJobsTab() {
        binding.bottomNav.selectedItemId = R.id.nav_jobs
    }
}
