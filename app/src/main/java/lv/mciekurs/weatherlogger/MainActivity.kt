package lv.mciekurs.weatherlogger

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import com.nabinbhandari.android.permissions.PermissionHandler
import com.nabinbhandari.android.permissions.Permissions;
import okhttp3.*
import java.io.IOException


class MainActivity : AppCompatActivity() {

    private lateinit var list: MutableList<WeatherInfo>
    private lateinit var adapter: RecyclerViewAdapter
    private var lat = 0.0
    private var lon = 0.0
    private val permissions = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        list = mutableListOf()
        adapter = RecyclerViewAdapter(list)

        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        recyclerView_main.layoutManager = layoutManager
        recyclerView_main.adapter = adapter

    }


    private val locationListener = object : LocationListener {
        override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {}

        override fun onProviderEnabled(p0: String?) {}

        override fun onProviderDisabled(p0: String?) {}

        override fun onLocationChanged(location: Location) {
            //TODO: Get lan and lon
            lat = location.latitude
            lon = location.longitude
        }

    }

    @SuppressLint("MissingPermission")
    private fun getJsonData(){
        val mLocationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1,
                1.0f, locationListener)

        //TODO: Location must not be null, location disabled
        lat = location.latitude
        lon = location.longitude

        val url = this.getString(R.string.weather_url, lat, lon)
        //Toast.makeText(this, url, Toast.LENGTH_SHORT).show()

        val request = Request.Builder().url(url).build()
        val client =  OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val body = response.body().toString()
                val gson = GsonBuilder().create()
                val weatherData = gson.fromJson(body, CurrentWeatherData::class.java)
                runOnUiThread {
                    val temp = weatherData.main.temp
                    val date = Calendar.getInstance().time.toString()
                    list.add(WeatherInfo(temp, date))
                    adapter.notifyDataSetChanged()
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                //TODO: Need to handle event son failure
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Coordinates: $lat / $lon", Toast.LENGTH_SHORT).show()
                    print("Coordinates: $lat / $lon")
                }
            }

        })


    }

    @SuppressLint("MissingPermission")
    override fun onStart() {
        super.onStart()
        Permissions.check(this, permissions, null, null, object : PermissionHandler() {
            override fun onGranted() {
                checkServices()
            }

            override fun onDenied(context: Context?, deniedPermissions: ArrayList<String>?) {
                //TODO: Disable button if editText is empty
                checkServices()
            }
        })
    }

    private fun checkPermission(){
        Permissions.check(this, permissions, null, null, object : PermissionHandler() {
            override fun onGranted() {
                snackbar(R.string.location_service_granted)
            }

            override fun onDenied(context: Context?, deniedPermissions: ArrayList<String>?) {
                snackbar(R.string.location_service_not_granted)
            }
        })

    }

    private fun checkServices(): Boolean{
        val lm = getSystemService(LOCATION_SERVICE) as LocationManager
        var gpsEnabled = false
        var networkEnabled = false

        try {
            gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (e: Exception) {}

        try {
            networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception){}

        if (!gpsEnabled && !networkEnabled) {

            val snackbar = Snackbar.make(root_layout, R.string.location_not_enabled, Snackbar.LENGTH_LONG)
            snackbar.setAction("Settings") {
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                this.startActivity(intent)
            }
            snackbar.show()
            return false
        }
        return true

    }


    @SuppressLint("MissingPermission")
    private fun addItem(){
        val date = Calendar.getInstance().time.toString()
        list.add(WeatherInfo("24,5", date))
        adapter.notifyDataSetChanged()

        val mLocationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1,
                1.0f, locationListener)

        //TODO: Location must not be null, location disabled
        lat = location.latitude
        lon = location.longitude

        Toast.makeText(this, "$lat / $lon", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflate = menuInflater
        inflate.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.item_save -> {
                //TODO: Add necessary function
                if (checkServices()){
                    //addItem()
                    getJsonData()
                }
                return true
            }
            R.id.item_alert -> {
                //TODO: Add settings function
                checkPermission()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    //simple snackbar function
    private fun Activity.snackbar(message: Int) =
            Snackbar.make(root_layout, message, Toast.LENGTH_SHORT).show()


}
