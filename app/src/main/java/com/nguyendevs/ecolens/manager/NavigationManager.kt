package com.nguyendevs.ecolens.manager

import android.view.View
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * Class quản lý navigation giữa các màn hình
 */
class NavigationManager(
    private val searchBarContainer: View,
    private val fabSpeak: FloatingActionButton,
    private val homeContainer: View,
    private val historyContainer: View,
    private val myGardenContainer: View,
    private val settingsContainer: View
) {

    fun showHomeScreen() {
        searchBarContainer.visibility = View.VISIBLE
        fabSpeak.visibility = View.GONE
        homeContainer.visibility = View.VISIBLE
        historyContainer.visibility = View.GONE
        myGardenContainer.visibility = View.GONE
        settingsContainer.visibility = View.GONE
    }

    fun showHistoryScreen() {
        searchBarContainer.visibility = View.GONE
        fabSpeak.visibility = View.GONE
        homeContainer.visibility = View.GONE
        historyContainer.visibility = View.VISIBLE
        myGardenContainer.visibility = View.GONE
        settingsContainer.visibility = View.GONE
    }

    fun showMyGardenScreen() {
        searchBarContainer.visibility = View.GONE
        fabSpeak.visibility = View.GONE
        homeContainer.visibility = View.GONE
        historyContainer.visibility = View.GONE
        myGardenContainer.visibility = View.VISIBLE
        settingsContainer.visibility = View.GONE
    }

    fun showSettingsScreen() {
        searchBarContainer.visibility = View.GONE
        fabSpeak.visibility = View.GONE
        homeContainer.visibility = View.GONE
        historyContainer.visibility = View.GONE
        myGardenContainer.visibility = View.GONE
        settingsContainer.visibility = View.VISIBLE
    }
}