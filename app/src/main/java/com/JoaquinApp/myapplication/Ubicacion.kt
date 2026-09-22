package com.JoaquinApp.myapplication

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint

data class Ubicacion(
    val id: String = "",
    val posicion: GeoPoint? = null,
    val fecha: Timestamp? = null
)
