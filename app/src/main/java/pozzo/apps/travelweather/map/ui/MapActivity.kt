package pozzo.apps.travelweather.map.ui

import android.app.AlertDialog
import android.app.ProgressDialog
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.res.Configuration
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.view.KeyEvent
import android.view.View
import android.view.ViewTreeObserver
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.splunk.mint.Mint
import pozzo.apps.tools.AndroidUtil
import pozzo.apps.travelweather.R
import pozzo.apps.travelweather.core.BaseActivity
import pozzo.apps.travelweather.core.Error
import pozzo.apps.travelweather.databinding.ActivityMapsBinding
import pozzo.apps.travelweather.forecast.adapter.ForecastInfoWindowAdapter
import pozzo.apps.travelweather.forecast.model.Weather
import pozzo.apps.travelweather.map.action.ActionRequest
import pozzo.apps.travelweather.map.viewmodel.MapViewModel
import pozzo.apps.travelweather.map.viewmodel.PreferencesViewModel
import pozzo.apps.travelweather.map.viewrequest.LocationPermissionRequest
import pozzo.apps.travelweather.map.viewrequest.PermissionRequest
import java.util.*

/**
 * todo A viewmodel nao pode definir como alguma coisa exibida, apenas deinir o que vai ser exibida... ?
 */
class MapActivity : BaseActivity(), OnMapReadyCallback {
    companion object {
        private val ANIM_ROUTE_TIME = 1200
        private val REQ_PERMISSION_FOR_CURRENT_LOCATION = 0x1
    }

    //todo maybe I can create an improved abstraction of this map
    private var mapMarkerToWeather = HashMap<Marker, Weather>()

    private var map: GoogleMap? = null
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var eSearch: EditText
    private lateinit var vgTopBar: View
    private lateinit var progressDialog: ProgressDialog

    private lateinit var mainThread: Handler

    private lateinit var viewModel: MapViewModel
    private lateinit var preferencesViewModel: PreferencesViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.mainThread = Handler()
        setupViewModel()
        setupDataBind()
        setupMapFragment()
        setupView()
        observeViewModel()
    }

    private fun setupViewModel() {
        viewModel = ViewModelProviders.of(this).get(MapViewModel::class.java)
        preferencesViewModel = ViewModelProviders.of(this).get(PreferencesViewModel::class.java)
    }

    private fun setupDataBind() {
        val contentView = DataBindingUtil.setContentView<ActivityMapsBinding>(this, R.layout.activity_maps)
        contentView.viewModel = viewModel
    }

    private fun setupMapFragment() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun setupView() {
        drawerLayout = findViewById(R.id.drawerLayout)
        vgTopBar = findViewById(R.id.vgTopBar)
        eSearch = findViewById(R.id.eSearch)
        eSearch.setOnEditorActionListener(onSearchGo)
        //todo replace progress dialog
        progressDialog = ProgressDialog(this)
        progressDialog.isIndeterminate = true
    }

    private val onSearchGo = TextView.OnEditorActionListener { textView, _, event ->
        if (event == null || event.action != KeyEvent.ACTION_DOWN)
            return@OnEditorActionListener false

        viewModel.searchAddress(textView.text.toString())
        return@OnEditorActionListener true
    }

    private fun observeViewModel() {
        preferencesViewModel.selectedDay.observe(this, Observer { refreshMarkers() })

        viewModel.startPosition.observe(this, Observer { latLng -> startPositionChanged(latLng) })
        viewModel.finishPosition.observe(this, Observer { latLng -> finishPositionChanged(latLng) })
        viewModel.isShowingProgress.observe(this, Observer { isShowingProgress -> progressDialogStateChanged(isShowingProgress) })
        viewModel.directionLine.observe(this, Observer { rectLine ->
            if (map == null)
                return@Observer

            if (rectLine != null)
                map?.addPolyline(rectLine)
            else
                Toast.makeText(this@MapActivity, R.string.warning_pathNotFound,
                        Toast.LENGTH_SHORT).show()
        })
        viewModel.weathers.observe(this, Observer { weathers ->
            clearMapOverlay()
            weathers?.forEach {
                addMark(it)
            }
        })
        viewModel.isShowingTopBar.observe(this, Observer { aBoolean ->
            if (aBoolean == true)
                showTopBar()
            else
                hideTopBar()
        })
        viewModel.shouldFinish.observe(this, Observer { aBoolean ->
            if (aBoolean == true)
                finish()
        })
        viewModel.error.observe(this, Observer { error ->
            if (error != null)
                showErrorDialog(error)
        })
        viewModel.actionRequest.observe(this, Observer { actionRequest ->
            if (actionRequest != null)
                showActionRequest(actionRequest)
        })
        viewModel.permissionRequest.observe(this, Observer { permissionRequest ->
            if (permissionRequest != null) requestPermissions(permissionRequest)
        })
    }

    private fun startPositionChanged(startPosition: LatLng?) {
        clearMapOverlay()
        pointMapTo(startPosition)
    }

    private fun pointMapTo(center: LatLng?) {
        if (center != null) map?.animateCamera(CameraUpdateFactory.newLatLngZoom(center, 8f))
    }

    private fun finishPositionChanged(finishPosition: LatLng?) {
        if (finishPosition != null) {
            fitCurrentRouteOnScreen()
        }
    }

    private fun fitCurrentRouteOnScreen() = pointMapTo(viewModel.getRouteBounds())

    private fun pointMapTo(latLng: LatLngBounds?) {
        if (latLng == null) return

        try {
            map?.animateCamera(CameraUpdateFactory.newLatLngBounds(latLng, 70), ANIM_ROUTE_TIME, null)
        } catch (e: IllegalStateException) {
            Mint.logException(e)
        }
    }

    private fun progressDialogStateChanged(isShowingProgress: Boolean?) {
        if (isShowingProgress == true) {
            mainThread.postDelayed(triggerCheckedShowProgress, 300)
        } else {
            progressDialog.hide()
        }
    }

    private val triggerCheckedShowProgress = Runnable {
        if (viewModel.isShowingProgress.value == true) {
            progressDialog.show()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        fitToScreenWhenLayoutIsReady()
    }

    private fun fitToScreenWhenLayoutIsReady() {
        val view = findViewById<View>(R.id.vgMain)
        val observer = view.viewTreeObserver
        observer.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                fitCurrentRouteOnScreen()
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
    }

    public override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putParcelable("startPosition", viewModel.startPosition.value)
        outState?.putParcelable("finishPosition", viewModel.finishPosition.value)

        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        if (savedInstanceState != null) {
            viewModel.setStartPosition(savedInstanceState.getParcelable("startPosition"))
            viewModel.setFinishPosition(savedInstanceState.getParcelable("finishPosition"))
        }
    }

    override fun onBackPressed() {
        viewModel.back()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.map = googleMap

        googleMap.setOnMapClickListener({ latLng -> viewModel.addPoint(latLng) })
        googleMap.setOnMapLongClickListener({ viewModel.requestClear() })
        googleMap.setOnInfoWindowClickListener(goToWeatherForecastWebPage)
        googleMap.setInfoWindowAdapter(ForecastInfoWindowAdapter(this))

        clearMapOverlay()

        mainThread.postDelayed({
            if (viewModel.startPosition.value == null)
                viewModel.setCurrentLocationAsStart(this)
        }, 500)
    }

    private fun clearMapOverlay() {
        map?.clear()
        mapMarkerToWeather.clear()
    }

    private val goToWeatherForecastWebPage = GoogleMap.OnInfoWindowClickListener { marker ->
        val weather = mapMarkerToWeather[marker]
        val url = if (weather?.url == null) "" else weather.url
        AndroidUtil.openUrl(url, this@MapActivity)
    }

    private fun addMark(weather: Weather?) {
        if (weather?.address == null) return

        val selectedDay = preferencesViewModel.selectedDay.value
        val forecast = weather.getForecast(selectedDay!!)

        val markerOptions = MarkerOptions()
                .position(weather.latLng)
                .title(forecast.text)
                .icon(forecast.icon)
        val marker = map?.addMarker(markerOptions)
        if (marker != null) mapMarkerToWeather.put(marker, weather)
    }

    private fun showErrorDialog(error: Error) {
        AlertDialog.Builder(this)
                .setTitle(R.string.warning)
                .setMessage(error.messageId)
                .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                .setOnDismissListener { viewModel.errorDismissed() }
                .show()
    }

    private fun showActionRequest(actionRequest: ActionRequest) {
        AlertDialog.Builder(this)
                .setMessage(actionRequest.messageId)
                .setPositiveButton(R.string.yes) { _, _ -> viewModel.actionRequestAccepted(actionRequest) }
                .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                .setOnDismissListener { viewModel.actionRequestDismissed() }
                .show()
    }

    fun toggleSearch(view: View) {
        viewModel.toggleTopBar()
    }

    private fun hideTopBar() {
        vgTopBar.animate().alpha(0f)
        eSearch.visibility = View.INVISIBLE
        AndroidUtil.hideKeyboard(this, eSearch)
    }

    private fun showTopBar() {
        vgTopBar.animate().alpha(1f)
        eSearch.visibility = View.VISIBLE
        eSearch.requestFocus()
        AndroidUtil.showKeyboard(this, eSearch)
    }

    fun onMenu(view: View) {
        drawerLayout.openDrawer(GravityCompat.START)
    }

    fun onClearSearch(view: View) {
        eSearch.setText("")
    }

    private fun refreshMarkers() {
        if (mapMarkerToWeather.isEmpty())
            return

        val markerWeathers = this.mapMarkerToWeather
        this.mapMarkerToWeather = HashMap()
        for ((key, value) in markerWeathers) {
            key.remove()
            addMark(value)
        }
    }

    private fun requestPermissions(permissionRequest: PermissionRequest) {
        ActivityCompat.requestPermissions(this, permissionRequest.permissions, REQ_PERMISSION_FOR_CURRENT_LOCATION)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQ_PERMISSION_FOR_CURRENT_LOCATION) {
            viewModel.onPermissionRequestedGranted(LocationPermissionRequest(viewModel), this)
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
}
