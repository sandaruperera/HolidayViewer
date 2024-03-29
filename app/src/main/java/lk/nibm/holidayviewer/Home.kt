package lk.nibm.holidayviewer

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import lk.nibm.holidayviewer.adaptor.CurrentMonthHolidayAdaptor
import lk.nibm.holidayviewer.model.HolidaysModel
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class Home : AppCompatActivity() {

    private lateinit var txtHomeCurrentLocation: TextView
    private lateinit var rvThisMonthHolidays: RecyclerView
    private lateinit var selectedDateRecyclerView: RecyclerView
    private lateinit var currentMonthHolidays: ArrayList<HolidaysModel>
    private lateinit var cardLocalHolidays: CardView
    private lateinit var cardGlobalHolidays: CardView
    private lateinit var calendarView: CalendarView


    //For getting location
    private lateinit var fusedLocation: FusedLocationProviderClient
    var isPermissionGranted: Boolean = false
    private val LOCATION_REQUEST_CODE = 100
    private var countryCode: String? = null
    var holidayDataArray = JSONArray()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        initializeComponents()
        fusedLocation = LocationServices.getFusedLocationProviderClient(this)
        checkLocationPermission()
        clickListeners()
    }

    private fun clickListeners() {
        cardLocalHolidays.setOnClickListener {
            val intent = Intent(this, LocalHolidays::class.java)
            startActivity(intent)
        }
        cardGlobalHolidays.setOnClickListener {
            val intent = Intent(this, GlobalHolidays::class.java)
            startActivity(intent)
        }
    }

    private fun initializeComponents() {
        txtHomeCurrentLocation = findViewById(R.id.txtHomeCurrentLocation)
        rvThisMonthHolidays = findViewById(R.id.rvThisMonthHolidays)
        currentMonthHolidays = arrayListOf<HolidaysModel>()
        cardLocalHolidays = findViewById(R.id.cardLocalHolidays)
        cardGlobalHolidays = findViewById(R.id.cardGlobalHolidays)
        selectedDateRecyclerView = findViewById(R.id.selectedDateRecyclerView)
        calendarView = findViewById(R.id.calendarView)

    }


    override fun onStart() {
        checkLocationPermission()
        super.onStart()
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {

        if (isPermissionGranted) {
            val locationResult = fusedLocation.lastLocation
            locationResult.addOnCompleteListener(this) { location ->
                if (location.isSuccessful) {
                    val lastLocation = location.result
                    val geocoder = Geocoder(this, Locale.getDefault())
                    val addresses =
                        geocoder.getFromLocation(lastLocation!!.latitude, lastLocation.longitude, 1)
                    val country = addresses?.get(0)!!.countryName
                    txtHomeCurrentLocation.text = country
                    if (country != null) {
                        getCountryId(country)
                    } else {
                        getCurrentMonthHolidays("LK")

                    }
                } else {
                    getCurrentMonthHolidays("LK")

                }
            }
        } else {
            txtHomeCurrentLocation.text = "Location Not Available !"
            getCurrentMonthHolidays("LK")

        }
    }

    fun getCountryId(name: String) {
        val url =
            resources.getString(R.string.countries_based_url) + resources.getString(R.string.API_Key)
        val resultCountries = StringRequest(Request.Method.GET, url, Response.Listener { response ->
            try {
                val jsonObject = JSONObject(response)
                val jsonObjectResponse = jsonObject.getJSONObject("response")
                val jsonArrayCountries = jsonObjectResponse.getJSONArray("countries")
                for (i in 0 until jsonArrayCountries.length()) {
                    val jsonObjectCountry = jsonArrayCountries.getJSONObject(i)
                    if (jsonObjectCountry.getString("country_name") == name) {
                        countryCode = jsonObjectCountry.getString("iso-3166").toString()
                        getCurrentMonthHolidays(countryCode!!)
                        getCurrentDayHolidayData(countryCode!!)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this, "" + e.message, Toast.LENGTH_SHORT).show()

            }
        }, Response.ErrorListener { error ->
            Toast.makeText(this, "" + error.message, Toast.LENGTH_SHORT).show()

        })
        Volley.newRequestQueue(this).add(resultCountries)
    }

    private fun getCurrentDayHolidayData(countryId: String) {
        calendarView.setOnDateChangeListener(CalendarView.OnDateChangeListener { view, year, month, dayOfMonth ->

            val todayDay = (dayOfMonth.toString())
            val todayYear = year
            val todayMonth = month + 1

            val url =
                resources.getString(R.string.holidays_based_url) + resources.getString(R.string.API_Key) + "&country=" + countryId + "&year=" + todayYear + "&month=" + todayMonth + "&day=" + todayDay

            val request = StringRequest(Request.Method.GET, url,
                Response.Listener { response ->
                    try {
                        val jsonObject = JSONObject(response)
                        val jsonObjectResponse = jsonObject.getJSONObject("response")
                        holidayDataArray = jsonObjectResponse.getJSONArray("holidays")
                        selectedDateRecyclerView.adapter?.notifyDataSetChanged()


                    } catch (e: Exception) {
                        e.printStackTrace()

                    }
                },
                Response.ErrorListener { error ->
                    error.printStackTrace()

                })

            Volley.newRequestQueue(this).add(request)

        })
    }

    fun getCurrentMonthHolidays(countryId: String){
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)

        val url =
            resources.getString(R.string.holidays_based_url) + resources.getString(R.string.API_Key) + "&country=" + countryId + "&year=" + currentYear
        val result = StringRequest(Request.Method.GET, url, Response.Listener { response ->
            try {
                val jsonObject = JSONObject(response)
                val jsonObjectResponse = jsonObject.getJSONObject("response")
                val jsonArrayHolidays = jsonObjectResponse.getJSONArray("holidays")

                // Check holidays in this month
                val calendar = Calendar.getInstance()
                val currentMonth = (calendar.get(Calendar.MONTH) + 1).toString()
                val currentDay =
                    SimpleDateFormat("d", Locale.getDefault()).format(Date()).toString()
                val longMonth = SimpleDateFormat("MMMM", Locale.getDefault()).format(Date())
                currentMonthHolidays.clear()
                for (i in 0 until jsonArrayHolidays.length()) {
                    val jsonObjectHolidayList = jsonArrayHolidays.getJSONObject(i)
                    val date = jsonObjectHolidayList.getJSONObject("date")
                    val dateTime = date.getJSONObject("datetime")
                    val month = dateTime.getString("month")

                    if (month == currentMonth) {
                        val holidays = HolidaysModel()
                        holidays.holidayName = jsonObjectHolidayList.getString("name")
                        holidays.holidayDescription = jsonObjectHolidayList.getString("description")
                        holidays.holidayDate = dateTime.getString("day")
                        holidays.holidayMonth = longMonth
                        holidays.holidayYear = dateTime.getString("year")
                        holidays.holidayPrimaryType =
                            jsonObjectHolidayList.getString("primary_type")
                        holidays.holidayCountry =
                            jsonObjectHolidayList.getJSONObject("country").getString("name")

                        currentMonthHolidays.add(holidays)
                    }

                }
                // Pass holidays to adapter
                val adapter = CurrentMonthHolidayAdaptor(this, currentMonthHolidays)
                rvThisMonthHolidays.setHasFixedSize(true)
                rvThisMonthHolidays.layoutManager =
                    LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
                rvThisMonthHolidays.adapter = adapter
                adapter.notifyDataSetChanged()

            } catch (e: Exception) {
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            }
        }, Response.ErrorListener { error ->
            Toast.makeText(this, error.message, Toast.LENGTH_SHORT).show()

        })
        Volley.newRequestQueue(this).add(result)
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_REQUEST_CODE
            )
        } else {
            isPermissionGranted = true
            getCurrentLocation()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            LOCATION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    isPermissionGranted = true
                    getCurrentLocation()
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_LONG).show()
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    inner class HolidayAdapter : RecyclerView.Adapter<HolidayViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolidayViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.selected_date_holidays, parent, false)

            return HolidayViewHolder(view)
        }

        override fun onBindViewHolder(holder: HolidayViewHolder, position: Int) {
            val calendar =   Calendar.getInstance()
            if (holidayDataArray.length() == 0) {
                holder.holidayName.text = "No Holiday ! "
                holder.holidayType.text = ""
            } else {
                try {
                    val holiday = holidayDataArray.getJSONObject(position)

                    val holidayObject = holidayDataArray.getJSONObject(position)
                    val holidayName = holidayObject.getString("name")
                    val holidayType = holidayObject.getString("primary_type")
                    val date = holidayObject.getJSONObject("date")
                    val dateTime = date.getJSONObject("datetime")
                    val day = dateTime.getString("day")


                    holder.holidayName.text = holidayName
                    holder.holidayType.text = holidayType
                    holiday.getJSONObject("date").getJSONObject("datetime").getInt("day").toString()

                    holder.itemView.setOnClickListener {
                        val dialogBuilder = AlertDialog.Builder(holder.itemView.context)
                        dialogBuilder.setTitle(holiday.getString("name"))
                        dialogBuilder.setMessage(holiday.getString("description"))
                        dialogBuilder.setPositiveButton("OK", null)
                        dialogBuilder.create().show()
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        override fun getItemCount(): Int {
            return if (holidayDataArray.length() == 0) 1 else holidayDataArray.length()
        }
    }

    inner class HolidayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val holidayName: TextView = itemView.findViewById(R.id.txtTodayEvent)
        val holidayType: TextView = itemView.findViewById(R.id.txtType)
    }

}