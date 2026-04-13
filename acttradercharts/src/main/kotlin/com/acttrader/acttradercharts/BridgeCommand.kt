package com.acttrader.acttradercharts

import org.json.JSONArray
import org.json.JSONObject

/**
 * Commands sent from native Android code to the chart WebView.
 * Each subclass serialises itself to the JSON format expected by `window.ChartBridge.send()`.
 *
 * Protocol shape: `{ "type": "<cmd>", "payload": { ...fields } }`
 */
sealed class BridgeCommand {

    abstract fun toJson(): String

    // ── Core ──────────────────────────────────────────────────────────────────

    /** Re-creates the chart engine. Sent once after the WebView finishes loading. */
    data class Init(
        val theme: String = "dark",
        val symbol: String? = null,
        val series: String? = null,
        val timeframe: String? = null,
        val duration: String? = null,
        val enableTrading: Boolean = false,
        val minLots: Int = 1,
        val showVolume: Boolean? = null,
        val showUI: Boolean? = null,
        val showDrawingTools: Boolean? = null,
        val showBidAskLines: Boolean? = null,
        val showActLogo: Boolean? = null,
        val showCandleCountdown: Boolean? = null,
        val candleCountdownTimeframes: List<String>? = null,
        val disableCountdownOnMobile: Boolean? = null,
        val maxSubPanes: Int? = null,
        val mobileBarDivisor: Int? = null,
        val targetCandleWidth: Double? = null,
        val tickClosePriceSource: String? = null,
        val tradesThresholdForHorizontalLine: Int? = null,
        val tradeDisplayFilter: String? = null,
        val positionRenderStyle: String? = null,
    ) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "init")
            put("payload", JSONObject().apply {
                put("theme", theme)
                symbol?.let { put("symbol", it) }
                series?.let { put("series", it) }
                timeframe?.let { put("timeframe", it) }
                duration?.let { put("duration", it) }
                if (enableTrading) {
                    put("enableTrading", true)
                    put("minLots", minLots)
                }
                showVolume?.let { put("showVolume", it) }
                showUI?.let { put("showUI", it) }
                showDrawingTools?.let { put("showDrawingTools", it) }
                showBidAskLines?.let { put("showBidAskLines", it) }
                showActLogo?.let { put("showActLogo", it) }
                showCandleCountdown?.let { put("showCandleCountdown", it) }
                candleCountdownTimeframes?.let { put("candleCountdownTimeframes", JSONArray(it)) }
                disableCountdownOnMobile?.let { put("disableCountdownOnMobile", it) }
                maxSubPanes?.let { put("maxSubPanes", it) }
                mobileBarDivisor?.let { put("mobileBarDivisor", it) }
                targetCandleWidth?.let { put("targetCandleWidth", it) }
                tickClosePriceSource?.let { put("tickClosePriceSource", it) }
                tradesThresholdForHorizontalLine?.let { put("tradesThresholdForHorizontalLine", it) }
                tradeDisplayFilter?.let { put("tradeDisplayFilter", it) }
                positionRenderStyle?.let { put("positionRenderStyle", it) }
            })
        }.toString()
    }

    /** Replaces the full dataset. */
    data class LoadData(
        val bars: List<OHLCVBar>,
        val fitAll: Boolean = false,
    ) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "loadData")
            put("payload", JSONObject().apply {
                put("bars", JSONArray().also { arr ->
                    bars.forEach { bar ->
                        arr.put(JSONObject().apply {
                            put("open", bar.open)
                            put("high", bar.high)
                            put("low", bar.low)
                            put("close", bar.close)
                            put("volume", bar.volume)
                            put("time", bar.time)
                        })
                    }
                })
                put("fitAll", fitAll)
            })
        }.toString()
    }

    /** Pushes a live tick (bid/ask/timestamp) for streaming updates. */
    data class PushTick(
        val bid: Double,
        val ask: Double,
        val timestamp: Long,
    ) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "pushTick")
            put("payload", JSONObject().apply {
                put("B", bid)
                put("A", ask)
                put("T", timestamp)
            })
        }.toString()
    }

    // ── Appearance ────────────────────────────────────────────────────────────

    /** Switches between `"dark"` and `"light"` themes. */
    data class SetTheme(val theme: String) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setTheme")
            put("payload", JSONObject().apply {
                put("theme", theme)
            })
        }.toString()
    }

    /** Changes the chart series type (e.g. `"candlestick"`, `"line"`, `"area"`). */
    data class SetSeries(val series: String) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setSeries")
            put("payload", JSONObject().apply {
                put("series", series)
            })
        }.toString()
    }

    /** Changes the active timeframe (e.g. `"1m"`, `"1h"`, `"1D"`). */
    data class SetTimeframe(val timeframe: String) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setTimeframe")
            put("payload", JSONObject().apply {
                put("timeframe", timeframe)
            })
        }.toString()
    }

    /** Updates the displayed symbol name. */
    data class SetSymbol(val symbol: String) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setSymbol")
            put("payload", JSONObject().apply {
                put("symbol", symbol)
            })
        }.toString()
    }

    // ── Studies / Drawings ────────────────────────────────────────────────────

    /**
     * Adds a study overlay or oscillator by short name (e.g. `"SMA"`, `"RSI"`).
     * @param params Optional key-value parameters (e.g. `mapOf("period" to 20)`).
     */
    data class AddIndicator(
        val name: String,
        val params: Map<String, Any>? = null,
    ) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "addIndicator")
            put("payload", JSONObject().apply {
                put("shortName", name)
                params?.let { put("params", JSONObject(it)) }
            })
        }.toString()
    }

    /** Removes a study by name. */
    data class RemoveIndicator(val name: String) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "removeIndicator")
            put("payload", JSONObject().apply {
                put("name", name)
            })
        }.toString()
    }

    /** Activates a drawing tool by ID, or passes `null` to deactivate. */
    data class SetDrawingTool(val tool: String?) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setDrawingTool")
            put("payload", JSONObject().apply {
                if (tool != null) put("tool", tool) else put("tool", JSONObject.NULL)
            })
        }.toString()
    }

    /** Removes all drawings from the chart. */
    object ClearAllDrawings : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "clearAllDrawings")
            put("payload", JSONObject())
        }.toString()
    }

    // ── State ─────────────────────────────────────────────────────────────────

    /** Requests the current chart state; fires a `stateSnapshot` event in response. */
    object GetState : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "getState")
            put("payload", JSONObject())
        }.toString()
    }

    /**
     * Restores a previously captured chart state.
     * @param stateJson Raw JSON string returned by a prior `stateSnapshot` event.
     */
    data class SetState(val stateJson: String) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setState")
            put("payload", JSONObject(stateJson))
        }.toString()
    }

    /**
     * Resolves a pending dataLoader request with fetched bars.
     * @param requestId The ID received in the `dataRequest` bridge event.
     * @param bars The fetched OHLCV bars to return to the chart engine.
     */
    data class ResolveDataRequest(
        val requestId: String,
        val bars: List<OHLCVBar>,
    ) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "resolveDataRequest")
            put("payload", JSONObject().apply {
                put("requestId", requestId)
                put("bars", JSONArray().also { arr ->
                    bars.forEach { bar ->
                        arr.put(JSONObject().apply {
                            put("open", bar.open)
                            put("high", bar.high)
                            put("low", bar.low)
                            put("close", bar.close)
                            put("volume", bar.volume)
                            put("time", bar.time)
                        })
                    }
                })
            })
        }.toString()
    }

    /** Enables or disables verbose tick/render logging in the chart engine. */
    data class SetDebug(val enabled: Boolean) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setDebug")
            put("payload", JSONObject().apply {
                put("enabled", enabled)
            })
        }.toString()
    }

    /** Destroys the chart engine and releases resources. */
    object Destroy : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "destroy")
            put("payload", JSONObject())
        }.toString()
    }
}
