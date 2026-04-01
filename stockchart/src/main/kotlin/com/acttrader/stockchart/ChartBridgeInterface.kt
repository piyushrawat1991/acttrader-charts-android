package com.acttrader.stockchart

import android.os.Handler
import android.webkit.JavascriptInterface

/**
 * Receives JSON event strings posted by `window.ChartAndroidBridge.postMessage(json)` from the
 * chart WebView and forwards parsed [BridgeEvent] instances to [onBridgeEvent] on the main thread.
 *
 * Registered on the WebView via:
 * ```kotlin
 * webView.addJavascriptInterface(bridgeInterface, "ChartAndroidBridge")
 * ```
 */
internal class ChartBridgeInterface(
    private val mainHandler: Handler,
    var onBridgeEvent: ((BridgeEvent) -> Unit)?,
) {
    @JavascriptInterface
    fun postMessage(json: String) {
        val event = BridgeEventParser.parse(json) ?: return
        mainHandler.post { onBridgeEvent?.invoke(event) }
    }
}
