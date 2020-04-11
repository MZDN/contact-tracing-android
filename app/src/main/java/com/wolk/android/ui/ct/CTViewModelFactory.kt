package com.wolk.android.ui.ct

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wolk.android.ct.RollingProximityIdentifierDAO

class CTViewModelFactory(
    private val rollingProximityIdentifierDAO: RollingProximityIdentifierDAO,
    private val application: Application
) : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CTViewModel(rollingProximityIdentifierDAO, application) as T
    }
}
