package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.FakeAndroidDataSource
import com.udacity.project4.locationreminders.data.local.RemindersDatabase
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest : KoinTest {

    //TODO: test the navigation of the fragments.
    //TODO: test the displayed data on the UI.
    //TODO: add testing for the error messages.

    private lateinit var database: RemindersDatabase
    private val dataSource: FakeAndroidDataSource by inject()

    private val testModules = module {
        viewModel {
            RemindersListViewModel(
                get(),
                get() as FakeAndroidDataSource
            )
        }
        single {
            SaveReminderViewModel(
                get(),
                get() as FakeAndroidDataSource
            )
        }
        single {
            database = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                RemindersDatabase::class.java
            ).allowMainThreadQueries().build()
        }
        single { FakeAndroidDataSource() }
        single { database.reminderDao() }
    }

    @Before
    fun setupFragment() {
        stopKoin()
        startKoin {
            androidContext(ApplicationProvider.getApplicationContext<Application>())
            modules(listOf(testModules))
        }
    }

    @After
    fun removeFragment() {
        stopKoin()
    }

    @Test
    fun reminderListFragment_blankFragmentCheck() {
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.Theme_LocationReminder)
        val navController = mock(NavController::class.java)

        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        onView(withId(R.id.noDataTextView)).check(matches(withText("No Data")))
    }

    @Test
    fun reminderListFragment_filledFragmentCheck() = runBlockingTest {
        val reminder = ReminderDTO("Hello", "Hello World!", "World", 55.05, -05.55)
        dataSource.saveReminder(reminder)

        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.Theme_LocationReminder)
        val navController = mock(NavController::class.java)

        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        onView(withText("Hello")).check(matches(isDisplayed()))
    }

    @Test
    fun reminderListFragment_RedirectionToSaveReminderFragmentCheck() {
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.Theme_LocationReminder)
        val navController = mock(NavController::class.java)

        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        onView(withId(R.id.addReminderFAB)).perform(click())

        verify(navController).navigate(
            ReminderListFragmentDirections.toSaveReminder()
        )
    }
}