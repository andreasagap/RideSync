package gr.andreasagap.moto.communication.presentation.adapters

import android.annotation.SuppressLint
import android.net.wifi.p2p.WifiP2pDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import gr.andreasagap.moto.communication.R

interface ListDeviceActions{
    fun onClick(position: Int)
}
class ListDeviceAdapter(private var list: List<WifiP2pDevice>, private val callBack: ListDeviceActions) :
    RecyclerView.Adapter<ListDeviceAdapter.MyViewHolder>() {


    inner class MyViewHolder internal constructor(view: View) : RecyclerView.ViewHolder(view) {
        internal val title: AppCompatTextView

        init {
            title = view.findViewById(R.id.title)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device_list, parent, false)
        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.title.text = list[position].deviceName
        holder.itemView.setOnClickListener{
            callBack.onClick(position)
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(arrayList: List<WifiP2pDevice>) {
        list = arrayList
        notifyDataSetChanged()
    }
}