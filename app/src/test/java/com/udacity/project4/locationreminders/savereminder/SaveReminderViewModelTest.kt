package com.udacity.project4.locationreminders.savereminder

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PointOfInterest
import com.udacity.project4.MainCoroutineRule
import com.udacity.project4.getOrAwaitValue
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.nullValue
import com.udacity.project4.R
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

	private lateinit var saveReminderViewModel: SaveReminderViewModel
	private val fakeDataSource = FakeDataSource()

	@get:Rule
	var mainCoroutineRule = MainCoroutineRule()

	@Before
	fun initViewModel() {
		saveReminderViewModel = SaveReminderViewModel(
			ApplicationProvider.getApplicationContext(),
			fakeDataSource
		)
	}

	@After
	fun tearDown() {
		stopKoin()
	}

	@Test
	fun test_onClearFunction() {
		saveReminderViewModel.reminderTitle.value = "Title"
		saveReminderViewModel.reminderDescription.value = "Description"
		saveReminderViewModel.reminderSelectedLocationStr.value = "SelectedLocation"
		saveReminderViewModel.selectedPOI.value = PointOfInterest(LatLng(0.0, 0.0), "", "")
		saveReminderViewModel.longitude.value = 0.0
		saveReminderViewModel.latitude.value = 0.0

		assertThat(saveReminderViewModel.reminderTitle.getOrAwaitValue(), not(nullValue()))
		assertThat(saveReminderViewModel.reminderDescription.getOrAwaitValue(), not(nullValue()))
		assertThat(saveReminderViewModel.reminderSelectedLocationStr.getOrAwaitValue(), not(nullValue()))
		assertThat(saveReminderViewModel.selectedPOI.getOrAwaitValue(), not(nullValue()))
		assertThat(saveReminderViewModel.longitude.getOrAwaitValue(), not(nullValue()))
		assertThat(saveReminderViewModel.latitude.getOrAwaitValue(), not(nullValue()))

		saveReminderViewModel.onClear()

		assertThat(saveReminderViewModel.reminderTitle.getOrAwaitValue(), `is`(nullValue()))
		assertThat(saveReminderViewModel.reminderDescription.getOrAwaitValue(), `is`(nullValue()))
		assertThat(saveReminderViewModel.reminderSelectedLocationStr.getOrAwaitValue(), `is`(nullValue()))
		assertThat(saveReminderViewModel.selectedPOI.getOrAwaitValue(), `is`(nullValue()))
		assertThat(saveReminderViewModel.longitude.getOrAwaitValue(), `is`(nullValue()))
		assertThat(saveReminderViewModel.latitude.getOrAwaitValue(), `is`(nullValue()))
	}

	@Test
	fun test_validateEnteredData_withTitleNull() {
		val reminderItem = ReminderDataItem(
			title = null,
			description = "des",
			location = "",
			longitude = 0.0,
			latitude = 0.0,
			id = ""
		)

		assertThat(saveReminderViewModel.validateEnteredData(reminderItem), `is`(false))
		assertThat(saveReminderViewModel.showSnackBarInt.value, `is`(R.string.err_enter_title))
	}

	@Test
	fun test_validateEnteredData_withLocationNull() {
		val reminderItem = ReminderDataItem(
			title = "Title",
			description = "des",
			location = null,
			longitude = 0.0,
			latitude = 0.0,
			id = ""
		)

		assertThat(saveReminderViewModel.validateEnteredData(reminderItem), `is`(false))
		assertThat(saveReminderViewModel.showSnackBarInt.value, `is`(R.string.err_select_location))
	}

	@Test
	fun test_validateEnteredData_withTitleAndLocationNotNull() {
		val reminderItem = ReminderDataItem(
			title = "Title",
			description = "des",
			location = "Location",
			longitude = 0.0,
			latitude = 0.0,
			id = ""
		)

		assertThat(saveReminderViewModel.validateEnteredData(reminderItem), `is`(true))
	}

	@Test
	fun test_validateAndSaveReminder_withTitleAndLocationNotNull() {
		runBlockingTest {
			val reminderItem = ReminderDataItem(
				title = "Title",
				description = "des",
				location = "Location",
				longitude = 0.0,
				latitude = 0.0,
				id = "0090"
			)
			saveReminderViewModel.validateAndSaveReminder(reminderItem)
			val result = fakeDataSource.getReminder("0090") as Result.Success

			assertThat(saveReminderViewModel.validateEnteredData(reminderItem), `is`(true))
			assertThat(reminderItem.id, `is`(result.data.id))
		}
	}

	@Test
	fun test_validateAndSaveReminder_withTitleAndLocationNull() {
		runBlockingTest {
			val reminderItem = ReminderDataItem(
				title = null,
				description = "des",
				location = null,
				longitude = 0.0,
				latitude = 0.0,
				id = "0090"
			)

			saveReminderViewModel.validateAndSaveReminder(reminderItem)
			assertThat(saveReminderViewModel.validateEnteredData(reminderItem), `is`(false))


			fakeDataSource.setReturnError(true)
			assertThat((fakeDataSource.getReminder("0090") as Result.Error).message, `is`("Get reminders exception"))
		}
	}

	@Test
	fun test_saveReminder() {
		runBlockingTest {
			val reminderItem = ReminderDataItem(
				title = "title",
				description = "des",
				location = "location",
				longitude = 0.0,
				latitude = 0.0,
				id = "0090"
			)

			saveReminderViewModel.saveReminder(reminderItem)

			assertThat((fakeDataSource.getReminder("0090") as Result.Success).data, `is`(reminderItem.convertToDTO()))
		}
	}

	@Test
	fun test_loading() {
		val reminderItem = ReminderDataItem(
			title = "title",
			description = "des",
			location = "location",
			longitude = 0.0,
			latitude = 0.0,
			id = "0090"
		)

		mainCoroutineRule.pauseDispatcher()
		saveReminderViewModel.saveReminder(reminderItem)

		assertThat(saveReminderViewModel.showLoading.value, `is`(true))

		mainCoroutineRule.resumeDispatcher()
		assertThat(saveReminderViewModel.showLoading.value, `is`(false))
	}
}