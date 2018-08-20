package lv.mciekurs.weatherlogger

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.single_layout_main.view.*
import java.time.LocalDateTime
import java.util.*

class RecyclerViewAdapter (private val list: MutableList<String>): RecyclerView.Adapter<CustomViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
        val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.single_layout_main, parent, false)
        return CustomViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        holder.view.textView_date.text = list[position]
    }

    override fun getItemCount() = list.size

}

class CustomViewHolder(val view: View): RecyclerView.ViewHolder(view)