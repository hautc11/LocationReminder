package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource : ReminderDataSource {

    var remindersData: MutableList<ReminderDTO> = mutableListOf()

    private var shouldReturnError = false

    fun setReturnError(value: Boolean) {
        shouldReturnError = value
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        return if (shouldReturnError) {
            Result.Error("Get reminders exception")
        } else {
            Result.Success(remindersData)
        }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        remindersData.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        return if (shouldReturnError) {
            Result.Error("Reminder not found!")
        } else {
            val availableReminder = remindersData.first { it.id == id }
            Result.Success(availableReminder)
        }
    }

    override suspend fun deleteAllReminders() {
        remindersData.clear()
    }


}