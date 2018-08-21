package lv.mciekurs.weatherlogger

class CurrentWeatherData(val main: Main)

class Main(val temp: String)

class WeatherInfo(val temp: String, val data: String)