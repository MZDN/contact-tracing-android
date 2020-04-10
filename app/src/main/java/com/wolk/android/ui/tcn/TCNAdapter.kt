package com.wolk.android.ui.tcn

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.wolk.android.R
import com.wolk.android.databinding.ListItemTCNProximityBinding
import com.wolk.android.tcn.TCNProximity


class TCNAdapter() :
    PagedListAdapter<TCNProximity, TCNAdapter.TCNViewHolder>(
        DIFF_CALLBACK
    ) {

    override fun getItemViewType(position: Int): Int {
        return R.layout.list_item_t_c_n_proximity
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
        val contactEvent: TCNProximity = getItem(position) ?: return
        holder.bind(contactEvent)
    }

    class TCNViewHolder(private val binding: ListItemTCNProximityBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: TCNProximity) {
            binding.apply {
                contactEvent = item
                executePendingBindings()
            }
        }

    }

    companion object {
        private val DIFF_CALLBACK = object :
            DiffUtil.ItemCallback<TCNProximity>() {
            // Contact event details may have changed if reloaded from the database,
            // but ID is fixed.
            override fun areItemsTheSame(
                oldTCN: TCNProximity,
                newTCN: TCNProximity
            ) = oldTCN.publicKey == newTCN.publicKey

            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(
                oldTCN: TCNProximity,
                newTCN: TCNProximity
            ) = oldTCN == newTCN
        }
    }

}
