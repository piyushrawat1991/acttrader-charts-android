package com.acttrader.acttradercharts

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout

/**
 * A self-contained chart view that renders `acttrader-charts` inside a [WebView].
 *
 * ## Basic usage
 * ```kotlin
 * val chart = ActtraderChartsView(context)
 * chart.onReady = { chart.loadData(bars) }
 * chart.onError = { err -> Log.e("Chart", err.message) }
 * parentLayout.addView(chart)
 * ```
 *
 * All public command methods are safe to call from any thread.
 */
class ActtraderChartsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    // ── WebView setup ─────────────────────────────────────────────────────────

    private val mainHandler = Handler(Looper.getMainLooper())

    @SuppressLint("SetJavaScriptEnabled")
    private val webView = WebView(context).also { wv ->
        wv.layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT,
        )
        wv.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            allowFileAccess = true
        }
        wv.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            wv.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, true)
        }
        wv.webViewClient = WebViewClient()
        addView(wv)
    }

    // Loading skeleton — shown until the `ready` event fires to eliminate the blank-flash
    // on WebView cold-start. Background matches the dark theme canvas colour.
    private val skeletonView = View(context).also { v ->
        v.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        v.setBackgroundColor(Color.parseColor("#13151a"))
        addView(v)
    }

    private val bridgeInterface = ChartBridgeInterface(mainHandler, null).also { iface ->
        webView.addJavascriptInterface(iface, "ChartAndroidBridge")
        iface.onBridgeEvent = { event -> dispatchEvent(event) }
    }

    // ── Command queue ─────────────────────────────────────────────────────────

    @Volatile private var isReady = false
    @Volatile private var hasCalledOnReady = false
    private val commandQueue = ArrayList<String>()  // guarded by synchronized(this)

    init {
        webView.loadUrl("file:///android_asset/chart.html")
    }

    // ── Typed event callbacks (set by the consumer) ───────────────────────────

    /** Called when the chart engine is ready to receive commands. */
    var onReady: (() -> Unit)? = null

    /** Called whenever the crosshair moves over a bar. */
    var onCrosshair: ((BridgeEvent.Crosshair) -> Unit)? = null

    /** Called when the user taps a bar. */
    var onBarClick: ((BridgeEvent.BarClick) -> Unit)? = null

    /** Called when the visible viewport changes (pan / zoom). */
    var onViewportChange: ((BridgeEvent.ViewportChange) -> Unit)? = null

    /** Called when the active series type changes. */
    var onSeriesChange: ((BridgeEvent.SeriesChange) -> Unit)? = null

    /** Called when the active timeframe changes. */
    var onTimeframeChange: ((BridgeEvent.TimeframeChange) -> Unit)? = null

    /** Called when the active duration changes. */
    var onDurationChange: ((BridgeEvent.DurationChange) -> Unit)? = null

    /** Called on any chart state mutation. */
    var onStateChange: ((BridgeEvent.StateChange) -> Unit)? = null

    /** Called in response to [getState]; contains the full serialised state JSON. */
    var onStateSnapshot: ((BridgeEvent.StateSnapshot) -> Unit)? = null

    /** Called after [loadData] completes. */
    var onDataLoaded: ((BridgeEvent.DataLoaded) -> Unit)? = null

    /** Called when a new bar is appended at the live edge. */
    var onNewBar: ((BridgeEvent.NewBar) -> Unit)? = null

    /** Called when the stream connection status changes. */
    var onStreamStatus: ((BridgeEvent.StreamStatus) -> Unit)? = null

    /** Called when the user submits an order via the floating trade button. */
    var onPlaceOrder: ((BridgeEvent.PlaceOrder) -> Unit)? = null

    /** Called when the user taps × to close or cancel a trade level or remove a bracket. */
    var onTradeLevelClose: ((BridgeEvent.TradeLevelClose) -> Unit)? = null

    /** Called on every pointer move while a level or bracket is being dragged. */
    var onTradeLevelDrag: ((BridgeEvent.TradeLevelDrag) -> Unit)? = null

    /** Called when the user confirms edits to a trade level (main, SL, TP batched together). */
    var onTradeLevelEdit: ((BridgeEvent.TradeLevelEdit) -> Unit)? = null

    /** Called when the chart ✓ button confirms an edit (including draft orders). */
    var onTradeLevelConfirmed: ((BridgeEvent.TradeLevelConfirmed) -> Unit)? = null

    /** Called when the user taps the pencil/edit button to open the order panel for a level. */
    var onTradeLevelEditOpen: ((BridgeEvent.TradeLevelEditOpen) -> Unit)? = null

    /**
     * Called after [addLevelBracket] auto-places a SL/TP bracket.
     * Use [BridgeEvent.TradeLevelBracketActivated.price] to populate your form's SL/TP input field.
     */
    var onTradeLevelBracketActivated: ((BridgeEvent.TradeLevelBracketActivated) -> Unit)? = null

    /** Called when a new draft order is shown on the chart — open the buy/sell form. */
    var onDraftInitiated: ((BridgeEvent.DraftInitiated) -> Unit)? = null

    /** Called when a draft order is cancelled without confirming. */
    var onDraftCancelled: ((BridgeEvent.DraftCancelled) -> Unit)? = null

    /**
     * Called when the chart engine requests data for a time range.
     *
     * Implement this to serve data requests from the chart. Fetch bars for the given
     * [BridgeEvent.DataRequest.timeframe]/[BridgeEvent.DataRequest.interval] and
     * [BridgeEvent.DataRequest.start]/[BridgeEvent.DataRequest.end] timestamps
     * (milliseconds since epoch), then call [resolveDataRequest] to deliver the data.
     */
    var onDataRequest: ((BridgeEvent.DataRequest) -> Unit)? = null

    /**
     * Called when the user taps the symbol name and `onSymbolClick = true` was passed to [init].
     * No ISIN picker modal is shown when this callback is set via the init command.
     */
    var onSymbolClick: ((BridgeEvent.SymbolClick) -> Unit)? = null

    /** Called when the chart engine reports an error. */
    var onError: ((BridgeEvent.Error) -> Unit)? = null

    /**
     * Generic fallback — called for every event including those that have a typed callback.
     * Useful for logging or forwarding all events to a single handler.
     */
    var onBridgeEvent: ((BridgeEvent) -> Unit)? = null

    // ── Event dispatcher ──────────────────────────────────────────────────────

    private fun dispatchEvent(event: BridgeEvent) {
        onBridgeEvent?.invoke(event)
        when (event) {
            is BridgeEvent.Ready -> {
                flushCommandQueue()
                skeletonView.visibility = View.GONE
                if (!hasCalledOnReady) {
                    hasCalledOnReady = true
                    onReady?.invoke()
                }
            }
            is BridgeEvent.Crosshair -> onCrosshair?.invoke(event)
            is BridgeEvent.BarClick -> onBarClick?.invoke(event)
            is BridgeEvent.ViewportChange -> onViewportChange?.invoke(event)
            is BridgeEvent.SeriesChange -> onSeriesChange?.invoke(event)
            is BridgeEvent.TimeframeChange -> onTimeframeChange?.invoke(event)
            is BridgeEvent.DurationChange -> onDurationChange?.invoke(event)
            is BridgeEvent.StateChange -> onStateChange?.invoke(event)
            is BridgeEvent.StateSnapshot -> onStateSnapshot?.invoke(event)
            is BridgeEvent.DataLoaded -> onDataLoaded?.invoke(event)
            is BridgeEvent.NewBar -> onNewBar?.invoke(event)
            is BridgeEvent.StreamStatus -> onStreamStatus?.invoke(event)
            is BridgeEvent.PlaceOrder          -> onPlaceOrder?.invoke(event)
            is BridgeEvent.TradeLevelClose     -> onTradeLevelClose?.invoke(event)
            is BridgeEvent.TradeLevelDrag      -> onTradeLevelDrag?.invoke(event)
            is BridgeEvent.TradeLevelEdit      -> onTradeLevelEdit?.invoke(event)
            is BridgeEvent.TradeLevelConfirmed -> onTradeLevelConfirmed?.invoke(event)
            is BridgeEvent.TradeLevelEditOpen         -> onTradeLevelEditOpen?.invoke(event)
            is BridgeEvent.TradeLevelBracketActivated -> onTradeLevelBracketActivated?.invoke(event)
            is BridgeEvent.DraftInitiated             -> onDraftInitiated?.invoke(event)
            is BridgeEvent.DraftCancelled      -> onDraftCancelled?.invoke(event)
            is BridgeEvent.DataRequest         -> onDataRequest?.invoke(event)
            is BridgeEvent.SymbolClick         -> onSymbolClick?.invoke(event)
            is BridgeEvent.Error               -> onError?.invoke(event)
        }
    }

    // ── Public command API ────────────────────────────────────────────────────

    /**
     * Re-creates the chart engine with the given configuration.
     * Call this from [onReady] to apply settings before loading data.
     */
    fun init(
        theme: String = "dark",
        symbol: String? = null,
        series: String? = null,
        timeframe: String? = null,
        duration: String? = null,
        enableTrading: Boolean = false,
        showVolume: Boolean? = null,
        showUI: Boolean? = null,
        showDrawingTools: Boolean? = null,
        showBidAskLines: Boolean? = null,
        showActLogo: Boolean? = null,
        showCandleCountdown: Boolean? = null,
        candleCountdownTimeframes: List<String>? = null,
        disableCountdownOnMobile: Boolean? = null,
        maxSubPanes: Int? = null,
        mobileBarDivisor: Int? = null,
        targetCandleWidth: Double? = null,
        tickClosePriceSource: String? = null,
        tradesThresholdForHorizontalLine: Int? = null,
        tradeDisplayFilter: String? = null,
        positionRenderStyle: String? = null,
        hideLevelConfirmCancel: Boolean? = null,
        /** Per-timeframe base interval override for client-side aggregation, e.g. `mapOf("1h" to "30m")`. */
        aggregateFrom: Map<String, String>? = null,
        /** Per-theme canvas background color overrides as a raw JSON string. */
        canvasColorsJson: String? = null,
        /** Per-theme deep-partial color overrides for the built-in themes as a raw JSON string. */
        themeOverridesJson: String? = null,
        /** Per-theme deep-partial color overrides (typed). Takes precedence over [themeOverridesJson] when both are set. */
        themeOverrides: ThemeOverrides? = null,
        /** User-visible string overrides for i18n/localisation as a raw JSON string. */
        labelsJson: String? = null,
        /** Per-component UI configuration overrides as a raw JSON string. */
        uiConfigJson: String? = null,
        /** Override the default duration → timeframe pairings, e.g. `mapOf("1Y" to "1D")`. */
        durationTimeframeMap: Map<String, String>? = null,
        /** When true, fires [BridgeEvent.SymbolClick] on symbol tap instead of opening the picker modal. */
        onSymbolClick: Boolean = false,
    ) = sendCommand(BridgeCommand.Init(
        theme = theme, symbol = symbol, series = series, timeframe = timeframe,
        duration = duration, enableTrading = enableTrading,
        showVolume = showVolume, showUI = showUI, showDrawingTools = showDrawingTools,
        showBidAskLines = showBidAskLines, showActLogo = showActLogo,
        showCandleCountdown = showCandleCountdown,
        candleCountdownTimeframes = candleCountdownTimeframes,
        disableCountdownOnMobile = disableCountdownOnMobile,
        maxSubPanes = maxSubPanes, mobileBarDivisor = mobileBarDivisor,
        targetCandleWidth = targetCandleWidth, tickClosePriceSource = tickClosePriceSource,
        tradesThresholdForHorizontalLine = tradesThresholdForHorizontalLine,
        tradeDisplayFilter = tradeDisplayFilter, positionRenderStyle = positionRenderStyle,
        hideLevelConfirmCancel = hideLevelConfirmCancel,
        aggregateFrom = aggregateFrom, canvasColorsJson = canvasColorsJson,
        themeOverridesJson = themeOverridesJson ?: themeOverrides?.toJsonString(), labelsJson = labelsJson,
        uiConfigJson = uiConfigJson, durationTimeframeMap = durationTimeframeMap,
        onSymbolClick = onSymbolClick,
    ))

    /**
     * Loads a full dataset into the chart and optionally fits all bars into view.
     * Emits [BridgeEvent.DataLoaded] on completion.
     */
    fun loadData(bars: List<OHLCVBar>, fitAll: Boolean = false) =
        sendCommand(BridgeCommand.LoadData(bars, fitAll))

    /** Switches between `"dark"` and `"light"` themes. */
    fun setTheme(theme: String) = sendCommand(BridgeCommand.SetTheme(theme))

    /**
     * Changes the chart series type.
     * Valid values: `"candlestick"`, `"hollow_candle"`, `"line"`, `"area"`, `"ohlc"`.
     */
    fun setSeries(series: String) = sendCommand(BridgeCommand.SetSeries(series))

    /** Changes the active timeframe (e.g. `"1m"`, `"1h"`, `"1D"`). */
    fun setTimeframe(timeframe: String) = sendCommand(BridgeCommand.SetTimeframe(timeframe))

    /**
     * Pushes a live tick for streaming updates.
     * The bridge aggregates ticks into the current candle; use [loadData] for bulk replacement.
     */
    fun pushTick(bid: Double, ask: Double, timestamp: Long) =
        sendCommand(BridgeCommand.PushTick(bid, ask, timestamp))

    /**
     * Adds a study by short name (e.g. `"SMA"`, `"EMA"`, `"RSI"`, `"BB"`).
     * @param params Optional parameters, e.g. `mapOf("period" to 20)`.
     */
    fun addIndicator(name: String, params: Map<String, Any>? = null) =
        sendCommand(BridgeCommand.AddIndicator(name, params))

    /** Removes a study by name. */
    fun removeIndicator(name: String) = sendCommand(BridgeCommand.RemoveIndicator(name))

    /**
     * Activates a drawing tool by ID (e.g. `"trend_line"`, `"horizontal_line"`).
     * Pass `null` to deactivate the current drawing tool.
     */
    fun setDrawingTool(tool: String?) = sendCommand(BridgeCommand.SetDrawingTool(tool))

    /** Removes all drawings from the chart. */
    fun clearAllDrawings() = sendCommand(BridgeCommand.ClearAllDrawings)

    /** Updates the displayed symbol name in the chart's top bar. */
    fun setSymbol(symbol: String) = sendCommand(BridgeCommand.SetSymbol(symbol))

    /**
     * Requests the current chart state.
     * The result is delivered asynchronously via [onStateSnapshot].
     */
    fun getState() = sendCommand(BridgeCommand.GetState)

    /**
     * Restores a previously captured chart state.
     * @param stateJson Raw JSON string from a prior [onStateSnapshot] callback.
     */
    fun setState(stateJson: String) = sendCommand(BridgeCommand.SetState(stateJson))

    /**
     * Resolves a pending data request from the chart engine with fetched bars.
     *
     * Call this from [onDataRequest] after fetching the required data.
     * @param requestId The [BridgeEvent.DataRequest.requestId] received in the event.
     * @param bars The OHLCV bars covering the requested time range.
     */
    fun resolveDataRequest(requestId: String, bars: List<OHLCVBar>) =
        sendCommand(BridgeCommand.ResolveDataRequest(requestId, bars))

    /**
     * Enables or disables verbose tick/render logging in the browser console.
     *
     * Useful for diagnosing live candle or streaming issues during development.
     */
    fun setDebug(enabled: Boolean) = sendCommand(BridgeCommand.SetDebug(enabled))

    /** Destroys the chart engine and releases WebView resources. */
    fun destroy() {
        sendCommand(BridgeCommand.Destroy)
        mainHandler.postDelayed({
            webView.stopLoading()
            webView.destroy()
        }, 200)
    }

    // ── Trade levels ──────────────────────────────────────────────────────────

    /**
     * Replaces all levels of the given [type] with [levels].
     * Each map in [levels] must contain at least the [labelKey] and [priceKey] fields.
     * Optional per-entry fields: `side`, `stopLossPrice`, `takeProfitPrice`,
     * `pnl`, `pnlText`, `text`, `lots`, `orderType`, `entryPriceEditable`.
     * @param type `"position"`, `"pending"`, or `"trade"`.
     */
    fun setLevels(
        levels: List<Map<String, Any>>,
        labelKey: String,
        priceKey: String,
        type: String,
        pnlKey: String? = null,
        pnlTextKey: String? = null,
    ) = sendCommand(BridgeCommand.SetLevels(levels, labelKey, priceKey, type, pnlKey, pnlTextKey))

    /** Removes a single level by its label. No-op if not found. */
    fun removeLevelByLabel(label: String) =
        sendCommand(BridgeCommand.RemoveLevelByLabel(label))

    /** Updates the entry price of an existing level. */
    fun updateLevelMainPrice(label: String, price: Double) =
        sendCommand(BridgeCommand.UpdateLevelMainPrice(label, price))

    /**
     * Updates or removes a SL/TP bracket on an existing level.
     * @param bracketType `"sl"` or `"tp"`.
     * @param price Pass `null` to remove the bracket.
     */
    fun updateLevelBracket(label: String, bracketType: String, price: Double?) =
        sendCommand(BridgeCommand.UpdateLevelBracket(label, bracketType, price))

    /**
     * Adds a SL or TP bracket to an existing level at an auto-computed default price.
     * The chart places the bracket and fires [onTradeLevelBracketActivated] with the chosen price
     * so your order form can populate the SL/TP input field.
     * @param bracketType `"sl"` or `"tp"`.
     */
    fun addLevelBracket(label: String, bracketType: String) =
        sendCommand(BridgeCommand.AddLevelBracket(label, bracketType))

    /**
     * Unified bracket placement — works for both existing levels and the active draft order.
     * Pass [label] (OrderID/TradeID) for an existing level; omit it for the active draft order.
     * Fires [onTradeLevelBracketActivated] with the auto-computed price.
     * The event's label is null when the bracket was placed on a draft order.
     * @param bracketType `"sl"` or `"tp"`.
     */
    fun addBracket(bracketType: String, label: String? = null) =
        sendCommand(BridgeCommand.AddBracket(bracketType, label))

    /**
     * Unified bracket removal — works for both existing levels and the active draft order.
     * Pass [label] (OrderID/TradeID) for an existing level; omit it for the active draft order.
     * @param bracketType `"sl"` or `"tp"`.
     */
    fun removeBracket(bracketType: String, label: String? = null) =
        sendCommand(BridgeCommand.RemoveBracket(bracketType, label))

    /** Cancels an in-progress level edit, reverting to the last confirmed price. */
    fun cancelLevelEdit(label: String) = sendCommand(BridgeCommand.CancelLevelEdit(label))

    /** Programmatically selects (highlights) a level, or deselects all when [label] is null. */
    fun selectLevel(label: String?) = sendCommand(BridgeCommand.SelectLevel(label))

    // ── Draft orders ──────────────────────────────────────────────────────────

    /**
     * Shows a draggable limit or stop draft order line on the chart.
     * While the user drags it, [onTradeLevelDrag] events fire; confirming emits [onTradeLevelConfirmed].
     * @param side `"buy"` or `"sell"`.
     * @param orderType `"limit"` or `"stop"`.
     */
    fun showDraftOrder(price: Double, side: String, orderType: String) =
        sendCommand(BridgeCommand.ShowDraftOrder(price, side, orderType))

    /**
     * Shows a non-draggable market-order preview line.
     * SL/TP brackets can still be attached via [updateDraftOrderBracket].
     * @param side `"buy"` or `"sell"`.
     */
    fun showMarketDraft(price: Double, side: String) =
        sendCommand(BridgeCommand.ShowMarketDraft(price, side))

    /** Removes any active draft order from the chart. */
    fun clearDraftOrder() = sendCommand(BridgeCommand.ClearDraftOrder)

    /** Cancels whatever is currently being edited or drafted on the chart (draft order or level edit). No-op when nothing is active. */
    fun cancelCurrentEdit() = sendCommand(BridgeCommand.CancelCurrentEdit)

    /** Updates the lot quantity shown on the active draft order chip. */
    fun setDraftOrderLots(lots: Double) = sendCommand(BridgeCommand.SetDraftOrderLots(lots))

    /** Moves the draft order price line to a new price. */
    fun updateDraftOrderPrice(price: Double) = sendCommand(BridgeCommand.UpdateDraftOrderPrice(price))

    /**
     * Updates or removes a SL/TP bracket on the active draft order.
     * @param bracketType `"sl"` or `"tp"`.
     * @param price Pass `null` to remove the bracket.
     */
    fun updateDraftOrderBracket(bracketType: String, price: Double?) =
        sendCommand(BridgeCommand.UpdateDraftOrderBracket(bracketType, price))

    /**
     * Sets or clears the estimated PNL text shown on a draft order's SL or TP bracket line.
     * Call this after [updateDraftOrderBracket] to display your consumer-calculated P&L estimate.
     * @param bracketType `"sl"` or `"tp"`.
     * @param pnlText Pre-formatted string (e.g. `"-$12.50"`). Pass `null` to clear.
     */
    fun setDraftBracketPnl(bracketType: String, pnlText: String?) =
        sendCommand(BridgeCommand.SetDraftBracketPnl(bracketType, pnlText))

    // ── UI controls ───────────────────────────────────────────────────────────

    /** Shows or hides the volume sub-pane. */
    fun setVolume(show: Boolean) = sendCommand(BridgeCommand.SetVolume(show))

    /** Updates the symbol list used by the ISIN picker modal after initial setup. */
    fun setIsins(isins: List<String>) = sendCommand(BridgeCommand.SetIsins(isins))

    /** Resets both price and time axes to their default auto-fit state. */
    fun resetView() = sendCommand(BridgeCommand.ResetView)

    /** Shows or hides the loading overlay. */
    fun setLoading(loading: Boolean) = sendCommand(BridgeCommand.SetLoading(loading))

    /**
     * Updates per-theme deep-partial color overrides and rebuilds the active theme.
     * @param overridesJson Raw JSON string, e.g. `{"dark":{"background":"#111"},"light":{"background":"#fff"}}`.
     */
    fun setThemeOverrides(overridesJson: String) = sendCommand(BridgeCommand.SetThemeOverrides(overridesJson))

    /** Updates per-theme deep-partial color overrides using typed [ThemeOverrides]. */
    fun setThemeOverrides(overrides: ThemeOverrides) = sendCommand(BridgeCommand.SetThemeOverrides(overrides.toJsonString()))

    /**
     * Replaces a specific bar with authoritative OHLCV data (e.g. a correction from the server).
     * @param barTime Unix millisecond timestamp of the bar to replace.
     */
    fun correctBar(barTime: Long, bar: OHLCVBar) = sendCommand(BridgeCommand.CorrectBar(barTime, bar))

    // ── Internal ──────────────────────────────────────────────────────────────

    /**
     * Serialises [cmd] and either queues it (if the chart is not yet ready) or
     * evaluates it in the WebView immediately. Safe to call from any thread.
     */
    private fun sendCommand(cmd: BridgeCommand) {
        val json = cmd.toJson()
        synchronized(this) {
            if (!isReady) {
                commandQueue.add(json)
                return
            }
        }
        evalOnMainThread(json)
    }

    private fun evalOnMainThread(json: String) {
        val escaped = json.replace("\\", "\\\\").replace("'", "\\'")
        webView.post { webView.evaluateJavascript("window.ChartBridge.send('$escaped');", null) }
    }

    /**
     * Flushes all queued commands as a single [WebView.evaluateJavascript] call.
     * Must be called on the main thread (i.e. from [dispatchEvent] on [BridgeEvent.Ready]).
     */
    private fun flushCommandQueue() {
        val batch = synchronized(this) {
            isReady = true
            commandQueue.toList().also { commandQueue.clear() }
        }
        if (batch.isEmpty()) return
        val js = batch.joinToString(";") { json ->
            "window.ChartBridge.send('${json.replace("\\", "\\\\").replace("'", "\\'")}')"
        }
        webView.evaluateJavascript("$js;", null)
    }
}
