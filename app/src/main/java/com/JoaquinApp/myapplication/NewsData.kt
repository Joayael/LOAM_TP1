package com.JoaquinApp.myapplication

data class EonetResponse(
    val events: List<DisasterEvent>
)

data class DisasterEvent(
    val id: String,
    val title: String,
    val categories: List<DisasterCategory>,
    val geometry: List<DisasterGeometry>,
    val sources: List<DisasterSource>
)

data class DisasterCategory(
    val id: String,
    val title: String
)

data class DisasterGeometry(
    val date: String,
    val coordinates: List<Double>
)

data class DisasterSource(
    val id: String,
    val url: String
)
