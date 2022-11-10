package mx.izzi.ventas.arcgisdemo.util

import com.esri.arcgisruntime.data.Feature
import com.esri.arcgisruntime.geometry.GeometryEngine
import com.esri.arcgisruntime.geometry.Point
import com.esri.arcgisruntime.geometry.SpatialReferences

fun Feature.stringAttributes(): String {
    val stringBuilder = StringBuilder()
    for (entry in attributes) {
        stringBuilder.append("${entry.key} -> ${entry.value.toString()}\n")
    }
    return stringBuilder.toString()
}

fun Point.toWgs84Format(): Point {
    return GeometryEngine.project(this, SpatialReferences.getWgs84()) as Point
}