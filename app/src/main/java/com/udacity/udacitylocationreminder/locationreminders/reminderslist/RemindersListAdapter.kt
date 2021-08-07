package com.udacity.udacitylocationreminder.locationreminders.reminderslist

import com.udacity.udacitylocationreminder.R
import com.udacity.udacitylocationreminder.base.BaseRecyclerViewAdapter


//Use data binding to show the reminder on the item
class RemindersListAdapter(callBack: (selectedReminder: ReminderDataItem) -> Unit) :
    BaseRecyclerViewAdapter<ReminderDataItem>(callBack) {
    override fun getLayoutRes(viewType: Int) = R.layout.it_reminder
}