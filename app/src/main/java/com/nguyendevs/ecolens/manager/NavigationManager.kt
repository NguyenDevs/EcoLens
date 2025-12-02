package com.nguyendevs.ecolens.manager

import android.view.View
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * Class quản lý navigation giữa các màn hình
 */
class NavigationManager(
    private val fabSearch: FloatingActionButton,
    private val fabSpeak: FloatingActionButton,
    private val homeContainer: View,
    private val historyContainer: View,
    private val myGardenContainer: View,
    private val settingsContainer: View
) {

    fun showHomeScreen() {
        fabSearch.visibility = View.VISIBLE
        fabSpeak.visibility = View.VISIBLE
        homeContainer.visibility = View.VISIBLE
        historyContainer.visibility = View.GONE
        myGardenContainer.visibility = View.GONE
        settingsContainer.visibility = View.GONE
    }

    fun showHistoryScreen() {
        fabSearch.visibility = View.GONE
        fabSpeak.visibility = View.GONE
        homeContainer.visibility = View.GONE
        historyContainer.visibility = View.VISIBLE
        myGardenContainer.visibility = View.GONE
        settingsContainer.visibility = View.GONE
    }

    fun showMyGardenScreen() {
        fabSearch.visibility = View.GONE
        fabSpeak.visibility = View.GONE
        homeContainer.visibility = View.GONE
        historyContainer.visibility = View.GONE
        myGardenContainer.visibility = View.VISIBLE
        settingsContainer.visibility = View.GONE
    }

    fun showSettingsScreen() {
        fabSearch.visibility = View.GONE
        fabSpeak.visibility = View.GONE
        homeContainer.visibility = View.GONE
        historyContainer.visibility = View.GONE
        myGardenContainer.visibility = View.GONE
        settingsContainer.visibility = View.VISIBLE
    }
}