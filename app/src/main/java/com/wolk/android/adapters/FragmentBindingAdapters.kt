package com.wolk.android.adapters

import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.fragment.app.Fragment
import com.wolk.android.ct.TCNProximity

/**
 * Binding adapters that work with a fragment instance.
 */
class FragmentBindingAdapters constructor(val fragment: Fragment) {

}

@BindingAdapter("tcn")
fun TextView.setTCNString(tcn: TCNProximity?) {
    tcn?.let {
        text = tcn.publicKey.toString()
    }
}

@BindingAdapter("timestamp")
fun TextView.setTCNTimesamp(tcn: TCNProximity?) {
    tcn?.let {
        text = tcn.timestamp.toString()
    }
}