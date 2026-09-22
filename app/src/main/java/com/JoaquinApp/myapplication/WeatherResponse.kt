package com.JoaquinApp.myapplication

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    val main: Main,
    val weather: List<WeatherDetail>,
    val wind: Wind
)

data class Main(
    val temp: Double,
    @SerializedName("feels_like") val feelsLike: Double,
    val humidity: Int
)

data class WeatherDetail(
    val description: String,
    val icon: String
)

data class Wind(
    val speed: Double
)
