package mx.izzi.ventas.arcgisdemo

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.esri.arcgisruntime.ArcGISRuntimeEnvironment
import com.esri.arcgisruntime.geometry.Point
import com.esri.arcgisruntime.geometry.SpatialReferences
import com.esri.arcgisruntime.layers.FeatureLayer
import com.esri.arcgisruntime.mapping.ArcGISMap
import com.esri.arcgisruntime.mapping.BasemapStyle
import com.esri.arcgisruntime.mapping.Viewpoint
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay
import com.esri.arcgisruntime.mapping.view.MapView
import com.google.android.material.snackbar.Snackbar
import mx.izzi.ventas.arcgisdemo.databinding.ActivityMainBinding
import mx.izzi.ventas.arcgisdemo.model.LatLongPoint
import mx.izzi.ventas.arcgisdemo.util.EventObserver
import mx.izzi.ventas.arcgisdemo.viewmodel.MainViewModel

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private lateinit var arcGisMapView: MapView

    private lateinit var graphicsOverlay: GraphicsOverlay
    private lateinit var featureLayer: FeatureLayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        arcGisMapView = binding.arcgisMap

        setupArcGisApiKey()
        observe()
        setupMap()
        /*addGraphics()
        performReverseGeocoding()
        setupView()*/

        viewModel.setupFeatureLayer()
    }

    override fun onResume() {
        super.onResume()

        arcGisMapView.resume()
    }

    private fun setupArcGisApiKey() {
        ArcGISRuntimeEnvironment.setApiKey("AAPK9a286acf48434e399c843b7c16b45307RvC9Iybe8_ztBbEaC3TvAn0nG-S-dHnELfzBp4dW-kFpl9zfcBqP-vlX76krDLnQ")
    }

    private fun setupMap() {
        val map = ArcGISMap(BasemapStyle.ARCGIS_TOPOGRAPHIC)

        arcGisMapView.map = map
        arcGisMapView.setViewpoint(Viewpoint(viewModel.homePoint.latitude, viewModel.homePoint.longitude, 72000.0))
    }

    private fun addGraphics() {
        graphicsOverlay = GraphicsOverlay()
        arcGisMapView.graphicsOverlays.add(graphicsOverlay)

        addPoint()
        addLine()
        addPolygon()
    }

    private fun addPoint () {
        viewModel.processPoint()
    }

    private fun addLine() {
        viewModel.processMapLine()
    }

    private fun addPolygon() {
        viewModel.processMapPolygon()
    }

    private fun performReverseGeocoding() {
        viewModel.performReverseGeocoding(LatLongPoint(19.441682,-99.1615303))
    }

    private fun observe() {
        viewModel.featureLayerReady.observe(this, EventObserver {
            arcGisMapView.setViewpoint(Viewpoint(
                Point(
                    -11000000.0,
                    5000000.0,
                    SpatialReferences.getWebMercator()
                ), 100000000.0
            ))
            arcGisMapView.map.operationalLayers.add(viewModel.featureLayer)

            binding.searchInputLayout.isEnabled = true
            binding.searchOpt.isEnabled = true
        })
        viewModel.mapLineProcessed.observe(this, EventObserver {
            graphicsOverlay.graphics.add(it)
        })
        viewModel.mapPolygonProcessed.observe(this, EventObserver {
            graphicsOverlay.graphics.add(it)
        })
        viewModel.mapPointProcessed.observe(this, EventObserver {
            graphicsOverlay.graphics.add(it)
        })
        viewModel.latLongReverseGeocodeApplied.observe(this, EventObserver { reverseGeocodeList ->
            if (reverseGeocodeList.isEmpty()) {
                Snackbar.make(binding.root, "No address found for point", Snackbar.LENGTH_SHORT).show()
            } else {
                val firstResult = reverseGeocodeList.first()
            }
        })
        viewModel.searchPerformed.observe(this, EventObserver { (resultSearch, envelope) ->
            if (MainViewModel.Companion.ResultSearch.NOT_EMPTY == resultSearch) {
                envelope?.let {
                    arcGisMapView.setViewpointGeometryAsync(it, 50.0)
                }
            }
        })
    }

    private fun setupView() {
        binding.searchOpt.setOnClickListener {
            searchSomething()
        }
    }

    private fun searchSomething() {
        viewModel.performSearch(binding.searchEditText.text.toString().trim())
    }

    override fun onPause() {
        super.onPause()

        arcGisMapView.pause()
    }

    override fun onDestroy() {
        super.onDestroy()

        arcGisMapView.dispose()
    }
}