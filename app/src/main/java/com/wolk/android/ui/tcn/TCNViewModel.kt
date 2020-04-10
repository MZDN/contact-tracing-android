package com.wolk.android.ui.tcn

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.PagedList
import androidx.paging.toLiveData
import com.wolk.android.R
import com.wolk.android.tcn.TCNProximity
import com.wolk.android.tcn.TCNProximityDAO
import com.wolk.android.tcn.TCNUser


class TCNViewModel(contactEventDAO: TCNProximityDAO, application: Application) : AndroidViewModel(application) {

    val contactEvents: LiveData<PagedList<TCNProximity>> =

        contactEventDAO.pagedAllSortedByDescTimestamp.toLiveData(pageSize = 50)

    var isContactEventLoggingEnabled = MutableLiveData<Boolean>().apply {
        val isEnabled = application.getSharedPreferences(
            application.getString(R.string.preference_file_key),
            Context.MODE_PRIVATE
        ).getBoolean(
            application.getString(R.string.preference_is_contact_event_logging_enabled),
            false
        )
        value = isEnabled
    }

}