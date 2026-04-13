package com.acttrader.acttradercharts

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

    /**
     * Chart engine is requesting data for a time range.
     * Respond by calling [ActtraderChartsView.resolveDataRequest] with the fetched bars.
     *
     * @param requestId Correlation ID — must be passed back to [ActtraderChartsView.resolveDataRequest].
     * @param timeframe Current chart timeframe (e.g. `"1h"`).
     * @param interval  Base interval string passed to the data loader (e.g. `"1hour"`).
     * @param start     Range start in milliseconds since epoch.
     * @param end       Range end in milliseconds since epoch.
     */
    data class DataRequest(
        val requestId: String,
        val timeframe: String,
        val interval: String,
        val start: Long,
        val end: Long,
    ) : BridgeEvent()

    /** User tapped × to close or cancel a trade level or remove a bracket. */
    data class TradeLevelClose(
        val label: String,
        val type: String,
        /** `"CLOSED"`, `"CANCELLED"`, `"REMOVE_SL"`, or `"REMOVE_TP"`. */
        val action: String,
        /** Opaque level data serialised as a raw JSON string. */
        val data: String,
        /** `"SL"` or `"TP"` when the × removes a bracket; `null` otherwise. */
        val bracketType: String?,
        val isFullscreen: Boolean,
    ) : BridgeEvent()

    /** Live drag position — fires on every pointer move while a level or bracket is dragged. */
    data class TradeLevelDrag(
        val label: String,
        val newPrice: Double,
        /** Opaque level data serialised as a raw JSON string. */
        val data: String,
        /** `"stopLoss"` or `"takeProfit"` when a bracket is being dragged; `null` for main level. */
        val bracketType: String?,
        val isFullscreen: Boolean,
    ) : BridgeEvent()

    /** A single change entry within a [TradeLevelEdit] event. */
    data class TradeLevelChange(
        /** `"MAIN"`, `"SL"`, `"TP"`, `"ADD_SL"`, `"ADD_TP"`, `"REMOVE_SL"`, or `"REMOVE_TP"`. */
        val field: String,
        val newPrice: Double,
        /** Opaque change data serialised as a raw JSON string. */
        val data: String,
        val bracketOrderLabel: String?,
    )

    /** User confirmed edits to a trade level — main price, SL, TP changes batched together. */
    data class TradeLevelEdit(
        val label: String,
        val type: String,
        /** Opaque level data serialised as a raw JSON string. */
        val data: String,
        val isFullscreen: Boolean,
        val changes: List<TradeLevelChange>,
    ) : BridgeEvent()

    /** Chart ✓ button confirmed an edit (including draft orders). */
    data class TradeLevelConfirmed(
        val label: String,
        val type: String,
        val isFullscreen: Boolean,
    ) : BridgeEvent()

    /** User tapped the pencil/edit button to open the order panel for a level. */
    data class TradeLevelEditOpen(
        val label: String,
        val type: String,
        /** Opaque level data serialised as a raw JSON string. */
        val data: String,
        val price: Double,
        val side: String?,
        val stopLossPrice: Double?,
        val takeProfitPrice: Double?,
        val isFullscreen: Boolean,
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

            "dataRequest" -> {
                val p = obj.getJSONObject("payload")
                BridgeEvent.DataRequest(
                    requestId = p.getString("requestId"),
                    timeframe = p.getString("timeframe"),
                    interval  = p.getString("interval"),
                    start     = p.getLong("start"),
                    end       = p.getLong("end"),
                )
            }

            "tradeLevelClose" -> {
                val p = obj.getJSONObject("payload")
                BridgeEvent.TradeLevelClose(
                    label        = p.getString("label"),
                    type         = p.getString("type"),
                    action       = p.getString("action"),
                    data         = p.optJSONObject("data")?.toString() ?: p.optString("data", "{}"),
                    bracketType  = p.optString("bracketType").takeIf { it.isNotEmpty() },
                    isFullscreen = p.optBoolean("isFullscreen", false),
                )
            }

            "tradeLevelDrag" -> {
                val p = obj.getJSONObject("payload")
                BridgeEvent.TradeLevelDrag(
                    label        = p.getString("label"),
                    newPrice     = p.getDouble("newPrice"),
                    data         = p.optJSONObject("data")?.toString() ?: p.optString("data", "{}"),
                    bracketType  = p.optString("bracketType").takeIf { it.isNotEmpty() },
                    isFullscreen = p.optBoolean("isFullscreen", false),
                )
            }

            "tradeLevelEdit" -> {
                val p = obj.getJSONObject("payload")
                val rawChanges = p.optJSONArray("changes")
                val changes = if (rawChanges != null) {
                    (0 until rawChanges.length()).map { i ->
                        val c = rawChanges.getJSONObject(i)
                        BridgeEvent.TradeLevelChange(
                            field             = c.getString("field"),
                            newPrice          = c.getDouble("newPrice"),
                            data              = c.optJSONObject("data")?.toString() ?: c.optString("data", "{}"),
                            bracketOrderLabel = c.optString("bracketOrderLabel").takeIf { it.isNotEmpty() },
                        )
                    }
                } else emptyList()
                BridgeEvent.TradeLevelEdit(
                    label        = p.getString("label"),
                    type         = p.getString("type"),
                    data         = p.optJSONObject("data")?.toString() ?: p.optString("data", "{}"),
                    isFullscreen = p.optBoolean("isFullscreen", false),
                    changes      = changes,
                )
            }

            "tradeLevelConfirmed" -> {
                val p = obj.getJSONObject("payload")
                BridgeEvent.TradeLevelConfirmed(
                    label        = p.getString("label"),
                    type         = p.getString("type"),
                    isFullscreen = p.optBoolean("isFullscreen", false),
                )
            }

            "tradeLevelEditOpen" -> {
                val p = obj.getJSONObject("payload")
                BridgeEvent.TradeLevelEditOpen(
                    label           = p.getString("label"),
                    type            = p.getString("type"),
                    data            = p.optJSONObject("data")?.toString() ?: p.optString("data", "{}"),
                    price           = p.getDouble("price"),
                    side            = p.optString("side").takeIf { it.isNotEmpty() },
                    stopLossPrice   = if (p.has("stopLossPrice"))   p.getDouble("stopLossPrice")   else null,
                    takeProfitPrice = if (p.has("takeProfitPrice")) p.getDouble("takeProfitPrice") else null,
                    isFullscreen    = p.optBoolean("isFullscreen", false),
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
