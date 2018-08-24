package lv.mciekurs.weatherlogger

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.gson.GsonBuilder
import com.nabinbhandari.android.permissions.PermissionHandler
import com.nabinbhandari.android.permissions.Permissions
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.*
import org.jetbrains.anko.toast
import java.io.IOException
import java.net.InetAddress
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var list: MutableList<WeatherInfo>
    private lateinit var adapter: RecyclerViewAdapter

    private val TAG = MainActivity::class.java.simpleName

    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var  mSettingsClient: SettingsClient
    private lateinit var mLocationRequest: LocationRequest
    private lateinit var mLocationCallback: LocationCallback
    private lateinit var mLocationSettingsRequest: LocationSettingsRequest
    private lateinit var mCurrentLocation: Location
    private var mRequestingLocationUpdates: Boolean = false

    private val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 10000
    private val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS: Long = UPDATE_INTERVAL_IN_MILLISECONDS / 2
    private val REQUEST_PERMISSIONS_REQUEST_CODE = 23
    private val REQUEST_CHECK_SETTINGS = 0x1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar_main)
        supportActionBar?.setDisplayShowTitleEnabled(false)



        list = mutableListOf()
        adapter = RecyclerViewAdapter(list)

        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        recyclerView_main.layoutManager = layoutManager
        recyclerView_main.adapter = adapter

        //initialize core essentials
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mSettingsClient = LocationServices.getSettingsClient(this)

        mCurrentLocation = Location("dummyprovider")

        createLocationCallback()
        createLocationRequest()
        buildLocationSettingsRequest()


    }

    @SuppressLint("StringFormatMatches")
    private fun getJsonData(){
        var url =  ""

        if (checkIfEmpty()){
            val lat = mCurrentLocation.latitude
            val lon = mCurrentLocation.longitude

            url = this.getString(R.string.weather_url_location, lat, lon)
        } else {
            url = this.getString(R.string.weather_url_city, "London")
        }

        val request = Request.Builder().url(url).build()
        val client =  OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call?, response: Response?) {
                val body = response?.body()?.string()
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
                runOnUiThread {
                    if (!isInternetAvailable()){
                        //toast("Please enable internet connection!")
                        toast(e.message.toString())
                    } else {
                        toast(e.message.toString())
                    }

                }
            }

        })


    }

    /** Callback for recieving location events */
    private fun createLocationCallback(){
        mLocationCallback = object : LocationCallback(){
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)

                mCurrentLocation = locationResult.lastLocation
                updateLocationUI()
            }
        }
    }

    /** Setup location request settings */
    private fun createLocationRequest() {
        mLocationRequest = LocationRequest()

        //interval is somewhat inexact(other application can request faster)
        mLocationRequest.interval = UPDATE_INTERVAL_IN_MILLISECONDS
        //interval is exact (application will never receive updates faster than this value)
        mLocationRequest.fastestInterval = FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS
        //creates accuracy priority
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    /** Check if device has the needed location settings */
    private fun buildLocationSettingsRequest() {
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(mLocationRequest)
        //stores the types of location services the client is interested in using
        mLocationSettingsRequest = builder.build()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            REQUEST_CHECK_SETTINGS -> when (resultCode) {
                Activity.RESULT_OK -> {
                    Log.i(TAG, "User agreed to make required location settings changes.")
                    //nothing to do, startLocationupdates() gets called in onResume again
                }
                Activity.RESULT_CANCELED -> {
                    Log.i(TAG, "User chose not to make required location settings changes.")
                    mRequestingLocationUpdates = false
                    updateUI()
                }
            }
        }
    }

    /** Requests location updates from the FusedLocationApi */
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates(){
        mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener {
                    Log.i(TAG, "All location settings are satisfied.")

                    //requests location updates with a callback on the specified Looper thread
                    mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                            mLocationCallback, Looper.myLooper())
                    updateUI()
                }.addOnFailureListener{
                    val statusCode = (it as ApiException).statusCode
                    when (statusCode){
                        LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                            Log.i(TAG, "Location settings not satisfied")
                            try {
                                val rae = it as ResolvableApiException
                                rae.startResolutionForResult(this, REQUEST_CHECK_SETTINGS)
                            } catch (sie: IntentSender.SendIntentException) {
                                Log.i(TAG, "PendingIntent unable to execute request.")
                            }
                        } LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                        val errorMsg = "Location settings are inadequate, fix in settings"
                        Log.e(TAG, errorMsg)
                        Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
                        mRequestingLocationUpdates = false
                    }
                    }
                    updateUI()
                }
    }

    private fun stopLocationUpdates(){
        if (!mRequestingLocationUpdates){
            Log.d(TAG, "Location updates stopped")
        }
        //removing location request for battery saving
        mFusedLocationClient.removeLocationUpdates(mLocationCallback)
                .addOnSuccessListener {
                    mRequestingLocationUpdates = false
                    //TODO: Change button state
                }


    }

    private fun updateUI(){
        //TODO: Change button states
        updateLocationUI()
    }

    private fun updateLocationUI(){
        //if(mCurrentLocation != null){
        //TODO: Update location values
        //textView_lat.text = mCurrentLocation.latitude.toString()
        //textView_lon.text = mCurrentLocation.longitude.toString()
        //}
    }

    override fun onStart() {
        super.onStart()

        if (mRequestingLocationUpdates && checkPermissions()){
            startLocationUpdates()
        }

        //TODO: Handle permissions? (kinda works with everything disabled, idk why tho)
    }

    override fun onPause() {
        super.onPause()

        //remove location updates to save battery
        stopLocationUpdates()
    }

    override fun onResume() {
        super.onResume()

        if (mRequestingLocationUpdates && checkPermissions()){
            startLocationUpdates()
        } else if (!checkPermissions()){
            requestPermissions()
        }

    }

    fun isInternetAvailable(): Boolean {
        return try {
            val ipAddr = InetAddress.getByName("google.com")
            //You can replace it with your name
            !ipAddr.equals("")

        } catch (e: Exception) {
            false
        }

    }

    private fun checkPermissions(): Boolean {
        val permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
        return permissionState == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions(){
        val shouldProvideRationale = ActivityCompat.
                shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)

        //provide additional rationale for user
        if (shouldProvideRationale){
            Log.i(TAG, "Display permission rational")
            Snackbar.make(root_layout, R.string.permission_rationale, Snackbar.LENGTH_SHORT)
                    .setAction(android.R.string.ok) {
                        ActivityCompat.requestPermissions(this,
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                                REQUEST_PERMISSIONS_REQUEST_CODE)
                    }
        } else {
            Log.i(TAG, "Requesting permission")
            //possible that user have checked "never ask again"
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_PERMISSIONS_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        Log.i(TAG, "Callback received from permission request")
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE){
            if (grantResults.isEmpty()){
                Log.i(TAG, "User interaction was canceled")
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mRequestingLocationUpdates){
                    Log.i(TAG, "Permission granted, starting location updates")
                    startLocationUpdates()
                }
            } else {
                //notify the user via a SnackBar that they have rejected a core permission
                Snackbar.make(root_layout, R.string.permission_denied_explanation, Snackbar.LENGTH_SHORT)
                        .setAction(R.string.settings) {
                            val intent = Intent()
                            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            val uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                            intent.data = uri
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) : Boolean {
        when (item.itemId) {
            R.id.item_save -> {
                //TODO: Add necessary function
                getJsonData()
                return true
            }
            R.id.item_alert -> {
                //TODO: Add settings function
                Permissions.check(this, Manifest.permission.ACCESS_FINE_LOCATION, null, object : PermissionHandler() {
                    override fun onGranted() {
                        Toast.makeText(this@MainActivity, "Permission granted", Toast.LENGTH_SHORT).show()
                    }
                })
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun checkIfEmpty(): Boolean{
        val editText = toolbar_main.findViewById(R.id.editText_search) as EditText
        if (editText.text.trim().isEmpty()){
            return true
        }
        return false
    }


}
