package com.JoaquinApp.myapplication

import android.content.Context
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ChatHandler(private val context: Context, private val rootView: View) {

    private val etMessage: EditText? = rootView.findViewById(R.id.etMessage)
    private val btnSend: View? = rootView.findViewById(R.id.btnSend)
    private val rvChat: RecyclerView? = rootView.findViewById(R.id.chatRecyclerView)
    
    private val db = FirebaseFirestore.getInstance()
    private val messages = mutableListOf<Mensaje>()
    private val adapter = ChatAdapter(messages)

    fun setupChat() {
        rvChat?.layoutManager = LinearLayoutManager(context).apply {
            stackFromEnd = true
        }
        rvChat?.adapter = adapter

        btnSend?.setOnClickListener {
            sendMessage()
        }

        listenForMessages()
    }

    private fun listenForMessages() {
        db.collection("Mensaje")
            .orderBy("momento", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val newMessages = snapshot.toObjects(Mensaje::class.java)
                    adapter.updateMessages(newMessages)
                    if (messages.isNotEmpty()) {
                        rvChat?.scrollToPosition(messages.size - 1)
                    }
                }
            }
    }

    private fun sendMessage() {
        val text = etMessage?.text.toString().trim()
        
        if (text.isEmpty()) {
            return
        }

        val mensaje = Mensaje(
            texto = text,
            momento = Timestamp.now(),
            esUsuario = true,
        )

        db.collection("Mensaje")
            .add(mensaje)
            .addOnSuccessListener {
                etMessage?.text?.clear()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error al enviar mensaje", Toast.LENGTH_SHORT).show()
            }
    }
}
