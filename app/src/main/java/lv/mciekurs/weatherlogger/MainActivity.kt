package lv.mciekurs.weatherlogger

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var ACCESS_COARSE_LOCATION_CODE = 123
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

    @SuppressLint("MissingPermission")
    private fun getJsonData(){
        var lat = 0.0
        var lon = 0.0

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation.addOnCompleteListener {
            if (it.isSuccessful){
                lat = it.result.latitude
                lon = it.result.longitude

                Toast.makeText(applicationContext, "$lat, $lon", Toast.LENGTH_SHORT).show()
            }
            else {
                Toast.makeText(applicationContext, "Unsuccessful conn", Toast.LENGTH_SHORT).show()
            }
        }

        val url = "http://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&units=metric&appid=65b9e5e5bf3289000b4d663073142565"
    }

    override fun onStart() {
        super.onStart()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            //permission not granted

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.ACCESS_COARSE_LOCATION)) {
                //show an explanation to the user
            } else {
                //request the permission
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                        ACCESS_COARSE_LOCATION_CODE)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode){
            ACCESS_COARSE_LOCATION_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    //permission granted
                } else {
                    //permission denied
                }
                return
            }
        }
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
                //Toast.makeText(applicationContext, "Test Toast", Toast.LENGTH_SHORT).show()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }


}
