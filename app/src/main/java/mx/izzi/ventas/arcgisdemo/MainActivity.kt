package mx.izzi.ventas.arcgisdemo

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Picture
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.esri.arcgisruntime.ArcGISRuntimeEnvironment
import com.esri.arcgisruntime.concurrent.ListenableFuture
import com.esri.arcgisruntime.data.Feature
import com.esri.arcgisruntime.data.ServiceFeatureTable
import com.esri.arcgisruntime.layers.FeatureLayer
import com.esri.arcgisruntime.loadable.LoadStatus
import com.esri.arcgisruntime.mapping.ArcGISMap
import com.esri.arcgisruntime.mapping.BasemapStyle
import com.esri.arcgisruntime.mapping.Viewpoint
import com.esri.arcgisruntime.mapping.view.Callout
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener
import com.esri.arcgisruntime.mapping.view.Graphic
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay
import com.esri.arcgisruntime.mapping.view.LayerViewStatus
import com.esri.arcgisruntime.mapping.view.MapView
import com.esri.arcgisruntime.symbology.PictureMarkerSymbol
import com.esri.arcgisruntime.symbology.SimpleRenderer
import com.esri.arcgisruntime.tasks.geocode.GeocodeParameters
import com.esri.arcgisruntime.tasks.geocode.LocatorTask
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import mx.izzi.ventas.arcgisdemo.databinding.ActivityMainBinding
import mx.izzi.ventas.arcgisdemo.util.stringAttributes
import mx.izzi.ventas.arcgisdemo.util.toWgs84Format
import mx.izzi.ventas.arcgisdemo.viewmodel.MainViewModel
import java.util.*
import kotlin.math.roundToInt


class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private lateinit var arcGisMapView: MapView

    private lateinit var addressPointsFeatureLayer: FeatureLayer
    private lateinit var neighborhoodsFeatureLayer: FeatureLayer
    private lateinit var callout: Callout
    private lateinit var mapGraphicsOverlay: GraphicsOverlay
    private lateinit var pinSymbol: PictureMarkerSymbol
    private lateinit var addressPointSymbol: PictureMarkerSymbol
    private lateinit var locatorTask: LocatorTask
    private lateinit var geocodeParameters: GeocodeParameters

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        arcGisMapView = binding.arcgisMap

        setupMap()
    }

    override fun onResume() {
        super.onResume()

        arcGisMapView.resume()
    }

    private fun setupMap() {
        ArcGISRuntimeEnvironment.setApiKey("AAPK9a286acf48434e399c843b7c16b45307RvC9Iybe8_ztBbEaC3TvAn0nG-S-dHnELfzBp4dW-kFpl9zfcBqP-vlX76krDLnQ")

        val map = ArcGISMap(BasemapStyle.ARCGIS_STREETS)
        arcGisMapView.map = map
        mapGraphicsOverlay = GraphicsOverlay()
        arcGisMapView.graphicsOverlays.add(mapGraphicsOverlay)
        createMarkerSymbol()
        createAddressPointLayerSymbol()

        setupGeocoding()

        binding.searchInputLayout.visibility = View.GONE
        binding.searchOpt.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()

        arcGisMapView.pause()
    }

    override fun onDestroy() {
        super.onDestroy()

        arcGisMapView.dispose()
    }

    /**
     * Formats and displays the layer view status flags in a text view.
     *
     * @param layerViewStatus to display
     */
    private fun displayViewStateText(layerViewStatus: EnumSet<LayerViewStatus>) {
        // for each view state property that's active,
        // add it to a list and display the states as a comma-separated string
        val stringList = mutableListOf<String>()
        if (layerViewStatus.contains(LayerViewStatus.ACTIVE)) {
            stringList.add(getString(R.string.active_state))
        }
        if (layerViewStatus.contains(LayerViewStatus.ERROR)) {
            stringList.add(getString(R.string.error_state))
        }
        if (layerViewStatus.contains(LayerViewStatus.LOADING)) {
            stringList.add(getString(R.string.loading_state))
        }
        if (layerViewStatus.contains(LayerViewStatus.NOT_VISIBLE)) {
            stringList.add(getString(R.string.not_visible_state))
        }
        if (layerViewStatus.contains(LayerViewStatus.OUT_OF_SCALE)) {
            stringList.add(getString(R.string.out_of_scale_state))
        }
        if (layerViewStatus.contains(LayerViewStatus.WARNING)) {
            stringList.add(getString(R.string.warning_state))
        }
        Toast.makeText(this, stringList.joinToString(", "), Toast.LENGTH_LONG).show()

        Log.i(MainActivity::class.java.simpleName, stringList.joinToString(", "))
    }

    private fun centerFeatureOnMap(feature: Feature) {
        val envelope = feature.geometry.extent
        arcGisMapView.setViewpointGeometryAsync(envelope, 200.0)
    }

    private fun showFeatureInfo(feature: Feature) {
        val alert = MaterialAlertDialogBuilder(this)
            .setTitle("Feature properties")
            .setMessage(feature.stringAttributes())
            .setPositiveButton("Dissmiss") { dialogInterface, i ->

            }
            .create()
        alert.show()
    }

    private fun showFeatureCalloutInfo(feature: Feature) {
        val calloutContent = TextView(this)
        calloutContent.text = feature.stringAttributes()

        val envelope = feature.geometry.extent
        callout.location = envelope.center
        callout.content = calloutContent
        callout.show()
    }

    private fun setupNeighborhoodsFeatureLayer(map: ArcGISMap) {
        val serviceFeatureTable = ServiceFeatureTable("https://gis-p.sunvizion.izzi.mx:6443/arcgis/rest/services/prod/Inventory/FeatureServer/2")
        serviceFeatureTable.addDoneLoadingListener {
            neighborhoodsFeatureLayer = FeatureLayer(serviceFeatureTable).apply {
                opacity = 0.8f
                minScale = 12000.0
            }

            val layerAdded = map.operationalLayers?.add(neighborhoodsFeatureLayer) ?: false
            if (layerAdded) {

            }
        }
        serviceFeatureTable.loadAsync()
    }

    private fun createMarkerSymbol() {
        val pinDrawable = ContextCompat.getDrawable(this, R.drawable.ic_map_pin) as BitmapDrawable
        val pinSymbolFuture = PictureMarkerSymbol.createAsync(pinDrawable)
        pinSymbolFuture.addDoneListener {
            pinSymbol = pinSymbolFuture.get()
            pinSymbol.apply {
                height = 40F
                width = 30F
                offsetY = 25F
            }
        }
    }

    private fun createAddressPointLayerSymbol() {
        val addressPointDrawable = ContextCompat.getDrawable(this, R.drawable.ic_address_point) as BitmapDrawable
        val addressPointSymbolFuture = PictureMarkerSymbol.createAsync(addressPointDrawable)
        addressPointSymbolFuture.addDoneListener {
            addressPointSymbol = addressPointSymbolFuture.get()
            addressPointSymbol.apply {
                height = 20F
                width = 20F
            }
        }
    }

    private fun setupGeocoding() {
        val locationToSearch = "San Rafael, Cuauhtemoc, 06470"
        locatorTask = LocatorTask("https://geocode-api.arcgis.com/arcgis/rest/services/World/GeocodeServer")
        geocodeParameters = GeocodeParameters().apply {
            resultAttributeNames.add("PlaceName")
            resultAttributeNames.add("Place_addr")
            maxResults = 1
        }
        locatorTask.addDoneLoadingListener {
            if (LoadStatus.LOADED == locatorTask.loadStatus) {
                val geocodingResultListenableFuture = locatorTask.geocodeAsync(locationToSearch, geocodeParameters)
                geocodingResultListenableFuture.addDoneListener {
                    geocodingResultListenableFuture.get()?.let { geocodeResults ->
                        if (geocodeResults.size > 0) {
                            val resultToDisplay = geocodeResults.first()
                            arcGisMapView.setViewpointAsync(Viewpoint(resultToDisplay.extent))
                            setupFeatureLayers()
                        } else {
                            Toast.makeText(this@MainActivity, "Location $locationToSearch not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                locatorTask.retryLoadAsync()
            }
        }
        locatorTask.loadAsync()
    }

    private fun setupFeatureLayers() {
        setupAddressPointsFeatureLayer(arcGisMapView.map)
        setupNeighborhoodsFeatureLayer(arcGisMapView.map)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupAddressPointsFeatureLayer(map: ArcGISMap) {
        val serviceFeatureTable = ServiceFeatureTable("https://arcgis.sunvizion.izzi.mx:6443/arcgis/rest/services/staging_ri/Inventory/FeatureServer/38")
        serviceFeatureTable.addDoneLoadingListener {
            addressPointsFeatureLayer = FeatureLayer(serviceFeatureTable).apply {
                minScale = 8000.0
            }

            val simpleRenderer = SimpleRenderer(addressPointSymbol)
            addressPointsFeatureLayer.renderer = simpleRenderer

            val layerAdded = map.operationalLayers?.add(addressPointsFeatureLayer) ?: false
            if (layerAdded) {
                //arcGisMapView.setViewpoint(Viewpoint(viewModel.homePoint.latitude, viewModel.homePoint.longitude, 72000.0))

                /*arcGisMapView.addLayerViewStateChangedListener { layerViewStateChangedEvent ->
                    // get the layer which changed its state
                    val layer = layerViewStateChangedEvent.layer
                    // we only want to check the view state of the image layer
                    if (layer != addressPointsFeatureLayer) {
                        return@addLayerViewStateChangedListener
                    }

                    val layerViewStatus = layerViewStateChangedEvent.layerViewStatus
                    // if there is an error or warning, display it in a toast
                    layerViewStateChangedEvent.error?.let { error ->
                        val message = error.cause?.toString() ?: error.message
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                        Log.i(MainActivity::class.java.simpleName, message!!)
                    }
                    displayViewStateText(layerViewStatus)
                }*/
                arcGisMapView.selectionProperties.color = Color.RED
                callout = arcGisMapView.callout
                arcGisMapView.onTouchListener = object : DefaultMapViewOnTouchListener(this, arcGisMapView) {
                    override fun onSingleTapConfirmed(motionEvent: MotionEvent): Boolean {
                        if (callout.isShowing) {
                            callout.dismiss()
                        }
                        mapGraphicsOverlay.graphics.clear()
                        addressPointsFeatureLayer.clearSelection()

                        motionEvent?.let { event ->
                            val screenPoint = Point(event.x.roundToInt(), event.y.roundToInt())
                            val mapPoint = arcGisMapView.screenToLocation(screenPoint)
                            val tolerance = 10.0

                            val identifyLayerResultFuture = arcGisMapView.identifyLayerAsync(addressPointsFeatureLayer, screenPoint, tolerance, false, -1)
                            identifyLayerResultFuture.addDoneListener {
                                val identifyLayerResult = identifyLayerResultFuture.get()

                                if (identifyLayerResult != null && identifyLayerResult.elements.isNotEmpty()) {
                                    val identifiedFeatures = mutableListOf<Feature>()
                                    identifyLayerResult.elements.forEach { geoElement ->
                                        if (geoElement is Feature) {
                                            identifiedFeatures.add(geoElement)
                                        }
                                    }
                                    addressPointsFeatureLayer.selectFeature(identifiedFeatures.first())
                                    centerFeatureOnMap(identifiedFeatures.first())
                                    showFeatureInfo(identifiedFeatures.first())
                                } else {
                                    val pinGraphic = Graphic(mapPoint, pinSymbol)
                                    mapGraphicsOverlay.graphics.add(pinGraphic)
                                    arcGisMapView.setViewpointCenterAsync(mapPoint)
                                }
                            }
                        }

                        return true
                    }
                }
                //arcGisMapView.map.operationalLayers?.first()?.isVisible = true
            }
        }
        serviceFeatureTable.loadAsync()
    }
}