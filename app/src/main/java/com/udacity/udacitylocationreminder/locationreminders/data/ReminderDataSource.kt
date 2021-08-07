package com.udacity.udacitylocationreminder.locationreminders.data

import com.udacity.udacitylocationreminder.locationreminders.data.dto.ReminderDTO
import com.udacity.udacitylocationreminder.locationreminders.data.dto.Result

/**
 * Main entry point for accessing reminders data.
 */
interface ReminderDataSource {
    suspend fun getReminders(): Result<List<ReminderDTO>>
    suspend fun saveReminder(reminder: ReminderDTO)
    suspend fun getReminder(id: String): Result<ReminderDTO>
    suspend fun deleteAllReminders()
}