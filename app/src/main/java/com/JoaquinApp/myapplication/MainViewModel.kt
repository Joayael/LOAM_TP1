package com.JoaquinApp.myapplication

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore

class MainViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    val ubicaciones = mutableStateListOf<Ubicacion>()

    init {
        fetchUbicaciones()
    }

    private fun fetchUbicaciones() {
        db.collection("Ubicacion")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    ubicaciones.clear()
                    val fetched = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Ubicacion::class.java)?.copy(id = doc.id)
                    }
                    ubicaciones.addAll(fetched)
                }
            }
    }
}
