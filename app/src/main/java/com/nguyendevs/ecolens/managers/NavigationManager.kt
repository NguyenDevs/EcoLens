package com.nguyendevs.ecolens.managers

import android.view.View
import android.view.ViewGroup
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.google.android.material.floatingactionbutton.FloatingActionButton

class NavigationManager(
    private val searchBarContainer: View,
    private val fabSpeak: FloatingActionButton,
    private val homeContainer: View,
    private val historyContainer: View,
    private val myGardenContainer: View,
    private val settingsContainer: View
) {

    private fun beginTransition() {
        val root = searchBarContainer.parent as? ViewGroup ?: return

        val fade = Fade()
        fade.duration = 150

        TransitionManager.beginDelayedTransition(root, fade)
    }

    fun showHomeScreen(shouldShowFab: Boolean) {
        beginTransition()

        searchBarContainer.visibility = View.VISIBLE
        fabSpeak.visibility = if (shouldShowFab) View.VISIBLE else View.GONE
        homeContainer.visibility = View.VISIBLE
        historyContainer.visibility = View.GONE
        myGardenContainer.visibility = View.GONE
        settingsContainer.visibility = View.GONE
    }

    fun showHistoryScreen() {
        beginTransition()

        searchBarContainer.visibility = View.GONE
        fabSpeak.visibility = View.GONE
        homeContainer.visibility = View.GONE
        historyContainer.visibility = View.VISIBLE
        myGardenContainer.visibility = View.GONE
        settingsContainer.visibility = View.GONE
    }

    fun showMyGardenScreen() {
        beginTransition()

        searchBarContainer.visibility = View.GONE
        fabSpeak.visibility = View.GONE
        homeContainer.visibility = View.GONE
        historyContainer.visibility = View.GONE
        myGardenContainer.visibility = View.VISIBLE
        settingsContainer.visibility = View.GONE
    }

    fun showSettingsScreen() {
        beginTransition()

        searchBarContainer.visibility = View.GONE
        fabSpeak.visibility = View.GONE
        homeContainer.visibility = View.GONE
        historyContainer.visibility = View.GONE
        myGardenContainer.visibility = View.GONE
        settingsContainer.visibility = View.VISIBLE
    }
}