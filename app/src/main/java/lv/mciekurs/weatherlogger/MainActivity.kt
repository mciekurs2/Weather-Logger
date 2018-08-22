package lv.mciekurs.weatherlogger

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.location.Location
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import com.nabinbhandari.android.permissions.PermissionHandler
import com.nabinbhandari.android.permissions.Permissions;


class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private var ACCESS_COARSE_LOCATION_CODE = 123
    private lateinit var dialog: Dialog
    private lateinit var list: MutableList<WeatherInfo>
    private lateinit var adapter: RecyclerViewAdapter

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

    private fun getLocation(){
        locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 100
        locationRequest.fastestInterval = 100



    }

    @SuppressLint("MissingPermission")
    private fun getJsonData(){
        var lat = 0.0
        var lon = 0.0

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                lat = location.latitude
                lon = location.longitude

                Toast.makeText(applicationContext, "$lat, $lon", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(applicationContext, "Unsuccessful conn", Toast.LENGTH_SHORT).show()
            }
        }



/*            if (it.isSuccessful){
                lat = it.result.latitude
                lon = it.result.longitude

                Toast.makeText(applicationContext, "$lat, $lon", Toast.LENGTH_SHORT).show()
            }
            else {
                Toast.makeText(applicationContext, "Unsuccessful conn", Toast.LENGTH_SHORT).show()
            }*/


        val url = "http://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&units=metric&appid=65b9e5e5bf3289000b4d663073142565"
    }

    override fun onStart() {
        super.onStart()
        Permissions.check(this, Manifest.permission.ACCESS_COARSE_LOCATION, null, object : PermissionHandler() {
            override fun onGranted() {}
        })
    }

    private fun checkPermission(){
        Permissions.check(this, Manifest.permission.ACCESS_COARSE_LOCATION, null, object : PermissionHandler() {
            override fun onGranted() {
                snackbar("Location permission has been granted.")
            }

            override fun onDenied(context: Context?, deniedPermissions: ArrayList<String>?) {
                snackbar("Location permission has been denied.")
            }
        })

    }

    private fun addItem(){
        val date = Calendar.getInstance().time.toString()
        list.add(WeatherInfo("24,5", date))
        adapter.notifyDataSetChanged()
        getJsonData()
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
                addItem()
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
    private fun Activity.snackbar(message: CharSequence) =
            Snackbar.make(root_layout, message, Toast.LENGTH_SHORT).show()


}
