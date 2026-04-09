package com.acttrader.stockchart

import org.json.JSONObject

/**
 * Events emitted from the chart WebView back to native Android code.
 */
sealed class BridgeEvent {

    /** Chart engine is initialised and ready to receive commands. */
    object Ready : BridgeEvent()

    /** Crosshair moved; contains the bar data at the cursor position. */
    data class Crosshair(
        val time: Long,
        val open: Double,
        val high: Double,
        val low: Double,
        val close: Double,
        val volume: Double,
        val x: Double,
        val y: Double,
    ) : BridgeEvent()

    /** User tapped/clicked a bar. */
    data class BarClick(
        val time: Long,
        val open: Double,
        val high: Double,
        val low: Double,
        val close: Double,
        val volume: Double,
    ) : BridgeEvent()

    /** Viewport scroll or zoom changed. */
    data class ViewportChange(
        val startIndex: Int,
        val endIndex: Int,
        val barWidth: Double,
    ) : BridgeEvent()

    /** Active chart series type changed. */
    data class SeriesChange(val series: String) : BridgeEvent()

    /** Active timeframe changed. */
    data class TimeframeChange(val timeframe: String) : BridgeEvent()

    /** Active duration changed. */
    data class DurationChange(val duration: String) : BridgeEvent()

    /** Any aspect of chart state changed (generic). */
    data class StateChange(val stateJson: String) : BridgeEvent()

    /** Response to a `GetState` command; contains the full serialised state. */
    data class StateSnapshot(val stateJson: String) : BridgeEvent()

    /** `loadData` command completed successfully. */
    data class DataLoaded(val barCount: Int) : BridgeEvent()

    /** A new bar was appended (live edge shift). */
    data class NewBar(
        val time: Long,
        val open: Double,
        val high: Double,
        val low: Double,
        val close: Double,
        val volume: Double,
    ) : BridgeEvent()

    /** Stream connection status changed. */
    data class StreamStatus(val status: String) : BridgeEvent()

    /** User submitted an order via the floating trade button. */
    data class PlaceOrder(
        val price: Double,
        val side: String,
        val orderType: String,
    ) : BridgeEvent()

    /** An error occurred inside the chart engine. */
    data class Error(val message: String, val code: String? = null) : BridgeEvent()
}

/** Parses a raw JSON string from the WebView into a [BridgeEvent]. */
object BridgeEventParser {

    fun parse(json: String): BridgeEvent? = runCatching {
        val obj = JSONObject(json)
        when (obj.getString("type")) {
            "ready" -> BridgeEvent.Ready

            "crosshair" -> {
                val bar = obj.getJSONObject("bar")
                val pos = obj.optJSONObject("position")
                BridgeEvent.Crosshair(
                    time = bar.getLong("time"),
                    open = bar.getDouble("open"),
                    high = bar.getDouble("high"),
                    low = bar.getDouble("low"),
                    close = bar.getDouble("close"),
                    volume = bar.getDouble("volume"),
                    x = pos?.optDouble("x") ?: 0.0,
                    y = pos?.optDouble("y") ?: 0.0,
                )
            }

            "barClick" -> {
                val bar = obj.getJSONObject("bar")
                BridgeEvent.BarClick(
                    time = bar.getLong("time"),
                    open = bar.getDouble("open"),
                    high = bar.getDouble("high"),
                    low = bar.getDouble("low"),
                    close = bar.getDouble("close"),
                    volume = bar.getDouble("volume"),
                )
            }

            "viewportChange" -> {
                val vp = obj.getJSONObject("viewport")
                BridgeEvent.ViewportChange(
                    startIndex = vp.getInt("startIndex"),
                    endIndex = vp.getInt("endIndex"),
                    barWidth = vp.getDouble("barWidth"),
                )
            }

            "seriesChange" -> BridgeEvent.SeriesChange(obj.getString("series"))

            "timeframeChange" -> BridgeEvent.TimeframeChange(obj.getString("timeframe"))

            "durationChange" -> BridgeEvent.DurationChange(obj.getString("duration"))

            "stateChange" -> BridgeEvent.StateChange(obj.getJSONObject("state").toString())

            "stateSnapshot" -> BridgeEvent.StateSnapshot(obj.getJSONObject("state").toString())

            "dataLoaded" -> BridgeEvent.DataLoaded(obj.optInt("barCount", 0))

            "newBar" -> {
                val bar = obj.getJSONObject("bar")
                BridgeEvent.NewBar(
                    time = bar.getLong("time"),
                    open = bar.getDouble("open"),
                    high = bar.getDouble("high"),
                    low = bar.getDouble("low"),
                    close = bar.getDouble("close"),
                    volume = bar.getDouble("volume"),
                )
            }

            "streamStatus" -> BridgeEvent.StreamStatus(obj.getString("status"))

            "placeOrder" -> {
                val p = obj.getJSONObject("payload")
                BridgeEvent.PlaceOrder(
                    price = p.getDouble("price"),
                    side = p.getString("side"),
                    orderType = p.optString("orderType", "limit"),
                )
            }

            "error" -> BridgeEvent.Error(
                message = obj.optString("message", "Unknown error"),
                code = obj.optString("code").takeIf { it.isNotEmpty() },
            )

            else -> null
        }
    }.getOrNull()
}
