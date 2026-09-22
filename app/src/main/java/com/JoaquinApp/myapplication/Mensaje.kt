package com.JoaquinApp.myapplication

import com.google.firebase.Timestamp

data class Mensaje(
    val texto: String = "",
    val momento: Timestamp? = null,
    val esUsuario: Boolean = true
)
