package com.acttrader.stockchart

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
 * val chart = StockChartView(context)
 * chart.onReady = { chart.loadData(bars) }
 * chart.onError = { err -> Log.e("Chart", err.message) }
 * parentLayout.addView(chart)
 * ```
 *
 * All public command methods are safe to call from any thread.
 */
class StockChartView @JvmOverloads constructor(
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
            is BridgeEvent.PlaceOrder -> onPlaceOrder?.invoke(event)
            is BridgeEvent.Error -> onError?.invoke(event)
        }
    }

    // ── Public command API ────────────────────────────────────────────────────

    /**
     * Re-creates the chart engine with the given configuration.
     * Call this from [onReady] to apply settings (e.g. [enableTrading]) before loading data.
     */
    fun init(
        theme: String = "dark",
        symbol: String? = null,
        series: String? = null,
        enableTrading: Boolean = false,
        minLots: Int = 1,
        showCandleCountdown: Boolean? = null,
        disableCountdownOnMobile: Boolean? = null,
    ) = sendCommand(BridgeCommand.Init(theme, symbol, series, enableTrading, minLots, showCandleCountdown, disableCountdownOnMobile))

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

    /** Destroys the chart engine and releases WebView resources. */
    fun destroy() {
        sendCommand(BridgeCommand.Destroy)
        mainHandler.postDelayed({
            webView.stopLoading()
            webView.destroy()
        }, 200)
    }

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
