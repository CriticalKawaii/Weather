package com.example.weather.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weather.data.model.WeatherResponse
import com.example.weather.data.repository.WeatherRepository
import kotlinx.coroutines.launch

class WeatherViewModel : ViewModel() {

    private val repository = WeatherRepository.getInstance()

    private val _weatherData = MutableLiveData<WeatherResponse?>()

    val weatherData: MutableLiveData<WeatherResponse?> = _weatherData

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _temperatureUnit = MutableLiveData<TemperatureUnit>(TemperatureUnit.CELSIUS)
    val temperatureUnit: LiveData<TemperatureUnit> = _temperatureUnit

    private var lastSearchedCity: String? = null

    fun getWeatherByCity(cityName: String) {
        _errorMessage.value = null

        _isLoading.value = true

        lastSearchedCity = cityName

        viewModelScope.launch {
            try {
                val units = when (_temperatureUnit.value) {
                    TemperatureUnit.CELSIUS -> "metric"
                    TemperatureUnit.FAHRENHEIT -> "imperial"
                    else -> "metric"
                }

                val result = repository.getWeatherByCity(cityName, units)

                result.fold(
                    onSuccess = { weather ->
                        _weatherData.value = weather
                        _errorMessage.value = null
                    },
                    onFailure = { exception ->
                        _errorMessage.value = exception.message
                        _weatherData.value = null
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Unexpected error: ${e.message}"
                _weatherData.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getWeatherByLocation(latitude: Double, longitude: Double) {
        _errorMessage.value = null
        _isLoading.value = true

        lastSearchedCity = null

        viewModelScope.launch {
            try {
                val units = when (_temperatureUnit.value) {
                    TemperatureUnit.CELSIUS -> "metric"
                    TemperatureUnit.FAHRENHEIT -> "imperial"
                    else -> "metric"
                }

                val result = repository.getWeatherByCoordinates(latitude, longitude, units)

                result.fold(
                    onSuccess = { weather ->
                        _weatherData.value = weather
                        _errorMessage.value = null
                    },
                    onFailure = { exception ->
                        _errorMessage.value = exception.message
                        _weatherData.value = null
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Unexpected error: ${e.message}"
                _weatherData.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshWeather() {
        lastSearchedCity?.let { city ->
            getWeatherByCity(city)
        }
    }

    fun setTemperatureUnit(unit: TemperatureUnit) {
        if (_temperatureUnit.value != unit) {
            _temperatureUnit.value = unit

            if (_weatherData.value != null) {
                refreshWeather()
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

enum class TemperatureUnit {
    CELSIUS,
    FAHRENHEIT
}