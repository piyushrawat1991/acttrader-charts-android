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
        val hideLevelConfirmCancel: Boolean? = null,
        val showSettings: Boolean? = null,
        /** Per-timeframe base interval override for client-side aggregation, e.g. `mapOf("1h" to "30m")`. */
        val aggregateFrom: Map<String, String>? = null,
        /** Per-theme canvas background color overrides as a raw JSON string. */
        val canvasColorsJson: String? = null,
        /** Per-theme deep-partial color overrides for the built-in themes as a raw JSON string. */
        val themeOverridesJson: String? = null,
        /** User-visible string overrides for i18n/localisation as a raw JSON string. */
        val labelsJson: String? = null,
        /** Per-component UI configuration overrides as a raw JSON string. */
        val uiConfigJson: String? = null,
        /** Override the default duration → timeframe pairings, e.g. `mapOf("1Y" to "1D")`. */
        val durationTimeframeMap: Map<String, String>? = null,
        /** When true, fires a `symbolClick` bridge event on symbol tap instead of opening the picker modal. */
        val onSymbolClick: Boolean = false,
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
                hideLevelConfirmCancel?.let { put("hideLevelConfirmCancel", it) }
                showSettings?.let { put("showSettings", it) }
                aggregateFrom?.let { put("aggregateFrom", JSONObject(it)) }
                durationTimeframeMap?.let { put("durationTimeframeMap", JSONObject(it)) }
                canvasColorsJson?.let { runCatching { put("canvasColors", JSONObject(it)) } }
                themeOverridesJson?.let { runCatching { put("themeOverrides", JSONObject(it)) } }
                labelsJson?.let { runCatching { put("labels", JSONObject(it)) } }
                uiConfigJson?.let { runCatching { put("uiConfig", JSONObject(it)) } }
                if (onSymbolClick) put("onSymbolClick", true)
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

    // ── Trade levels ──────────────────────────────────────────────────────────

    /**
     * Replaces all levels of the given [type] with [levels].
     * Each map must contain at least [labelKey] and [priceKey] entries.
     * Optional fields per entry: `side`, `stopLossPrice`, `takeProfitPrice`,
     * `pnl`, `pnlText`, `text`, `lots`, `orderType`, `entryPriceEditable`.
     * @param type `"position"`, `"pending"`, or `"trade"`.
     */
    data class SetLevels(
        val levels: List<Map<String, Any>>,
        val labelKey: String,
        val priceKey: String,
        val type: String,
        val pnlKey: String? = null,
        val pnlTextKey: String? = null,
    ) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setLevels")
            put("payload", JSONObject().apply {
                put("levels", JSONArray().also { arr ->
                    levels.forEach { arr.put(JSONObject(it)) }
                })
                put("labelKey", labelKey)
                put("priceKey", priceKey)
                put("type", type)
                pnlKey?.let { put("pnlKey", it) }
                pnlTextKey?.let { put("pnlTextKey", it) }
            })
        }.toString()
    }

    /** Removes a single level by its label. No-op if not found. */
    data class RemoveLevelByLabel(val label: String) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "removeLevelByLabel")
            put("payload", JSONObject().apply { put("label", label) })
        }.toString()
    }

    /** Updates the entry price of an existing level. */
    data class UpdateLevelMainPrice(val label: String, val price: Double) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "updateLevelMainPrice")
            put("payload", JSONObject().apply {
                put("label", label)
                put("price", price)
            })
        }.toString()
    }

    /**
     * Updates or removes a SL/TP bracket on an existing level.
     * @param bracketType `"sl"` or `"tp"`.
     * @param price Pass `null` to remove the bracket.
     */
    data class UpdateLevelBracket(
        val label: String,
        val bracketType: String,
        val price: Double?,
    ) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "updateLevelBracket")
            put("payload", JSONObject().apply {
                put("label", label)
                put("bracketType", bracketType.lowercase())
                put("price", if (price != null) price else JSONObject.NULL)
            })
        }.toString()
    }

    /**
     * Adds a SL or TP bracket to an existing level at an auto-computed default price.
     * Listen for [BridgeEvent.TradeLevelBracketActivated] to receive the chosen price.
     * @param bracketType `"sl"` or `"tp"`.
     */
    data class AddLevelBracket(
        val label: String,
        val bracketType: String,
    ) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "addLevelBracket")
            put("payload", JSONObject().apply {
                put("label", label)
                put("bracketType", bracketType.lowercase())
            })
        }.toString()
    }

    /**
     * Unified bracket placement — works for both existing levels and the active draft order.
     * Pass [label] (OrderID/TradeID) for an existing level; omit it (null) for the active draft order.
     * Fires [BridgeEvent.TradeLevelBracketActivated] with the auto-computed price.
     * The event's label is null when the bracket was placed on a draft order.
     * @param bracketType `"sl"` or `"tp"`.
     */
    data class AddBracket(
        val bracketType: String,
        val label: String? = null,
    ) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "addBracket")
            put("payload", JSONObject().apply {
                put("bracketType", bracketType.lowercase())
                if (label != null) put("label", label)
            })
        }.toString()
    }

    /**
     * Unified bracket removal — works for both existing levels and the active draft order.
     * Pass [label] (OrderID/TradeID) for an existing level; omit it (null) for the active draft order.
     * @param bracketType `"sl"` or `"tp"`.
     */
    data class RemoveBracket(
        val bracketType: String,
        val label: String? = null,
    ) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "removeBracket")
            put("payload", JSONObject().apply {
                put("bracketType", bracketType.lowercase())
                if (label != null) put("label", label)
            })
        }.toString()
    }

    /** Cancels an in-progress level edit, reverting to the last confirmed price. */
    data class CancelLevelEdit(val label: String) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "cancelLevelEdit")
            put("payload", JSONObject().apply { put("label", label) })
        }.toString()
    }

    /** Programmatically selects a level, or deselects all when [label] is null. */
    data class SelectLevel(val label: String?) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "selectLevel")
            put("payload", JSONObject().apply {
                put("label", if (label != null) label else JSONObject.NULL)
            })
        }.toString()
    }

    // ── Draft orders ──────────────────────────────────────────────────────────

    /**
     * Shows a draggable limit or stop draft order line on the chart.
     * While the user drags it, `tradeLevelDrag` events fire; confirming emits `tradeLevelConfirmed`.
     * @param side `"buy"` or `"sell"`.
     * @param orderType `"limit"` or `"stop"`.
     */
    data class ShowDraftOrder(
        val price: Double,
        val side: String,
        val orderType: String,
    ) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "showDraftOrder")
            put("payload", JSONObject().apply {
                put("price", price)
                put("side", side)
                put("orderType", orderType)
            })
        }.toString()
    }

    /**
     * Shows a non-draggable market-order preview line.
     * SL/TP brackets can be attached via [UpdateDraftOrderBracket].
     * @param side `"buy"` or `"sell"`.
     */
    data class ShowMarketDraft(val price: Double, val side: String) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "showMarketDraft")
            put("payload", JSONObject().apply {
                put("price", price)
                put("side", side)
            })
        }.toString()
    }

    /** Removes any active draft order from the chart. */
    object ClearDraftOrder : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "clearDraftOrder")
            put("payload", JSONObject())
        }.toString()
    }

    /** Cancels whatever is currently being edited or drafted on the chart (draft order or level edit). */
    object CancelCurrentEdit : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "cancelCurrentEdit")
            put("payload", JSONObject())
        }.toString()
    }

    /** Updates the lot quantity shown on the active draft order chip. */
    data class SetDraftOrderLots(val lots: Double) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setDraftOrderLots")
            put("payload", JSONObject().apply { put("lots", lots) })
        }.toString()
    }

    /** Moves the draft order price line to a new price. */
    data class UpdateDraftOrderPrice(val price: Double) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "updateDraftOrderPrice")
            put("payload", JSONObject().apply { put("price", price) })
        }.toString()
    }

    /**
     * Updates or removes a SL/TP bracket on the active draft order.
     * @param bracketType `"sl"` or `"tp"`.
     * @param price Pass `null` to remove the bracket.
     */
    data class UpdateDraftOrderBracket(val bracketType: String, val price: Double?) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "updateDraftOrderBracket")
            put("payload", JSONObject().apply {
                put("bracketType", bracketType.lowercase())
                put("price", if (price != null) price else JSONObject.NULL)
            })
        }.toString()
    }

    /**
     * Sets or clears the estimated PNL text shown on a draft order's SL or TP bracket line.
     * Pass `null` for [pnlText] to clear.
     * @param bracketType `"sl"` or `"tp"`.
     */
    data class SetDraftBracketPnl(val bracketType: String, val pnlText: String?) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setDraftBracketPnl")
            put("payload", JSONObject().apply {
                put("bracketType", bracketType.lowercase())
                put("pnlText", if (pnlText != null) pnlText else JSONObject.NULL)
            })
        }.toString()
    }

    // ── UI controls ───────────────────────────────────────────────────────────

    /** Shows or hides the volume sub-pane. */
    data class SetVolume(val show: Boolean) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setVolume")
            put("payload", JSONObject().apply { put("show", show) })
        }.toString()
    }

    /** Updates the symbol list used by the ISIN picker modal after initial setup. */
    data class SetIsins(val isins: List<String>) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setIsins")
            put("payload", JSONObject().apply { put("isins", JSONArray(isins)) })
        }.toString()
    }

    /** Resets both price and time axes to their default auto-fit state. */
    object ResetView : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "resetView")
            put("payload", JSONObject())
        }.toString()
    }

    /** Shows or hides the loading overlay. */
    data class SetLoading(val loading: Boolean) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "setLoading")
            put("payload", JSONObject().apply { put("loading", loading) })
        }.toString()
    }

    /**
     * Replaces a specific bar with authoritative OHLCV data (e.g. a correction from the server).
     * @param barTime Unix millisecond timestamp of the bar to replace.
     */
    data class CorrectBar(val barTime: Long, val bar: OHLCVBar) : BridgeCommand() {
        override fun toJson(): String = JSONObject().apply {
            put("type", "correctBar")
            put("payload", JSONObject().apply {
                put("barTime", barTime)
                put("bar", JSONObject().apply {
                    put("open", bar.open)
                    put("high", bar.high)
                    put("low", bar.low)
                    put("close", bar.close)
                    put("volume", bar.volume)
                    put("time", bar.time)
                })
            })
        }.toString()
    }
}
