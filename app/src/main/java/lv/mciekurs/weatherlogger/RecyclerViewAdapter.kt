package lv.mciekurs.weatherlogger

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.single_layout_main.view.*

class RecyclerViewAdapter (private val list: MutableList<WeatherInfo>): RecyclerView.Adapter<CustomViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
        val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.single_layout_main, parent, false)
        return CustomViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        //Need to do force reload to layout after changing format argument in strings.xml file
        holder.view.textView_date.text = holder.view.context.getString(R.string.text_data, list[position].data)
        holder.view.textView_temperature.text = holder.view.context.getString(R.string.text_temperature, list[position].temp)
    }

    override fun getItemCount() = list.size

}

class CustomViewHolder(val view: View): RecyclerView.ViewHolder(view)