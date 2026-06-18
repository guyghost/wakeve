package com.guyghost.wakeve.weather

class AndroidEventWeatherProvider : EventWeatherProvider {
    override val providerName: String = "AndroidWeatherProvider"
    override val forecastWindowDays: Int = 10

    override suspend fun fetchDailyForecast(request: WeatherForecastRequest): WeatherProviderResult =
        WeatherProviderResult.Unavailable(
            availability = WeatherAvailability.PROVIDER_UNAVAILABLE,
            message = "Android weather provider is not configured yet"
        )
}
