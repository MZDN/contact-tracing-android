package com.wolk.android.ui.ct

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import com.wolk.android.R
import com.wolk.android.ct.CTDatabase
import com.wolk.android.ct.RollingProximityIdentifier
import com.wolk.android.ct.RollingProximityIdentifierDAO
import com.wolk.android.databinding.FragmentRollingProximityIdentifierBinding


class CTFragment : Fragment() {

    private lateinit var contactEventsViewModel: CTViewModel
    private lateinit var binding: FragmentRollingProximityIdentifierBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val context = context ?: return null

        val database = CTDatabase.getInstance(context)
        val viewModel: CTViewModel by viewModels(factoryProducer = {
            CTViewModelFactory(database.rollingProximityIdentifierDAO(), context.applicationContext as Application)
        })
        contactEventsViewModel = viewModel

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_rolling_proximity_identifier, container, false)
        binding.lifecycleOwner = this
        val adapter = CTAdapter()
        binding.contactEventsRecyclerview.adapter = adapter
        binding.contactEventsRecyclerview.addItemDecoration(
            DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        )
        viewModel.contactEvents.observe(viewLifecycleOwner, Observer { adapter.submitList(it) })

        setHasOptionsMenu(true)

        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        val isContactEventLoggingEnabled =
            contactEventsViewModel.isContactEventLoggingEnabled.value ?: false
        if (isContactEventLoggingEnabled) {
            inflater.inflate(R.menu.menu_contact_events_stop, menu)
        } else {
            inflater.inflate(R.menu.menu_contact_events_start, menu)
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.clear -> {
                CTDatabase.databaseWriteExecutor.execute {
                    val dao: RollingProximityIdentifierDAO = CTDatabase.getInstance(requireActivity()).rollingProximityIdentifierDAO()
                    dao.deleteAll()
                }
            }
            R.id.start_logging -> {
                setContactEventLogging(true)
                activity?.invalidateOptionsMenu()
            }
            R.id.stop_logging -> {
                setContactEventLogging(false)
                activity?.invalidateOptionsMenu()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setContactEventLogging(enabled: Boolean) {

        contactEventsViewModel.isContactEventLoggingEnabled.value = enabled

        val application = context?.applicationContext ?: return
        val sharedPref = application.getSharedPreferences(
            application.getString(R.string.preference_file_key), Context.MODE_PRIVATE
        ) ?: return
        with(sharedPref.edit()) {
            putBoolean(
                application.getString(R.string.preference_is_contact_event_logging_enabled),
                enabled
            )
            commit()
        }
    }

}
