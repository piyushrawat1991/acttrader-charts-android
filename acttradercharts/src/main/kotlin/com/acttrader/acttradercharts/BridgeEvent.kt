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
        /**
         * Opaque change data serialised as a raw JSON string. On the `MAIN` change, the embedded
         * `lots` field is overridden with the newly-edited qty when the user changed it this session.
         */
        val data: String,
        /** Present on the `MAIN` change when the user edited the lot size this session. */
        val newLots: Double?,
        val bracketOrderLabel: String?,
    )

    /** User confirmed edits to a trade level — main price, SL, TP changes batched together. */
    data class TradeLevelEdit(
        val label: String,
        val type: String,
        /**
         * Opaque level data serialised as a raw JSON string. When the user edited the lot size
         * during this session, the embedded `lots` field is overridden with the new value.
         */
        val data: String,
        val isFullscreen: Boolean,
        /** Present when the user changed the lot size via the QTY pill flyout during this edit session. */
        val newLots: Double?,
        val changes: List<TradeLevelChange>,
    ) : BridgeEvent()

    /**
     * Live qty edit via the QTY pill flyout — fires before the level edit is confirmed,
     * so hosts can refresh Estimated PNL on SL/TP brackets in real time instead of
     * waiting for [TradeLevelEdit] at ✓ confirm. [type] is `"draft"` for the in-progress
     * draft order, otherwise the parent level's type. [previousLots] is the qty at
     * edit-session start (useful for revert-aware previews).
     */
    data class TradeLevelQtyChange(
        val label: String,
        val type: String,
        val newLots: Double,
        val previousLots: Double,
        val isFullscreen: Boolean,
    ) : BridgeEvent()

    /** Chart ✓ button confirmed an edit (including draft orders). */
    data class TradeLevelConfirmed(
        val label: String,
        val type: String,
        val isFullscreen: Boolean,
    ) : BridgeEvent()

    /**
     * An in-progress level edit was cancelled from the chart (ESC key or inline ✕ cancel button).
     * Mirrors [TradeLevelConfirmed] for the revert path. Not fired for draft orders
     * (those emit [DraftCancelled]). Hosts listen to reset their external modify-order panel.
     */
    data class TradeLevelEditCancelled(
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

    /** Emitted after addLevelBracket() / addBracket() auto-places a SL/TP bracket; use the price to populate the form's input. label is an empty string when the bracket was placed on a draft order (no external ID yet) — check label.isEmpty() to detect the draft case. */
    data class TradeLevelBracketActivated(
        val label: String,
        /** `"sl"` or `"tp"`. */
        val bracketType: String,
        val price: Double,
        val isFullscreen: Boolean,
    ) : BridgeEvent()

    /** Emitted when a new draft order is shown on the chart (market, limit, or stop).
     *  Native layer should open the buy/sell form. */
    data class DraftInitiated(
        val side: String,
        val price: Double,
        val orderType: String,
        val isFullscreen: Boolean,
    ) : BridgeEvent()

    /** Emitted when a draft order is cancelled (Escape, ✕ button, or external revert). */
    data class DraftCancelled(
        val label: String,
        val isFullscreen: Boolean,
    ) : BridgeEvent()

    /** TFC (Trade from Charts) was toggled on or off via the top bar button or API. */
    data class TfcToggle(val enabled: Boolean) : BridgeEvent()

    /**
     * Emitted whenever any dismissible chart UI (flyout, modal, dropdown, popover) opens or closes.
     * Paired with [BridgeCommand.DismissAllUI], this lets the hosting Activity decide whether
     * the hardware back button should dismiss chart UI or propagate to normal back navigation.
     *
     * [ActtraderChartsView] already listens for this event internally and mirrors the state into
     * [ActtraderChartsView.hasOpenUI], so most hosts won't need to subscribe directly.
     */
    data class UiStateChange(val hasOpenUI: Boolean) : BridgeEvent()

    /** User tapped the symbol name; fires when `onSymbolClick` is enabled in the init command. */
    data class SymbolClick(val symbol: String) : BridgeEvent()

    /**
     * User triggered a chart snapshot via the camera button. Contains the PNG
     * as a base64-encoded string. Handled internally by [ActtraderChartsView]:
     * `download` saves to `Pictures/ActtraderCharts/`, `copy` copies the image
     * to the clipboard. Hosts can also observe via [ActtraderChartsView.onSnapshotResult].
     */
    data class Snapshot(
        /** `"download"` or `"copy"`. */
        val mode: String,
        val filename: String,
        val mimeType: String,
        /** Base64-encoded PNG (no `data:` prefix). */
        val base64: String,
        val symbol: String,
        val timeframe: String,
        val timestampMs: Long,
    ) : BridgeEvent()

    /** Snapshot hand-off finished successfully on the web side. */
    data class SnapshotTaken(
        val mode: String,
        val filename: String,
        val timestampMs: Long,
    ) : BridgeEvent()

    /** Snapshot failed on the web side (clipboard rejected, canvas blob failed, etc). */
    data class SnapshotError(
        val mode: String,
        val reason: String,
    ) : BridgeEvent()

    /** An error occurred inside the chart engine. */
    data class Error(val message: String, val code: String? = null) : BridgeEvent()
}

/** Parses a raw JSON string from the WebView into a [BridgeEvent].
 *
 *  The bridge sends `{ "type": "...", "payload": { ... } }`.  All fields
 *  are read from the nested `payload` object.
 */
object BridgeEventParser {

    fun parse(json: String): BridgeEvent? = runCatching {
        val obj = JSONObject(json)
        val type = obj.getString("type")

        // Every event except "ready" carries a payload object.
        val p: JSONObject = obj.optJSONObject("payload") ?: JSONObject()

        when (type) {
            "ready" -> BridgeEvent.Ready

            "crosshair" -> {
                val bar = p.getJSONObject("bar")
                val pos = p.optJSONObject("position")
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
                val bar = p.getJSONObject("bar")
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
                val vp = p.getJSONObject("viewport")
                BridgeEvent.ViewportChange(
                    startIndex = vp.getInt("startIndex"),
                    endIndex = vp.getInt("endIndex"),
                    barWidth = vp.getDouble("barWidth"),
                )
            }

            "seriesChange" -> BridgeEvent.SeriesChange(p.getString("series"))

            "timeframeChange" -> BridgeEvent.TimeframeChange(p.getString("timeframe"))

            "durationChange" -> BridgeEvent.DurationChange(p.getString("duration"))

            "stateChange" -> BridgeEvent.StateChange(p.getJSONObject("state").toString())

            "stateSnapshot" -> BridgeEvent.StateSnapshot(p.toString())

            "dataLoaded" -> BridgeEvent.DataLoaded(p.optInt("barCount", 0))

            "newBar" -> {
                val bar = p.getJSONObject("completedBar")
                BridgeEvent.NewBar(
                    time = bar.getLong("time"),
                    open = bar.getDouble("open"),
                    high = bar.getDouble("high"),
                    low = bar.getDouble("low"),
                    close = bar.getDouble("close"),
                    volume = bar.getDouble("volume"),
                )
            }

            "streamStatus" -> BridgeEvent.StreamStatus(p.getString("status"))

            "placeOrder" -> BridgeEvent.PlaceOrder(
                price = p.getDouble("price"),
                side = p.getString("side"),
                orderType = p.optString("orderType", "limit"),
            )

            "dataRequest" -> BridgeEvent.DataRequest(
                requestId = p.getString("requestId"),
                timeframe = p.getString("timeframe"),
                interval  = p.getString("interval"),
                start     = p.getLong("start"),
                end       = p.getLong("end"),
            )

            "tradeLevelClose" -> BridgeEvent.TradeLevelClose(
                label        = p.getString("label"),
                type         = p.getString("type"),
                action       = p.getString("action"),
                data         = p.optJSONObject("data")?.toString() ?: p.optString("data", "{}"),
                bracketType  = p.optString("bracketType").takeIf { it.isNotEmpty() },
                isFullscreen = p.optBoolean("isFullscreen", false),
            )

            "tradeLevelDrag" -> BridgeEvent.TradeLevelDrag(
                label        = p.getString("label"),
                newPrice     = p.getDouble("newPrice"),
                data         = p.optJSONObject("data")?.toString() ?: p.optString("data", "{}"),
                bracketType  = p.optString("bracketType").takeIf { it.isNotEmpty() },
                isFullscreen = p.optBoolean("isFullscreen", false),
            )

            "tradeLevelEdit" -> {
                val rawChanges = p.optJSONArray("changes")
                val changes = if (rawChanges != null) {
                    (0 until rawChanges.length()).map { i ->
                        val c = rawChanges.getJSONObject(i)
                        BridgeEvent.TradeLevelChange(
                            field             = c.getString("field"),
                            newPrice          = c.getDouble("newPrice"),
                            data              = c.optJSONObject("data")?.toString() ?: c.optString("data", "{}"),
                            newLots           = if (c.has("newLots") && !c.isNull("newLots")) c.getDouble("newLots") else null,
                            bracketOrderLabel = c.optString("bracketOrderLabel").takeIf { it.isNotEmpty() },
                        )
                    }
                } else emptyList()
                BridgeEvent.TradeLevelEdit(
                    label        = p.getString("label"),
                    type         = p.getString("type"),
                    data         = p.optJSONObject("data")?.toString() ?: p.optString("data", "{}"),
                    isFullscreen = p.optBoolean("isFullscreen", false),
                    newLots      = if (p.has("newLots") && !p.isNull("newLots")) p.getDouble("newLots") else null,
                    changes      = changes,
                )
            }

            "tradeLevelQtyChange" -> BridgeEvent.TradeLevelQtyChange(
                label        = p.getString("label"),
                type         = p.getString("type"),
                newLots      = p.getDouble("newLots"),
                previousLots = p.getDouble("previousLots"),
                isFullscreen = p.optBoolean("isFullscreen", false),
            )

            "tradeLevelConfirmed" -> BridgeEvent.TradeLevelConfirmed(
                label        = p.getString("label"),
                type         = p.getString("type"),
                isFullscreen = p.optBoolean("isFullscreen", false),
            )

            "tradeLevelEditCancelled" -> BridgeEvent.TradeLevelEditCancelled(
                label        = p.getString("label"),
                type         = p.getString("type"),
                isFullscreen = p.optBoolean("isFullscreen", false),
            )

            "tradeLevelEditOpen" -> BridgeEvent.TradeLevelEditOpen(
                label           = p.getString("label"),
                type            = p.getString("type"),
                data            = p.optJSONObject("data")?.toString() ?: p.optString("data", "{}"),
                price           = p.getDouble("price"),
                side            = p.optString("side").takeIf { it.isNotEmpty() },
                stopLossPrice   = if (p.has("stopLossPrice"))   p.getDouble("stopLossPrice")   else null,
                takeProfitPrice = if (p.has("takeProfitPrice")) p.getDouble("takeProfitPrice") else null,
                isFullscreen    = p.optBoolean("isFullscreen", false),
            )

            "tradeLevelBracketActivated" -> BridgeEvent.TradeLevelBracketActivated(
                label        = p.optString("label", ""),
                bracketType  = p.getString("bracketType"),
                price        = p.getDouble("price"),
                isFullscreen = p.optBoolean("isFullscreen", false),
            )

            "draftInitiated" -> BridgeEvent.DraftInitiated(
                side         = p.getString("side"),
                price        = p.getDouble("price"),
                orderType    = p.getString("orderType"),
                isFullscreen = p.optBoolean("isFullscreen", false),
            )

            "draftCancelled" -> BridgeEvent.DraftCancelled(
                label        = p.getString("label"),
                isFullscreen = p.optBoolean("isFullscreen", false),
            )

            "tfcToggle" -> BridgeEvent.TfcToggle(p.getBoolean("enabled"))

            "uiStateChange" -> BridgeEvent.UiStateChange(p.optBoolean("hasOpenUI", false))

            "symbolClick" -> BridgeEvent.SymbolClick(p.optString("symbol", ""))

            "snapshot" -> BridgeEvent.Snapshot(
                mode        = p.getString("mode"),
                filename    = p.getString("filename"),
                mimeType    = p.optString("mimeType", "image/png"),
                base64      = p.getString("base64"),
                symbol      = p.optString("symbol", ""),
                timeframe   = p.optString("timeframe", ""),
                timestampMs = p.optLong("timestampMs", 0L),
            )

            "snapshotTaken" -> BridgeEvent.SnapshotTaken(
                mode        = p.getString("mode"),
                filename    = p.getString("filename"),
                timestampMs = p.optLong("timestampMs", 0L),
            )

            "snapshotError" -> BridgeEvent.SnapshotError(
                mode   = p.getString("mode"),
                reason = p.optString("reason", "unknown"),
            )

            "error" -> BridgeEvent.Error(
                message = p.optString("message", "Unknown error"),
                code = p.optString("code").takeIf { it.isNotEmpty() },
            )

            else -> null
        }
    }.getOrNull()
}
