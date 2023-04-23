package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

	private lateinit var reminderLocalRepository: RemindersLocalRepository
	private lateinit var database: RemindersDatabase
	private val reminderDTODataSample = ReminderDTO(
		title = "title",
		description = "description",
		location = "location",
		latitude = 0.0,
		longitude = 0.0,
		id = "0900"
	)

	// Executes each task synchronously using Architecture Components.
	@get:Rule
	var instantExecutorRule = InstantTaskExecutorRule()

	@Before
	fun setup() {
		// using an in-memory database for testing, since it doesn't survive killing the process
		database = Room.inMemoryDatabaseBuilder(
			ApplicationProvider.getApplicationContext(),
			RemindersDatabase::class.java
		)
			.allowMainThreadQueries()
			.build()

		reminderLocalRepository =
			RemindersLocalRepository(
				database.reminderDao(),
				Dispatchers.Main
			)
	}

	@After
	fun cleanUp() {
		database.close()
	}

	@Test
	fun saveReminder_getReminder() {
		runBlocking {
			// GIVEN - a new reminder saved in the database
			reminderLocalRepository.saveReminder(reminderDTODataSample)

			// WHEN  - Reminder retrieved by ID
			val result = reminderLocalRepository.getReminder(reminderDTODataSample.id) as Result.Success

			// THEN - Same reminder is returned
			assertThat(result.data.id, `is`("0900"))
			assertThat(result.data.title, `is`("title"))
		}
	}

	@Test
	fun save2Reminders_getReminderList() {
		runBlocking {
			// GIVEN - a new reminder saved in the database
			reminderLocalRepository.saveReminder(reminderDTODataSample)
			reminderLocalRepository.saveReminder(reminderDTODataSample.copy(id = "0901"))

			// WHEN  - Reminder list retrieved from DB
			val result = reminderLocalRepository.getReminders() as Result.Success

			// THEN - Same reminders is returned
			assertThat(result.data[0].id, `is`("0900"))
			assertThat(result.data[1].id, `is`("0901"))
		}
	}

	@Test
	fun save2Reminders_deleteAllReminders() {
		runBlocking {
			// GIVEN - 2 new reminder saved in the database then delete all is execute.
			reminderLocalRepository.saveReminder(reminderDTODataSample)
			reminderLocalRepository.saveReminder(reminderDTODataSample.copy(id = "0901"))
			reminderLocalRepository.deleteAllReminders()

			// WHEN  - Get a reminder by id from DB
			val result = reminderLocalRepository.getReminder(reminderDTODataSample.id) as Result.Error

			// THEN - The message error is throws
			assertThat(result.message, `is`(notNullValue()))
		}
	}
}