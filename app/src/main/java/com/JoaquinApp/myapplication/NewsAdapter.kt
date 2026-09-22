package com.JoaquinApp.myapplication

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NewsAdapter(private val events: List<DisasterEvent>) : RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {

    class NewsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvNewsTitle)
        val tvLink: TextView = view.findViewById(R.id.tvNewsLink)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_news_minimalist, parent, false)
        return NewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val event = events[position]
        
        holder.tvTitle.text = event.title
        
        // El enlace al portal de la NASA u otra fuente oficial
        val url = if (event.sources.isNotEmpty()) event.sources[0].url else ""
        
        holder.tvLink.text = "Ver más en el portal oficial"
        holder.tvLink.setOnClickListener {
            if (url.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                holder.itemView.context.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int = events.size
}
