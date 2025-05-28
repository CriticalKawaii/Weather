package com.example.weather.data.model

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    @SerializedName("coord")
    val coordinates: Coordinates,

    @SerializedName("weather")
    val weatherList: List<Weather>,

    @SerializedName("base")
    val base: String,

    @SerializedName("main")
    val mainInfo: MainInfo,

    @SerializedName("visibility")
    val visibility: Int,

    @SerializedName("wind")
    val wind: Wind,

    @SerializedName("clouds")
    val clouds: Clouds,

    @SerializedName("dt")
    val timestamp: Long,

    @SerializedName("sys")
    val systemInfo: SystemInfo,

    @SerializedName("timezone")
    val timezone: Int,

    @SerializedName("id")
    val cityId: Int,

    @SerializedName("name")
    val cityName: String,

    @SerializedName("cod")
    val responseCode: Int
)

data class Coordinates(
    @SerializedName("lon")
    val longitude: Double,

    @SerializedName("lat")
    val latitude: Double
)

data class Weather(
    @SerializedName("id")
    val id: Int,

    @SerializedName("main")
    val main: String,

    @SerializedName("description")
    val description: String,

    @SerializedName("icon")
    val icon: String
) {
    val iconUrl: String
        get() = "https://openweathermap.org/img/wn/${icon}@2x.png"
}

data class MainInfo(
    @SerializedName("temp")
    val temperature: Double,

    @SerializedName("feels_like")
    val feelsLike: Double,

    @SerializedName("temp_min")
    val tempMin: Double,

    @SerializedName("temp_max")
    val tempMax: Double,

    @SerializedName("pressure")
    val pressure: Int,

    @SerializedName("humidity")
    val humidity: Int,

    @SerializedName("sea_level")
    val seaLevel: Int? = null,

    @SerializedName("grnd_level")
    val groundLevel: Int? = null
) {
    val tempCelsius: Double
        get() = temperature

    val tempFahrenheit: Double
        get() = temperature

    val feelsLikeCelsius: Double
        get() = feelsLike

    val feelsLikeFahrenheit: Double
        get() = feelsLike
}

data class Wind(
    @SerializedName("speed")
    val speed: Double,

    @SerializedName("deg")
    val degrees: Int,

    @SerializedName("gust")
    val gust: Double? = null
) {
    val direction: String
        get() = when (degrees) {
            in 0..22, in 338..360 -> "N"
            in 23..67 -> "NE"
            in 68..112 -> "E"
            in 113..157 -> "SE"
            in 158..202 -> "S"
            in 203..247 -> "SW"
            in 248..292 -> "W"
            in 293..337 -> "NW"
            else -> ""
        }
}

data class Clouds(
    @SerializedName("all")
    val cloudiness: Int
)

data class SystemInfo(
    @SerializedName("type")
    val type: Int? = null,

    @SerializedName("id")
    val id: Int? = null,

    @SerializedName("country")
    val country: String,

    @SerializedName("sunrise")
    val sunrise: Long,

    @SerializedName("sunset")
    val sunset: Long
)