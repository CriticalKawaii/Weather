package com.example.weather

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.weather.databinding.ActivityMainBinding
import com.example.weather.ui.viewmodel.TemperatureUnit
import com.example.weather.ui.viewmodel.WeatherViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val viewModel: WeatherViewModel by viewModels()

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var currentUnit = TemperatureUnit.CELSIUS

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true -> {
                getCurrentLocation()
            }
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                getCurrentLocation()
            }
            else -> {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupListeners()

        observeViewModel()

    }

    private fun setupListeners() {
        binding.searchButton.setOnClickListener {
            performSearch()
        }

        binding.cityEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else { false }
        }

        binding.locationButton.setOnClickListener {
            checkLocationPermissionAndGetWeather()
        }

        binding.unitToggleFab.setOnClickListener {
            toggleTemperatureUnit()
        }
    }

    private fun observeViewModel() {
        viewModel.weatherData.observe(this) { weather ->
            weather?.let {
                displayWeatherData(it)
                binding.weatherScrollView.visibility = View.VISIBLE
                binding.errorTextView.visibility = View.GONE
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            if (isLoading) {
                binding.weatherScrollView.visibility = View.GONE
            }
        }

        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                binding.errorTextView.text = it
                binding.errorTextView.visibility = View.VISIBLE
                binding.weatherScrollView.visibility = View.GONE
            }
        }

        viewModel.temperatureUnit.observe(this) { unit ->
            currentUnit = unit
            viewModel.weatherData.value?.let { displayWeatherData(it) }
        }
    }

    private fun performSearch() {
        val cityName = binding.cityEditText.text.toString().trim()

        if (cityName.isEmpty()) {
            Toast.makeText(this, "Введите город", Toast.LENGTH_SHORT).show()
            return
        }

        hideKeyboard()

        viewModel.getWeatherByCity(cityName)
    }

    private fun checkLocationPermissionAndGetWeather() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                getCurrentLocation()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                showLocationPermissionRationale()
            }
            else -> {
                requestLocationPermissions()
            }
        }
    }

    private fun showLocationPermissionRationale() {
        AlertDialog.Builder(this)
            .setTitle("Location Permission Needed")
            .setMessage("This app needs location permission to show weather for your current location.")
            .setPositiveButton("Grant Permission") { _, _ ->
                requestLocationPermissions()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun requestLocationPermissions() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        viewModel.getWeatherByLocation(0.0, 0.0)

        val cancellationTokenSource = CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        ).addOnSuccessListener { location: Location? ->
            location?.let {
                viewModel.getWeatherByLocation(it.latitude, it.longitude)
            } ?: run {
                getLastKnownLocation()
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(
                this,
                "Failed to get location: ${exception.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun getLastKnownLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                viewModel.getWeatherByLocation(it.latitude, it.longitude)
            } ?: run {
                Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayWeatherData(weather: com.example.weather.data.model.WeatherResponse) {
        binding.cityNameTextView.text = "${weather.cityName}, ${weather.systemInfo.country}"
        binding.dateTimeTextView.text = formatDateTime(weather.timestamp)

        val temp = when (currentUnit) {
            TemperatureUnit.CELSIUS -> weather.mainInfo.tempCelsius
            TemperatureUnit.FAHRENHEIT -> weather.mainInfo.tempFahrenheit
        }
        val feelsLike = when (currentUnit) {
            TemperatureUnit.CELSIUS -> weather.mainInfo.feelsLikeCelsius
            TemperatureUnit.FAHRENHEIT -> weather.mainInfo.feelsLikeFahrenheit
        }
        val unitSymbol = when (currentUnit) {
            TemperatureUnit.CELSIUS -> "°C"
            TemperatureUnit.FAHRENHEIT -> "°F"
        }

        binding.temperatureTextView.text = "${temp.toInt()}°"
        binding.feelsLikeTextView.text = "Ощущается ${feelsLike.toInt()}$unitSymbol"

        val weatherInfo = weather.weatherList.firstOrNull()
        weatherInfo?.let {
            binding.descriptionTextView.text = it.description

            Glide.with(this)
                .load(it.iconUrl)
                .into(binding.weatherIconImageView)
        }

        binding.humidityTextView.text = "${weather.mainInfo.humidity}%"
        binding.windSpeedTextView.text = "${weather.wind.speed} m/s ${weather.wind.direction}"
        binding.pressureTextView.text = "${weather.mainInfo.pressure} hPa"
        binding.visibilityTextView.text = "${weather.visibility / 1000} km"

        binding.sunriseTextView.text = formatTime(weather.systemInfo.sunrise)
        binding.sunsetTextView.text = formatTime(weather.systemInfo.sunset)
    }

    private fun toggleTemperatureUnit() {
        val newUnit = when (currentUnit) {
            TemperatureUnit.CELSIUS -> TemperatureUnit.FAHRENHEIT
            TemperatureUnit.FAHRENHEIT -> TemperatureUnit.CELSIUS
        }
        viewModel.setTemperatureUnit(newUnit)

        val unitName = when (newUnit) {
            TemperatureUnit.CELSIUS -> "Celsius"
            TemperatureUnit.FAHRENHEIT -> "Fahrenheit"
        }
        Toast.makeText(this, "Температура в $unitName", Toast.LENGTH_SHORT).show()
    }

    private fun formatDateTime(timestamp: Long): String {
        val date = Date(timestamp * 1000) // Convert seconds to milliseconds
        val format = SimpleDateFormat("EEEE, d MMMM yyyy • h:mm a", Locale.getDefault())
        return format.format(date)
    }

    private fun formatTime(timestamp: Long): String {
        val date = Date(timestamp * 1000)
        val format = SimpleDateFormat("h:mm a", Locale.getDefault())
        return format.format(date)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.cityEditText.windowToken, 0)
    }
}