package com.udacity.project4

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PointOfInterest
import com.udacity.project4.authentication.AuthenticationViewModel
import com.udacity.project4.locationreminders.ReminderViewModel
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get

@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest : AutoCloseKoinTest() {

    // Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var activity: RemindersActivity

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val activityTestRule = ActivityTestRule(RemindersActivity::class.java)

    private lateinit var repository: ReminderDataSource
    private lateinit var viewModel: SaveReminderViewModel
    private lateinit var appContext: Application

    @get:Rule
    var activityScenarioRule = ActivityScenarioRule(RemindersActivity::class.java)

    private val dataBindingIdlingResource = DataBindingIdlingResource()

    @Before
    fun init() {
        stopKoin() //stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel { AuthenticationViewModel() }
            viewModel { ReminderViewModel() }
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single<ReminderDataSource> { RemindersLocalRepository(get()) }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            androidContext(appContext)
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()
        activity = activityTestRule.activity
        viewModel = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    @Before
    fun setupActivity() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    @After
    fun removeActivity() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun remindersActivity_loggedUserFlowCheck() {
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        onView(withId(R.id.noDataTextView)).check(matches(withText("No Data")))
        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.selectLocation)).check(matches(isDisplayed()))
        onView(withId(R.id.selectLocation)).perform(click())
        onView(withId(R.id.mbt_save_location)).check(matches(isDisplayed()))
        onView(withId(R.id.map)).perform(longClick())
        onView(withId(R.id.mbt_save_location)).perform(click())
        onView(withId(R.id.reminderTitle)).check(matches(isDisplayed()))
        onView(withId(R.id.reminderTitle)).perform(typeText("Sample Reminder Title"), closeSoftKeyboard())
        onView(withId(R.id.reminderDescription)).perform(typeText("Sample Reminder Description"), closeSoftKeyboard())
        onView(withId(R.id.saveReminder)).perform(click())

        activityScenario.close()
    }

    @Test
    fun addingAReminder_andStartReminderDescriptionActivity() {
        val typingTitle = "Title"
        val typingDescription = "Description"
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // Verify: no data is shown
        onView(withId(R.id.noDataTextView)).check(matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))

        // Click add new task
        onView(withId(R.id.addReminderFAB)).perform(ViewActions.click())

        // Type data
        onView(withId(R.id.reminderTitle)).perform(ViewActions.typeText(typingTitle))
        onView(withId(R.id.reminderDescription)).perform(ViewActions.typeText(typingDescription))
        Espresso.closeSoftKeyboard()

        // Select location
        onView(withId(R.id.selectLocation)).perform(ViewActions.click())

        // Click any position in the map
        onView(withId(R.id.map)).perform(ViewActions.longClick())
        runBlocking {
            delay(1000)
        }
        // Save location
        onView(withId(R.id.mbt_save_location)).perform(ViewActions.click())

        // Get selected location
        val selectedLocation = viewModel.reminderSelectedLocationStr.value

        // Save
        onView(withId(R.id.saveReminder)).perform(ViewActions.click())

        // Verify: One item is created
        onView(withText(typingTitle)).check(matches(isDisplayed()))
        onView(withText(typingDescription)).check(matches(isDisplayed()))
        onView(withText(selectedLocation)).check(matches(isDisplayed()))

        onView(withId(R.id.addReminderFAB)).perform(ViewActions.click())
        // Verify snack is shown correctly!
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText(R.string.geofence_added)))

        // Click on that item
        onView(withText(typingTitle)).perform(ViewActions.click())

        // Verify detail screen is correct!
        onView(withText(typingTitle)).check(matches(isDisplayed()))
        onView(withText(typingDescription)).check(matches(isDisplayed()))

        // Make sure the activity is closed before resetting the db:
        activityScenario.close()
    }

    @Test
    fun addReminder_EmptyTitle_verifyShowErrorMessage() {
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // Verify: no data is shown
        onView(withId(R.id.noDataTextView)).check(matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))

        // Click add new task
        onView(withId(R.id.addReminderFAB)).perform(ViewActions.click())

        // Set location manually
        viewModel.selectedPOI.postValue(PointOfInterest(LatLng(54.67575865, 2.75759986), "", "Somewhere"))

        // Typing description
        onView(withId(R.id.reminderDescription)).perform(ViewActions.typeText("Description"))
        Espresso.closeSoftKeyboard()

        // Save reminder
        onView(withId(R.id.saveReminder)).perform(ViewActions.click())

        // Verify error message is shown
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText(R.string.err_enter_title)))

        // Make sure the activity is closed before resetting the db:
        activityScenario.close()
    }

    @Test
    fun addReminder_EmptyDescription_verifyShowErrorMessage() {
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // Verify: no data is shown
        onView(withId(R.id.noDataTextView)).check(matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))

        // Click add new task
        onView(withId(R.id.addReminderFAB)).perform(ViewActions.click())

        // Set location manually
        viewModel.selectedPOI.postValue(PointOfInterest(LatLng(54.67575865, 2.75759986), "", "Somewhere"))

        // Typing description
        onView(withId(R.id.reminderTitle)).perform(ViewActions.typeText("Title"))
        Espresso.closeSoftKeyboard()

        // Save reminder
        onView(withId(R.id.saveReminder)).perform(ViewActions.click())

        // Verify error message is shown
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText(R.string.err_enter_description)))

        // Make sure the activity is closed before resetting the db:
        activityScenario.close()
    }

    @Test
    fun addReminder_EmptyLocation_verifyShowErrorMessage() {
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // Verify: no data is shown
        onView(withId(R.id.noDataTextView)).check(matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))

        // Click add new task
        onView(withId(R.id.addReminderFAB)).perform(ViewActions.click())

        // Typing title & description
        onView(withId(R.id.reminderTitle)).perform(ViewActions.typeText("Title"))
        onView(withId(R.id.reminderDescription)).perform(ViewActions.typeText("Description"))
        Espresso.closeSoftKeyboard()

        // Save reminder
        onView(withId(R.id.saveReminder)).perform(ViewActions.click())

        // Verify error message is shown
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText(R.string.err_select_location)))

        // Make sure the activity is closed before resetting the db:
        activityScenario.close()
    }
}
