package com.example.cinephile.ui.search

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable
import com.example.cinephile.data.remote.TmdbPerson

class PersonSearchAdapter(
    context: Context,
    private val persons: MutableList<TmdbPerson> = mutableListOf()
) : ArrayAdapter<TmdbPerson>(context, android.R.layout.simple_dropdown_item_1line, persons), Filterable {

    override fun getFilter(): Filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            return FilterResults().apply {
                values = persons
                count = persons.size
            }
        }
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            clear()
            if (results?.values is List<*>) {
                @Suppress("UNCHECKED_CAST")
                addAll(results.values as List<TmdbPerson>)
            }
            notifyDataSetChanged()
        }
    }

    override fun getItem(position: Int): TmdbPerson? = persons.getOrNull(position)

    fun updateData(newPersons: List<TmdbPerson>) {
        persons.clear()
        persons.addAll(newPersons)
        notifyDataSetChanged()
    }

    override fun getCount(): Int = persons.size
    override fun isEnabled(position: Int) = true
    override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
        val view = super.getView(position, convertView, parent)
        val person = getItem(position)
        view.findViewById<android.widget.TextView>(android.R.id.text1)?.text = "${'$'}{person?.name} (${person?.knownForDepartment ?: "-"})"
        return view
    }
}
