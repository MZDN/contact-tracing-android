package com.wolk.android.ui.tcn

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wolk.android.tcn.TCNProximityDAO

class TCNViewModelFactory(
    private val contactEventDAO: TCNProximityDAO,
    private val application: Application
) : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TCNViewModel(contactEventDAO, application) as T
    }
}
