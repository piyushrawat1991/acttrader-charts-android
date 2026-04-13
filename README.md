# acttrader-charts-android

Android Kotlin library that renders [`acttrader-charts`](https://github.com/piyushrawat1991/acttrader-charts) inside a `WebView`.

## Requirements

- minSdk 24 (Android 7.0)
- Kotlin 1.9+

## Installation

Add GitHub Packages to your project's `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/piyushrawat1991/acttrader-charts-android")
        }
    }
}
```

Then add the dependency:

```kotlin
dependencies {
    implementation("com.acttrader:acttrader-charts-android:0.1.0")
}
```

## OHLCVBar

```kotlin
data class OHLCVBar(
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double,
    val time: Long,   // Unix timestamp in milliseconds (UTC)
)
```

## Basic usage

```kotlin
// In your Activity or Fragment
val chart = ActtraderChartsView(context)

chart.onReady = {
    chart.init(theme = "dark", symbol = "AAPL", enableTrading = false)
    chart.loadData(bars)           // List<OHLCVBar>
}

chart.onCrosshair = { event ->
    // event.open, .high, .low, .close, .volume, .time
}

chart.onError = { err ->
    Log.e("Chart", err.message)
}

parentLayout.addView(chart)
```

### XML layout

```xml
<com.acttrader.acttradercharts.ActtraderChartsView
    android:id="@+id/chart"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

## Commands

| Method | Description |
|--------|-------------|
| `init(...)` | Initialise the chart engine — call from `onReady` before `loadData` (see params below) |
| `loadData(bars, fitAll)` | Replace full dataset |
| `pushTick(bid, ask, timestamp)` | Stream a live tick |
| `setTheme("dark" \| "light")` | Switch theme |
| `setSeries(type)` | Change chart type (`"candlestick"`, `"hollow_candle"`, `"line"`, `"area"`, `"ohlc"`) |
| `setSymbol(symbol)` | Update displayed symbol name |
| `addIndicator(name, params?)` | Add study (e.g. `"SMA"`, `"EMA"`, `"RSI"`, `"BB"`, `"MACD"`) |
| `removeIndicator(name)` | Remove study by name |
| `setDrawingTool(tool?)` | Activate drawing tool (e.g. `"trend_line"`, `"horizontal_line"`), `null` to deactivate |
| `clearAllDrawings()` | Remove all drawings |
| `getState()` | Request state snapshot — result delivered via `onStateSnapshot` |
| `setState(stateJson)` | Restore a prior state from `onStateSnapshot` JSON |
| `resolveDataRequest(requestId, bars)` | Resolves a pending `onDataRequest` with fetched bars |
| `setDebug(enabled)` | Enable or disable verbose logging in the browser console |
| `destroy()` | Release WebView resources |
| **TFC — Trade Levels** | |
| `setLevels(levels, labelKey, priceKey, type, pnlKey?, pnlTextKey?)` | Replace all levels of a given type; pass empty list to clear |
| `removeLevelByLabel(label)` | Remove a single level by label |
| `updateLevelMainPrice(label, price)` | Update the entry price of an existing level |
| `updateLevelBracket(label, bracketType, price?)` | Update or remove a SL/TP bracket; pass `null` price to remove |
| `cancelLevelEdit(label)` | Cancel an in-progress level edit, reverting to last confirmed price |
| `selectLevel(label?)` | Programmatically highlight a level; pass `null` to deselect all |
| **TFC — Draft Orders** | |
| `showDraftOrder(price, side, orderType)` | Show a draggable limit or stop draft order line |
| `showMarketDraft(price, side)` | Show a non-draggable market-order preview line |
| `clearDraftOrder()` | Remove the active draft order |
| `setDraftOrderLots(lots)` | Update the lot quantity on the active draft order chip |
| `updateDraftOrderPrice(price)` | Move the draft order price line to a new price |
| `updateDraftOrderBracket(bracketType, price?)` | Update or remove a SL/TP bracket on the draft order; pass `null` to remove |
| **UI / Utility** | |
| `setVolume(show)` | Show or hide the volume sub-pane |
| `setIsins(isins)` | Update the symbol list used by the ISIN picker |
| `setMinLots(lots)` | Update the minimum lot size in the trade popover |
| `resetView()` | Reset price and time axes to auto-fit |
| `setLoading(loading)` | Show or hide the loading overlay |
| `correctBar(barTime, bar)` | Replace a specific bar with authoritative OHLCV data (e.g. server correction) |

### `init()` parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `theme` | `String` | `"dark"` | `"dark"` or `"light"` |
| `symbol` | `String?` | `null` | Symbol name shown in the top bar (e.g. `"AAPL"`) |
| `series` | `String?` | `null` | Initial chart type (e.g. `"candlestick"`, `"line"`, `"area"`, `"ohlc"`, `"hollow_candle"`) |
| `timeframe` | `String?` | `null` | Initial timeframe (e.g. `"1m"`, `"5m"`, `"1h"`, `"1D"`) |
| `duration` | `String?` | `null` | Initial duration button (e.g. `"1D"`, `"1M"`, `"1Y"`, `"All"`) |
| `showVolume` | `Boolean?` | `null` | Show volume bars |
| `showUI` | `Boolean?` | `null` | Show top / bottom bars |
| `showDrawingTools` | `Boolean?` | `null` | Show drawing toolbar and pencil button |
| `showBidAskLines` | `Boolean?` | `null` | Show bid and ask as dashed lines during a live stream |
| `showActLogo` | `Boolean?` | `null` | Show ACT watermark logo |
| `showCandleCountdown` | `Boolean?` | `null` | Show countdown timer on the live candle |
| `candleCountdownTimeframes` | `List<String>?` / `"all"` | `null` | Timeframes where the countdown appears |
| `disableCountdownOnMobile` | `Boolean?` | `null` | Hide the countdown on small screens |
| `enableTrading` | `Boolean` | `false` | Show floating buy/sell order button |
| `minLots` | `Int` | `1` | Minimum lot size for order entry (requires `enableTrading = true`) |
| `maxSubPanes` | `Int?` | `null` | Max simultaneous oscillator sub-panes |
| `mobileBarDivisor` | `Int?` | `null` | Divide desktop bar count on touch devices (`2`, `3`, or `4`) |
| `targetCandleWidth` | `Double?` | `null` | Target px width per candle for auto-calculating initial bar count |
| `tickClosePriceSource` | `String?` | `null` | `"bid"` or `"ask"` for live tick close/high/low |
| `tradesThresholdForHorizontalLine` | `Int?` | `null` | Level count above which render auto-switches to dot mode |
| `tradeDisplayFilter` | `String?` | `null` | Which TFC levels are visible: `"all"` · `"positions"` · `"orders"` · `"none"` |
| `positionRenderStyle` | `String?` | `null` | Force position render style: `"line"` or `"dot"` |
| `hideLevelConfirmCancel` | `Boolean?` | `null` | Hide on-canvas ✓/✗ confirm/cancel buttons for TFC level edits |
| `hideQtyButton` | `Boolean?` | `null` | Hide the floating Qty input overlay on draft orders |

## Events

| Callback | Type | Description |
|----------|------|-------------|
| `onReady` | `() -> Unit` | Engine ready to receive commands |
| `onCrosshair` | `BridgeEvent.Crosshair` | Crosshair moved — `.open`, `.high`, `.low`, `.close`, `.volume`, `.time`, `.x`, `.y` |
| `onBarClick` | `BridgeEvent.BarClick` | Bar tapped — `.open`, `.high`, `.low`, `.close`, `.volume`, `.time` |
| `onViewportChange` | `BridgeEvent.ViewportChange` | Pan / zoom — `.startIndex`, `.endIndex`, `.barWidth` |
| `onSeriesChange` | `BridgeEvent.SeriesChange` | Series type changed — `.series` |
| `onTimeframeChange` | `BridgeEvent.TimeframeChange` | Timeframe changed — `.timeframe` |
| `onDurationChange` | `BridgeEvent.DurationChange` | Duration selector changed — `.duration` |
| `onStateChange` | `BridgeEvent.StateChange` | Any chart state mutation — `.stateJson` |
| `onDataLoaded` | `BridgeEvent.DataLoaded` | `loadData` complete — `.barCount` |
| `onNewBar` | `BridgeEvent.NewBar` | New bar appended at live edge — `.open`, `.high`, `.low`, `.close`, `.volume`, `.time` |
| `onStreamStatus` | `BridgeEvent.StreamStatus` | Stream connection status changed — `.status` |
| `onPlaceOrder` | `BridgeEvent.PlaceOrder` | User submitted order (requires `enableTrading`) — `.price`, `.side`, `.orderType` |
| `onTradeLevelEdit` | `BridgeEvent.TradeLevelEdit` | User confirmed a TFC edit — `.label`, `.type`, `.data`, `.changes[]`, `.isFullscreen` |
| `onTradeLevelClose` | `BridgeEvent.TradeLevelClose` | User tapped × on a level — `.label`, `.type`, `.action`, `.data`, `.isFullscreen` |
| `onTradeLevelDrag` | `BridgeEvent.TradeLevelDrag` | Live price during drag, fires on every move — `.label`, `.newPrice`, `.bracketType?`, `.data`, `.isFullscreen` |
| `onTradeLevelEditOpen` | `BridgeEvent.TradeLevelEditOpen` | User tapped the pencil edit button — `.label`, `.type`, `.price`, `.side?`, `.stopLossPrice?`, `.takeProfitPrice?`, `.data`, `.isFullscreen` |
| `onTradeLevelConfirmed` | `BridgeEvent.TradeLevelConfirmed` | Chart ✓ button confirmed an edit — `.label`, `.type`, `.isFullscreen` |
| `onDraftInitiated` | `BridgeEvent.DraftInitiated` | New draft order shown — `.side`, `.price`, `.orderType`, `.isFullscreen` |
| `onDraftCancelled` | `BridgeEvent.DraftCancelled` | Draft order cancelled — `.label`, `.isFullscreen` |
| `onDataRequest` | `BridgeEvent.DataRequest` | Chart requests data for a time range — `.requestId`, `.from`, `.to`, `.timeframe`; call `resolveDataRequest` to respond |
| `onSymbolClick` | `BridgeEvent.SymbolClick` | User tapped the symbol name (requires `onSymbolClick = true` in `init`) |
| `onStateSnapshot` | `BridgeEvent.StateSnapshot` | Response to `getState()` — `.stateJson` |
| `onError` | `BridgeEvent.Error` | Engine error — `.message`, `.code` |
| `onBridgeEvent` | `BridgeEvent` | Generic — fires for every event including those with typed callbacks |

> **`isFullscreen`** is `true` when the chart is in fullscreen mode at the time of the TFC action. Use it to gate toast notifications so they only appear while the chart is covering the full screen.

## License

MIT
