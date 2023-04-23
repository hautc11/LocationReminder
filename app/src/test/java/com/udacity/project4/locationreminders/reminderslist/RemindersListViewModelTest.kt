package com.udacity.project4.locationreminders.reminderslist

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.nullValue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

	private lateinit var remindersListViewModel: RemindersListViewModel
	private val fakeDataSource = FakeDataSource()

	@get:Rule
	var mainCoroutineRule = MainCoroutineRule()

	@Before
	fun initViewModel() {
		remindersListViewModel = RemindersListViewModel(
			ApplicationProvider.getApplicationContext(),
			fakeDataSource
		)
	}

	@After
	fun tearDown() {
		stopKoin()
	}

	@Test
	fun test_loadReminderListSuccess_remindersListMustBeSameDataAsLocal() {
		val expectedReminders = listOf(createReminderItem())

		runBlockingTest {
			//Given
			fakeDataSource.saveReminder(createReminderDTO())

			//When
			remindersListViewModel.loadReminders()

			//Then
			assertThat(remindersListViewModel.remindersList.value, `is`(expectedReminders))
		}
	}

	@Test
	fun test_loadReminderListFailed_snackBarValueIsExceptionMessage() {
		//Given
		fakeDataSource.setReturnError(true)

		//When
		remindersListViewModel.loadReminders()

		//Then
		assertThat(remindersListViewModel.showSnackBar.value, `is`("Get reminders exception"))
	}

	@Test
	fun test_loadReminderListNull_showNoDataValueMustBeTrue() {
		//When
		assertThat(remindersListViewModel.remindersList.value, `is`(nullValue()))

		//Given
		remindersListViewModel.loadReminders()

		//Then
		assertThat(remindersListViewModel.showNoData.value, `is`(true))
	}

	@Test
	fun test_loading() {

		mainCoroutineRule.pauseDispatcher()
		remindersListViewModel.loadReminders()

		assertThat(remindersListViewModel.showLoading.value, `is`(true))

		mainCoroutineRule.resumeDispatcher()
		assertThat(remindersListViewModel.showLoading.value, `is`(false))
	}

	private fun createReminderDTO() = ReminderDTO(
		title = "Title",
		description = "Description",
		location = "Location",
		latitude = 0.0,
		longitude = 0.0,
		id = "123"
	)

	private fun createReminderItem() = ReminderDataItem(
		title = "Title",
		description = "Description",
		location = "Location",
		latitude = 0.0,
		longitude = 0.0,
		id = "123"
	)
}