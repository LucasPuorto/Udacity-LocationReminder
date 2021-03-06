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
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var remindersLocalRepository: RemindersLocalRepository
    private lateinit var database: RemindersDatabase

    private val reminder = ReminderDTO("Hello", "Hello World!", "World", 55.05, -05.55, "1234")

    @Before
    fun repositorySetup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).allowMainThreadQueries().build()

        remindersLocalRepository = RemindersLocalRepository(database.reminderDao(), Dispatchers.Main)
    }

    @After
    fun repositoryClean() {
        database.close()
    }

    @Test
    fun repository_saveAndGetCheck() = runBlocking {
        remindersLocalRepository.saveReminder(reminder)

        val result = remindersLocalRepository.getReminder(reminder.id)
        result as Result.Success

        MatcherAssert.assertThat(result.data.id, `is`(reminder.id))
        MatcherAssert.assertThat(result.data.title, `is`(reminder.title))
        MatcherAssert.assertThat(result.data.description, `is`(reminder.description))
        MatcherAssert.assertThat(result.data.location, `is`(reminder.location))
        MatcherAssert.assertThat(result.data.latitude, `is`(reminder.latitude))
        MatcherAssert.assertThat(result.data.longitude, `is`(reminder.longitude))
    }

    @Test
    fun saveReminder_getReminderList() = runBlocking {
        remindersLocalRepository.saveReminder(reminder)
        val reminderList = remindersLocalRepository.getReminders()
        reminderList as Result.Success
        MatcherAssert.assertThat(reminderList.data.size, `is`(1))
    }

    @Test
    fun saveReminder_getReminder_thenClearReminderList() = runBlocking {
        remindersLocalRepository.saveReminder(reminder)
        val reminderList = remindersLocalRepository.getReminders()
        remindersLocalRepository.deleteAllReminders()
        val updatedReminderList = remindersLocalRepository.getReminders()

        reminderList as Result.Success
        updatedReminderList as Result.Success

        MatcherAssert.assertThat(reminderList.data.size, `is`(1))
        MatcherAssert.assertThat(updatedReminderList.data.size, `is`(0))
    }

    @Test
    fun getReminderByNonExistingId_shouldReturnError() = runBlocking {
        // WHEN
        val result = remindersLocalRepository.getReminder("1")

        // THEN
        MatcherAssert.assertThat(result, `is`(not(Result.Success(reminder))))

        result as Result.Error

        MatcherAssert.assertThat(result.message, `is`("Reminder not found!"))
    }
}