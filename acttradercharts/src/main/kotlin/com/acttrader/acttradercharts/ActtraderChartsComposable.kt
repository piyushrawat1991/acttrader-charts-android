package com.acttrader.acttradercharts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Composable wrapper around [ActtraderChartsView].
 *
 * ## Basic usage
 * ```kotlin
 * var chart: ActtraderChartsView? = null
 *
 * ActtraderChart(
 *     modifier = Modifier.fillMaxSize(),
 *     onReady = { chart?.init(); chart?.loadData(bars) },
 *     ref     = { chart = it }
 * )
 * ```
 *
 * Use the [ref] callback to obtain a reference to the underlying [ActtraderChartsView] for
 * imperative commands (e.g. [ActtraderChartsView.loadData], [ActtraderChartsView.setTheme]).
 * The view's [ActtraderChartsView.destroy] is called automatically when the composable leaves
 * the composition.
 *
 * @param modifier         Compose layout modifier applied to the chart surface.
 * @param onReady          Invoked once when the chart engine is ready to receive commands.
 * @param onCrosshair      Invoked whenever the crosshair moves over a bar.
 * @param onBarClick       Invoked when the user taps a bar.
 * @param onViewportChange Invoked when the visible viewport changes (pan / zoom).
 * @param onSeriesChange   Invoked when the active series type changes.
 * @param onTimeframeChange Invoked when the active timeframe changes.
 * @param onDurationChange Invoked when the active duration changes.
 * @param onStateChange    Invoked on any chart state mutation.
 * @param onStateSnapshot  Invoked in response to [ActtraderChartsView.getState].
 * @param onDataLoaded     Invoked after [ActtraderChartsView.loadData] completes.
 * @param onNewBar         Invoked when a new bar is appended at the live edge.
 * @param onStreamStatus   Invoked when the stream connection status changes.
 * @param onPlaceOrder     Invoked when the user submits an order via the trade button.
 * @param onError          Invoked when the chart engine reports an error.
 * @param onBridgeEvent    Generic fallback called for every event.
 * @param ref              Called once with the [ActtraderChartsView] instance after creation.
 */
@Composable
fun ActtraderChart(
    modifier: Modifier = Modifier,
    onReady: (() -> Unit)? = null,
    onCrosshair: ((BridgeEvent.Crosshair) -> Unit)? = null,
    onBarClick: ((BridgeEvent.BarClick) -> Unit)? = null,
    onViewportChange: ((BridgeEvent.ViewportChange) -> Unit)? = null,
    onSeriesChange: ((BridgeEvent.SeriesChange) -> Unit)? = null,
    onTimeframeChange: ((BridgeEvent.TimeframeChange) -> Unit)? = null,
    onDurationChange: ((BridgeEvent.DurationChange) -> Unit)? = null,
    onStateChange: ((BridgeEvent.StateChange) -> Unit)? = null,
    onStateSnapshot: ((BridgeEvent.StateSnapshot) -> Unit)? = null,
    onDataLoaded: ((BridgeEvent.DataLoaded) -> Unit)? = null,
    onNewBar: ((BridgeEvent.NewBar) -> Unit)? = null,
    onStreamStatus: ((BridgeEvent.StreamStatus) -> Unit)? = null,
    onPlaceOrder: ((BridgeEvent.PlaceOrder) -> Unit)? = null,
    onError: ((BridgeEvent.Error) -> Unit)? = null,
    onBridgeEvent: ((BridgeEvent) -> Unit)? = null,
    ref: ((ActtraderChartsView) -> Unit)? = null,
) {
    // Hold the view instance across recompositions so callbacks can be updated in-place.
    val viewRef = remember { ActtraderChartsViewHolder() }

    DisposableEffect(Unit) {
        onDispose { viewRef.view?.destroy() }
    }

    AndroidView(
        factory = { context ->
            ActtraderChartsView(context).also { view ->
                viewRef.view = view
                view.onReady          = onReady
                view.onCrosshair      = onCrosshair
                view.onBarClick       = onBarClick
                view.onViewportChange = onViewportChange
                view.onSeriesChange   = onSeriesChange
                view.onTimeframeChange = onTimeframeChange
                view.onDurationChange = onDurationChange
                view.onStateChange    = onStateChange
                view.onStateSnapshot  = onStateSnapshot
                view.onDataLoaded     = onDataLoaded
                view.onNewBar         = onNewBar
                view.onStreamStatus   = onStreamStatus
                view.onPlaceOrder     = onPlaceOrder
                view.onError          = onError
                view.onBridgeEvent    = onBridgeEvent
                ref?.invoke(view)
            }
        },
        // Keep callbacks current if the caller's lambdas change between recompositions.
        update = { view ->
            view.onReady          = onReady
            view.onCrosshair      = onCrosshair
            view.onBarClick       = onBarClick
            view.onViewportChange = onViewportChange
            view.onSeriesChange   = onSeriesChange
            view.onTimeframeChange = onTimeframeChange
            view.onDurationChange = onDurationChange
            view.onStateChange    = onStateChange
            view.onStateSnapshot  = onStateSnapshot
            view.onDataLoaded     = onDataLoaded
            view.onNewBar         = onNewBar
            view.onStreamStatus   = onStreamStatus
            view.onPlaceOrder     = onPlaceOrder
            view.onError          = onError
            view.onBridgeEvent    = onBridgeEvent
        },
        modifier = modifier,
    )
}

/** Simple holder so DisposableEffect can reach the view without a direct capture. */
private class ActtraderChartsViewHolder {
    var view: ActtraderChartsView? = null
}
