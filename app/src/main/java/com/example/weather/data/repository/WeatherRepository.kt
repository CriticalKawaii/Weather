package com.example.weather.data.repository

import com.example.weather.BuildConfig
import com.example.weather.data.api.ApiClient
import com.example.weather.data.model.WeatherResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WeatherRepository {

    private val apiService = ApiClient.weatherApiService

    suspend fun getWeatherByCity(
        cityName: String,
        units: String = "metric"
    ): Result<WeatherResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getWeatherByCity(
                cityName = cityName,
                apiKey = BuildConfig.OPENWEATHER_API_KEY,
                units = units
            )

            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Weather data not found"))
            } else {
                when (response.code()) {
                    404 -> Result.failure(Exception("City not found"))
                    401 -> Result.failure(Exception("Invalid API key"))
                    429 -> Result.failure(Exception("Too many requests. Please try again later"))
                    else -> Result.failure(Exception("Error: ${response.message()}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    suspend fun getWeatherByCoordinates(
        latitude: Double,
        longitude: Double,
        units: String = "metric"
    ): Result<WeatherResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getWeatherByCoordinates(
                latitude = latitude,
                longitude = longitude,
                apiKey = BuildConfig.OPENWEATHER_API_KEY,
                units = units
            )

            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Weather data not found"))
            } else {
                when (response.code()) {
                    401 -> Result.failure(Exception("Invalid API key"))
                    429 -> Result.failure(Exception("Too many requests. Please try again later"))
                    else -> Result.failure(Exception("Error: ${response.message()}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: WeatherRepository? = null

        fun getInstance(): WeatherRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WeatherRepository().also { INSTANCE = it }
            }
        }
    }
}