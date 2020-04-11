package com.wolk.android.ui.ct

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.wolk.android.R
import com.wolk.android.ct.RollingProximityIdentifier
import com.wolk.android.databinding.ListItemRollingProximityIdentifierBinding


class CTAdapter() :
    PagedListAdapter<RollingProximityIdentifier, CTAdapter.TCNViewHolder>(
        DIFF_CALLBACK
    ) {

    override fun getItemViewType(position: Int): Int {
        return R.layout.list_item_rolling_proximity_identifier
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TCNViewHolder {
        return TCNViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                viewType, parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: TCNViewHolder, position: Int) {
        val contactEvent: RollingProximityIdentifier = getItem(position) ?: return
        holder.bind(contactEvent)
    }

    class TCNViewHolder(private val binding: ListItemRollingProximityIdentifierBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: RollingProximityIdentifier) {
            binding.apply {
                rollingProximityIdentifier = item
                executePendingBindings()
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object :
            DiffUtil.ItemCallback<RollingProximityIdentifier>() {
            // Contact event details may have changed if reloaded from the database,
            // but ID is fixed.
            override fun areItemsTheSame(
                oldRPI: RollingProximityIdentifier,
                newRPI: RollingProximityIdentifier
            ) = oldRPI.rpi == newRPI.rpi

            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(
                oldRPI: RollingProximityIdentifier,
                newRPI: RollingProximityIdentifier
            ) = oldRPI == newRPI
        }
    }

}
