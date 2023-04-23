package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

	private lateinit var database: RemindersDatabase

	@get:Rule
	var instantExecutorRule = InstantTaskExecutorRule()

	private val reminderDTODataSample = ReminderDTO(
		title = "title",
		description = "description",
		location = "location",
		latitude = 0.0,
		longitude = 0.0,
		id = "0900"
	)

	@Before
	fun initDb() {
		// using an in-memory database because the information stored here disappears when the
		// process is killed
		database = Room.inMemoryDatabaseBuilder(
			ApplicationProvider.getApplicationContext(),
			RemindersDatabase::class.java
		).build()
	}

	@After
	fun closeDb() = database.close()

	@Test
	fun saveReminder() {
		runBlockingTest {
			// GIVEN - save a reminder to DB
			database.reminderDao().saveReminder(reminderDTODataSample)

			// WHEN - Get the reminder by id from the DB
			val result = database.reminderDao().getReminderById(reminderDTODataSample.id)

			// THEN - The result data contains the expected values
			assertThat(result as ReminderDTO, notNullValue())
			assertThat(result.title, `is`(reminderDTODataSample.title))
			assertThat(result.description, `is`(reminderDTODataSample.description))
			assertThat(result.location, `is`(reminderDTODataSample.location))
			assertThat(result.latitude, `is`(reminderDTODataSample.latitude))
			assertThat(result.longitude, `is`(reminderDTODataSample.longitude))
			assertThat(result.id, `is`(reminderDTODataSample.id))
		}
	}

	@Test
	fun getReminders() {
		runBlockingTest {
			// GIVEN - save 2 reminder to DB
			database.reminderDao().saveReminder(reminderDTODataSample)
			database.reminderDao().saveReminder(reminderDTODataSample.copy(id = "0901"))

			// WHEN - Get reminder list from the DB
			val result = database.reminderDao().getReminders()

			// THEN - The result data is 2 items and each items is correct with inserted data.
			assertThat(result.size, `is`(2))
			assertThat(result[0], `is`(reminderDTODataSample))
			assertThat(result[1], `is`(reminderDTODataSample.copy(id = "0901")))
		}
	}

	@Test
	fun deleteAllReminders() {
		runBlockingTest {
			// GIVEN - save 2 reminder to DB
			database.reminderDao().saveReminder(reminderDTODataSample)
			database.reminderDao().saveReminder(reminderDTODataSample.copy(id = "0901"))

			// WHEN - Delete all reminders from DB then get reminder by id
			database.reminderDao().deleteAllReminders()
			val result = database.reminderDao().getReminderById(reminderDTODataSample.id)

			// THEN - The result data is null because there's no reminder.
			assertThat(result, nullValue())
		}
	}
}
