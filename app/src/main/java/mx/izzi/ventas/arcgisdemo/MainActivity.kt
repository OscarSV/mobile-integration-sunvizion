package mx.izzi.ventas.arcgisdemo

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.esri.arcgisruntime.ArcGISRuntimeEnvironment
import com.esri.arcgisruntime.data.ServiceFeatureTable
import com.esri.arcgisruntime.layers.FeatureLayer
import com.esri.arcgisruntime.mapping.ArcGISMap
import com.esri.arcgisruntime.mapping.BasemapStyle
import com.esri.arcgisruntime.mapping.Viewpoint
import com.esri.arcgisruntime.mapping.view.LayerViewStatus
import com.esri.arcgisruntime.mapping.view.MapView
import com.esri.arcgisruntime.symbology.SimpleFillSymbol
import com.esri.arcgisruntime.symbology.SimpleLineSymbol
import com.esri.arcgisruntime.symbology.SimpleRenderer
import mx.izzi.ventas.arcgisdemo.databinding.ActivityMainBinding
import mx.izzi.ventas.arcgisdemo.viewmodel.MainViewModel
import java.util.*


class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private lateinit var arcGisMapView: MapView

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

        val map = ArcGISMap(BasemapStyle.ARCGIS_TOPOGRAPHIC)
        val serviceFeatureTable = ServiceFeatureTable("https://gis-p.sunvizion.izzi.mx:6443/arcgis/rest/services/prod/Inventory/FeatureServer/8")
        serviceFeatureTable.addDoneLoadingListener {
            val lineSymbol = SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.BLACK, 1.0f)
            val fillSymbol = SimpleFillSymbol(SimpleFillSymbol.Style.SOLID, Color.YELLOW, lineSymbol)
            val featureLayer = FeatureLayer(serviceFeatureTable).apply {
                renderer = SimpleRenderer(fillSymbol)
                opacity = 0.8f
                maxScale = 10000.0
            }

            val layerAdded = map.operationalLayers?.add(featureLayer) ?: false
            if (layerAdded) {
                Toast.makeText(this, "Feature layer added!", Toast.LENGTH_SHORT).show()

                arcGisMapView.map = map
                arcGisMapView.setViewpoint(Viewpoint(viewModel.homePoint.latitude, viewModel.homePoint.longitude, 72000.0))
                arcGisMapView.addLayerViewStateChangedListener { layerViewStateChangedEvent ->
                    // get the layer which changed its state
                    val layer = layerViewStateChangedEvent.layer
                    // we only want to check the view state of the image layer
                    if (layer != featureLayer) {
                        return@addLayerViewStateChangedListener
                    }

                    val layerViewStatus = layerViewStateChangedEvent.layerViewStatus
                    // if there is an error or warning, display it in a toast
                    layerViewStateChangedEvent.error?.let { error ->
                        val message = error.cause?.toString() ?: error.message
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    }
                    displayViewStateText(layerViewStatus)
                }
                arcGisMapView.map.operationalLayers?.first()?.isVisible = true
            }
        }
        serviceFeatureTable.loadAsync()

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
}