package mx.izzi.ventas.arcgisdemo.viewmodel

import android.graphics.Color
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esri.arcgisruntime.data.Feature
import com.esri.arcgisruntime.data.FeatureQueryResult
import com.esri.arcgisruntime.data.QueryParameters
import com.esri.arcgisruntime.data.ServiceFeatureTable
import com.esri.arcgisruntime.geometry.*
import com.esri.arcgisruntime.layers.FeatureLayer
import com.esri.arcgisruntime.mapping.view.Graphic
import com.esri.arcgisruntime.symbology.SimpleFillSymbol
import com.esri.arcgisruntime.symbology.SimpleLineSymbol
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol
import com.esri.arcgisruntime.symbology.SimpleRenderer
import com.esri.arcgisruntime.tasks.geocode.GeocodeResult
import com.esri.arcgisruntime.tasks.geocode.LocatorTask
import com.esri.arcgisruntime.tasks.geocode.ReverseGeocodeParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mx.izzi.ventas.arcgisdemo.model.LatLongPoint
import mx.izzi.ventas.arcgisdemo.util.Event
import java.util.*

class MainViewModel: ViewModel() {

    private val _mapLineProcessed = MutableLiveData<Event<Graphic>>()
    val mapLineProcessed: LiveData<Event<Graphic>> = _mapLineProcessed

    private val _mapPolygonProcessed = MutableLiveData<Event<Graphic>>()
    val mapPolygonProcessed: LiveData<Event<Graphic>> = _mapPolygonProcessed

    private val _mapPointProcessed = MutableLiveData<Event<Graphic>>()
    val mapPointProcessed: LiveData<Event<Graphic>> = _mapPointProcessed

    private val _latLongReverseGeocodeApplied = MutableLiveData<Event<List<GeocodeResult>>>()
    val latLongReverseGeocodeApplied: LiveData<Event<List<GeocodeResult>>> = _latLongReverseGeocodeApplied

    private val _serviceFeatureTableReady = MutableLiveData<Event<ServiceFeatureTable>>()
    val serviceFeatureTableReady: LiveData<Event<ServiceFeatureTable>> = _serviceFeatureTableReady

    private val _featureLayerReady = MutableLiveData<Event<Unit>>()
    val featureLayerReady: LiveData<Event<Unit>> = _featureLayerReady

    private val _locatorTaskReady = MutableLiveData<Event<Unit>>()
    val locatorTaskReady: LiveData<Event<Unit>> = _locatorTaskReady

    private val _searchPerformed = MutableLiveData<Event<Pair<ResultSearch, Envelope?>>>()
    val searchPerformed: LiveData<Event<Pair<ResultSearch, Envelope?>>> = _searchPerformed

    val homePoint = LatLongPoint(19.4411085,-99.1625439)
    private val homeToMbPointList: List<LatLongPoint> = listOf(
        LatLongPoint(19.441260, -99.162603),
        LatLongPoint(19.441072, -99.161836),
        LatLongPoint(19.439909, -99.162190),
        LatLongPoint(19.439201, -99.159111),
        LatLongPoint(19.438174, -99.156509),
        LatLongPoint(19.436085, -99.157410)
    )
    private val homeNeighborhood: List<LatLongPoint> = listOf(
        LatLongPoint(19.443476, -99.165579),
        LatLongPoint(19.443172, -99.165085),
        LatLongPoint(19.442580, -99.162417),
        LatLongPoint(19.439820, -99.155748),
        LatLongPoint(19.432322, -99.159094),
        LatLongPoint(19.434257, -99.163688),
        LatLongPoint(19.434873, -99.165891),
        LatLongPoint(19.436379, -99.169576),
        LatLongPoint(19.439908, -99.167419)
    )

    private lateinit var locatorTask: LocatorTask
    lateinit var serviceFeatureTable: ServiceFeatureTable
    lateinit var featureLayer: FeatureLayer

    init {
        /*viewModelScope.launch(Dispatchers.IO) {
            setupFeatureLayer()
        }*/
        viewModelScope.launch(Dispatchers.IO) {
            locatorTask = LocatorTask("https://geocode.arcgis.com/arcgis/rest/services/World/GeocodeServer")
            _locatorTaskReady.postValue(Event(Unit))
        }
    }

    fun processPoint() {
        viewModelScope.launch(Dispatchers.IO) {
            val point = Point(homePoint.longitude, homePoint.latitude, SpatialReferences.getWgs84())

            val markerSymbol = SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, -0xa8cd, 10f)
            val blueOutlineSymbol = SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, -0xff9c01, 4f)
            markerSymbol.outline = blueOutlineSymbol

            val pointGraphic = Graphic(point, markerSymbol)
            _mapPointProcessed.postValue(Event(pointGraphic))
        }
    }

    fun processMapLine() {
        viewModelScope.launch(Dispatchers.IO) {
            val pointCollection = PointCollection(SpatialReferences.getWgs84()).apply {
                homeToMbPointList.forEach {
                    add(Point(it.longitude, it.latitude))
                }
            }

            val polyline = Polyline(pointCollection)
            val lineSymbol = SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, -0xff9c01, 3f)

            val lineGraphic = Graphic(polyline, lineSymbol)

            _mapLineProcessed.postValue(Event(lineGraphic))
        }
    }

    fun processMapPolygon() {
        viewModelScope.launch(Dispatchers.IO) {
            val pointCollection = PointCollection(SpatialReferences.getWgs84()).apply {
                homeNeighborhood.forEach {
                    add(Point(it.longitude, it.latitude))
                }
                homeNeighborhood.first().apply {
                    add(Point(longitude, latitude))
                }
            }

            val polygon = Polygon(pointCollection)
            val blueOutlineSymbol = SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, -0xff9c01, 4f)
            val polygonSymbol = SimpleFillSymbol(SimpleFillSymbol.Style.SOLID, -0x7f00a8cd, blueOutlineSymbol)

            val polygonGraphic = Graphic(polygon, polygonSymbol)
            _mapPolygonProcessed.postValue(Event(polygonGraphic))
        }
    }

    fun performReverseGeocoding(latLongPoint: LatLongPoint) {
        val reverseGeocodeParameters = ReverseGeocodeParameters().apply {
            maxResults = 1
            maxDistance = 100.0 //Meters
            outputLanguageCode = "ES"
        }

        val locationPoint = Point(latLongPoint.longitude, latLongPoint.latitude, SpatialReferences.getWgs84())
        val reverseGeocodeResultFuture = locatorTask.reverseGeocodeAsync(locationPoint)
        reverseGeocodeResultFuture.addDoneListener {
            reverseGeocodeResultFuture.get()?.let { mutableGeocodeResultList ->
                _latLongReverseGeocodeApplied.value = Event(mutableGeocodeResultList.toList())
            }
        }
    }

    fun setupFeatureLayer() {
        //serviceFeatureTable = ServiceFeatureTable("https://services.arcgis.com/jIL9msH9OI208GCb/arcgis/rest/services/USA_Daytime_Population_2016/FeatureServer/0")
        serviceFeatureTable = ServiceFeatureTable("https://gis-p.sunvizion.izzi.mx:6443/arcgis/rest/services/prod/Inventory/FeatureServer/8")
        serviceFeatureTable.loadAsync()

        val lineSymbol = SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.BLACK, 1.0f)
        val fillSymbol = SimpleFillSymbol(SimpleFillSymbol.Style.SOLID, Color.YELLOW, lineSymbol)

        featureLayer = FeatureLayer(serviceFeatureTable).apply {
            renderer = SimpleRenderer(fillSymbol)
            opacity = 0.8f
            maxScale = 10000.0
        }

        _featureLayerReady.postValue(Event(Unit))
    }

    fun performSearch(textToSearch: String) {
        featureLayer.clearSelection()
        viewModelScope.launch(Dispatchers.IO) {
            val queryParameters = QueryParameters()
            queryParameters.whereClause = "upper(STATE_NAME) LIKE '${textToSearch.uppercase(Locale.US)}'"

            val listenableFuture = serviceFeatureTable.queryFeaturesAsync(queryParameters)
            listenableFuture.addDoneListener {
                processSearchResults(listenableFuture.get())
            }
        }
    }

    private fun processSearchResults(featureQueryResult: FeatureQueryResult) {
        val resultsList: List<Feature> = featureQueryResult.toList()
        if (resultsList.any()) {
            val envelopeBuilder = EnvelopeBuilder(SpatialReferences.getWebMercator())
            resultsList.first().let { feature ->
                envelopeBuilder.unionOf(feature.geometry.extent)
                featureLayer.selectFeature(feature)
            }
            _searchPerformed.postValue(Event(Pair(ResultSearch.NOT_EMPTY, envelopeBuilder.toGeometry())))
        } else {
            _searchPerformed.postValue(Event(Pair(ResultSearch.EMPTY, null)))
        }
    }

    companion object {
        val TAG = MainViewModel::class.java.simpleName

        enum class ResultSearch{
            EMPTY, NOT_EMPTY
        }
    }
}