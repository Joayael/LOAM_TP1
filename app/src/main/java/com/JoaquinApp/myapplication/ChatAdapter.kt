package com.JoaquinApp.myapplication

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class ChatAdapter(private val messages: MutableList<Mensaje>) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMessage: TextView = view.findViewById(R.id.tvChatMessage)
        val bubbleCard: MaterialCardView = view.findViewById(R.id.bubbleCard)
        val parentLayout: LinearLayout = view.findViewById(R.id.bubbleParentLayout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_bubble, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messages[position]
        val context = holder.itemView.context
        
        holder.tvMessage.text = message.texto

        if (message.esUsuario) {
            // Estilo para el usuario (derecha, azul suave)
            holder.parentLayout.gravity = Gravity.END
            holder.bubbleCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.chat_bubble_user))
            holder.tvMessage.setTextColor(ContextCompat.getColor(context, R.color.text_main))
        } else {
            // Estilo para el asistente (izquierda, gris suave)
            holder.parentLayout.gravity = Gravity.START
            holder.bubbleCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.chat_bubble_other))
            holder.tvMessage.setTextColor(ContextCompat.getColor(context, R.color.text_main))
        }
    }

    override fun getItemCount(): Int = messages.size

    fun updateMessages(newMessages: List<Mensaje>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }
}
