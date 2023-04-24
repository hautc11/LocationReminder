package com.udacity.project4.locationreminders.reminderslist

import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.koin.test.AutoCloseKoinTest
import org.koin.test.KoinTest
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest : KoinTest {

	private val navController = mock(NavController::class.java)

	// Test the navigation of the fragments.
	@Test
	fun testNavigationToSaveFragment() {
		val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

		scenario.onFragment {
			Navigation.setViewNavController(it.view!!, navController)
		}

		onView(withId(R.id.addReminderFAB)).perform(click())

		verify(navController).navigate(
			ReminderListFragmentDirections.toSaveReminder()
		)
	}

	// Test the displayed data on the UI.
	@Test
	fun testDisplayOfNoDataTextView() {
		val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

		scenario.onFragment {
			Navigation.setViewNavController(it.view!!, navController)
		}

		onView(withId(R.id.noDataTextView)).check(matches(withText("No Data")))
	}
}