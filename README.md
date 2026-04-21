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

> **Note:** `init(...)` is how you apply a non-default configuration (theme, timeframe,
> symbol, `themeOverrides`, etc.). If you skip it, the chart auto-initialises with the
> built-in defaults (dark theme, `1D` timeframe) the moment the WebView is ready, so
> runtime methods like `setTheme` and `loadData` always have an engine to talk to.

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
| `setTimeframe(timeframe)` | `"1m"` `"5m"` `"15m"` `"30m"` `"1h"` `"4h"` `"1D"` `"1W"` `"1M"` `"1Y"` |
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
| `updateLevelMainPrice(label, price)` | Update the entry price of an existing level. Stages the edit in the chart's pending-edit buffer so it survives subsequent `setLevels` refreshes (e.g. per-tick PnL updates) until the server echoes the new price or `cancelLevelEdit` / `cancelCurrentEdit` is called. Call `cancelLevelEdit(label)` when your modify panel closes without submitting, otherwise the staged edit keeps overriding server state on the chart |
| `updateLevelBracket(label, bracketType, price?)` | Update or remove a SL/TP bracket on an existing level; pass `null` price to remove. Same staging semantics as `updateLevelMainPrice` |
| `addLevelBracket(label, bracketType)` | Auto-place a SL or TP bracket at a default price offset; fires `onTradeLevelBracketActivated` with the computed price |
| `addBracket(bracketType, label?)` | Unified auto-price bracket placement — pass `label` for an existing order/position, omit it for the active draft order; fires `onTradeLevelBracketActivated` (`label` is `""` for drafts — check `label.isEmpty()`) |
| `removeBracket(bracketType, label?)` | Unified bracket removal — pass `label` for an existing order/position, omit it for the active draft order |
| `cancelLevelEdit(label)` | Cancel an in-progress level edit, reverting to last confirmed price |
| `selectLevel(label?)` | Programmatically highlight a level; pass `null` to deselect all |
| | **Off-viewport indicators:** When a level's entry/SL/TP is outside the visible price range, a `▲ N` / `▼ N` pill appears near the chart's right edge. Tapping the pill smooth-scrolls the nearest off-screen marker to center. This is automatic — no configuration needed. |
| **TFC — Draft Orders** | |
| `showDraftOrder(price, side, orderType)` | Show a draggable limit or stop draft order line |
| `showMarketDraft(price, side)` | Show a non-draggable market-order preview line |
| `clearDraftOrder()` | Remove the active draft order |
| `cancelCurrentEdit()` | Cancel whatever is currently being edited or drafted (draft order or level edit); no-op when nothing is active |
| `setDraftOrderLots(lots)` | Update the lot quantity on the active draft order chip |
| `updateDraftOrderPrice(price)` | Move the draft order price line to a new price |
| `updateDraftOrderBracket(bracketType, price?)` | Update or remove a SL/TP bracket on the draft order; pass `null` to remove |
| `setDraftBracketPnl(bracketType, pnlText)` | Display estimated P&L text next to the active bracket host's SL or TP line — a draft order while drafting, or the currently selected existing pending order / position while modifying; pass `null` to clear |
| **UI / Utility** | |
| `setTfcActive(enabled)` | Toggle TFC on/off at runtime (only when `tfcEnabled = true` at init) |
| `setVolume(show)` | Show or hide the volume sub-pane |
| `setIsins(isins)` | Update the symbol list used by the ISIN picker |
| `setMinLots(lots)` | Update the minimum lot size in the trade popover |
| `resetView()` | Reset price and time axes to auto-fit. The built-in bottom-center reset button invokes this — it is hidden while the chart is at its default view and fades in only after the user pans, zooms, or price-scales |
| `resetData()` | Clear all bars, the live price line, any in-flight fetch, **all user drawings, and all trade/position levels** (including pending draft orders). Call before switching to a new symbol to prevent previous symbol state from bleeding in (see example below). For a same-symbol data refresh that should preserve drawings, call `loadData(emptyList())` directly instead |
| `setLoading(loading)` | Show or hide the loading overlay |
| `setTimezone(timezone)` | Change display timezone at runtime — IANA string (`"America/New_York"`) or `"local"` |
| `setThemeOverrides(overrides)` | Update per-theme color overrides at runtime — accepts typed `ThemeOverrides` or raw JSON string |
| `correctBar(barTime, bar)` | Replace a specific bar with authoritative OHLCV data (e.g. server correction) |

#### Symbol switch pattern

Always call `resetData()` before loading bars for a new symbol. This prevents
the previous symbol's candles, live price line, drawings, and trade levels
from bleeding into the new chart during the data-fetch window.

```kotlin
chart.setSymbol("GBPUSD")
chart.resetData()
// … fetch new bars for GBPUSD …
chart.loadData(bars)
```

### `init()` parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `theme` | `String` | `"dark"` | `"dark"` or `"light"` |
| `symbol` | `String?` | `null` | Symbol name shown in the top bar (e.g. `"AAPL"`) |
| `series` | `String?` | `null` | Initial chart type (e.g. `"candlestick"`, `"line"`, `"area"`, `"ohlc"`, `"hollow_candle"`) |
| `timeframe` | `String?` | `null` | Initial timeframe (e.g. `"1m"`, `"5m"`, `"1h"`, `"1D"`) |
| `duration` | `String?` | `null` | Initial duration button (e.g. `"1D"`, `"1M"`, `"1Y"`, `"All"`) |
| `showVolume` | `Boolean?` | `null` | Show volume bars |
| `showUI` | `Boolean?` | `null` | Show top / bottom bars. When `false`, the loading overlay is also suppressed |
| `showDrawingTools` | `Boolean?` | `null` | Show drawing toolbar and pencil button |
| `showBidAskLines` | `Boolean?` | `null` | Show bid and ask as dashed lines during a live stream |
| `showActLogo` | `Boolean?` | `null` | Show ACT watermark logo |
| `showCandleCountdown` | `Boolean?` | `null` | Show countdown timer on the live candle |
| `candleCountdownTimeframes` | `List<String>?` / `"all"` | `null` | Timeframes where the countdown appears |
| `disableCountdownOnMobile` | `Boolean?` | `null` | Hide the countdown on small screens |
| `enableTrading` | `Boolean` | `false` | Show floating buy/sell order button |
| `minLots` | `Int` | `1` | Minimum lot size for order entry (requires `enableTrading = true`) |
| `maxSubPanes` | `Int?` | `null` | Max simultaneous oscillator sub-panes |
| `prefetchThreshold` | `Int?` | `null` | Bars from start of data at which historical fetch triggers (min 20, default 80) |
| `mobileBarDivisor` | `Int?` | `null` | Divide desktop bar count on touch devices (`2`, `3`, or `4`) |
| `momentumScrollEnabled` | `Boolean?` | `null` | Enable momentum (kinetic) scrolling — chart coasts after a fast flick. Default: `true` |
| `momentumDecay` | `Double?` | `null` | Per-frame velocity decay, normalised to 60 fps. Clamped `[0.80, 0.99]`. Default: `0.95` |
| `momentumThreshold` | `Double?` | `null` | Min release velocity (px/ms) to launch momentum. Default: `0.3` |
| `momentumMaxVelocity` | `Double?` | `null` | Max launch velocity (px/ms). Default: `6.0` |
| `targetCandleWidth` | `Double?` | `null` | Target px width per candle for auto-calculating initial bar count |
| `tickClosePriceSource` | `String?` | `null` | `"bid"` or `"ask"` for live tick close/high/low |
| `tradesThresholdForHorizontalLine` | `Int?` | `null` | Level count above which render auto-switches to dot mode |
| `tradeDisplayFilter` | `String?` | `null` | Which TFC levels are visible: `"all"` · `"positions"` · `"orders"` · `"none"` |
| `positionRenderStyle` | `String?` | `null` | Force position render style: `"line"` or `"dot"` |
| `hideLevelConfirmCancel` | `Boolean?` | `null` | Hide on-canvas ✓/✗ confirm/cancel buttons for TFC level edits |
| `levelClusteringEnabled` | `Boolean?` | `true` | Enable trade-level fan-out clustering; overlapping levels group into expandable badges |
| `clusterThresholdDistance` | `Int?` | `20` | Pixel proximity threshold for clustering (only when `levelClusteringEnabled` is `true`) |
| `tfcEnabled` | `Boolean?` | `true` | Enable TFC toggle button in the top bar; when `false`, TFC is completely disabled and the toggle button is hidden |
| `hideQtyButton` | `Boolean?` | `null` | Hide the floating Qty input overlay on draft orders |
| `showQuantityField` | `Boolean?` | `null` (`false`) | Render an editable QTY pill at the left of the draft order info box. Tapping opens a flyout input to edit the quantity before submitting |
| `quantityFieldMinLots` | `Double?` | `null` (`1.0`) | Minimum lot size, step size, and initial quantity for the QTY flyout (only used when `showQuantityField = true`) |
| `quantityFieldMaxLots` | `Double?` | `null` (`100.0`) | Maximum lot size for the QTY flyout (only used when `showQuantityField = true`) |
| `showSettings` | `Boolean?` | `null` | Show the settings gear button in the top bar; set to `false` to hide it entirely |
| `showFullscreenButton` | `Boolean` | `false` | Show the fullscreen toggle button in the top bar. Hidden by default on mobile; set to `true` to surface it |
| `hideSymbolAndTick` | `Boolean?` | `null` | Hide the symbol name, OHLC strip, and tick-activity dot overlay |
| `showBottomBar` | `Boolean?` | `null` | Show the bottom duration-selector bar (hidden by default) |
| `timezone` | `String?` | `null` (`"UTC"`) | IANA timezone string for time-axis and crosshair labels. `"UTC"` (default), `"local"` (device timezone), or any IANA string (`"America/New_York"`, `"Europe/London"`, etc.) |
| `uiConfigJson` | `String?` | `null` | Per-component UI configuration overrides (font sizes, icon sizes, spacing) as a raw JSON string. See *Mobile icon sizing* below. |
| `themeOverrides` | `ThemeOverrides?` | `null` | Typed per-theme color overrides. See *Theme overrides* below. |
| `stateJson` | `String?` | `null` | Raw JSON from a prior `onStateSnapshot` to restore atomically at init (timeframe, series, indicators, drawings). See *Restoring state without a flash* below. |

### Restoring state without a flash

When you need to restore a previously saved chart state (e.g. the user re-opens the chart screen), pass the snapshot JSON as `stateJson` in `init()` instead of calling `setState()` separately:

```kotlin
// ✅ Correct — init + setState are evaluated in a single evaluateJavascript call;
//    the engine never renders a frame with the default "1D" timeframe.
chart.onReady = {
    chart.init(stateJson = savedStateJson)
    chart.loadData(bars)
}

// ❌ Avoid — setState fires after the chart has already rendered once with "1D".
chart.onReady = {
    chart.init()
    chart.setState(savedStateJson)
}
```

For simple cases where you only need to set a specific timeframe (without full state restore), use the `timeframe` parameter in `init()` directly — no `stateJson` required.

### Theme overrides

Use `themeOverrides` (in `init()`) or `setThemeOverrides(overrides)` to selectively override colors for each theme mode. Only the keys you supply are merged on top of the built-in dark/light themes.

```kotlin
// At init time
chart.init(
    theme = "dark",
    symbol = "EURUSD",
    themeOverrides = ThemeOverrides(
        dark = ChartThemeOverride(
            background = "#0a0a0a",
            candle = CandleColors(up = "#00e676", down = "#ff1744"),
            topBar = TopBarColors(btnColor = "#cccccc"),
        )
    ),
)

// Or update at runtime
chart.setThemeOverrides(ThemeOverrides(
    dark = ChartThemeOverride(background = "#111111"),
    light = ChartThemeOverride(background = "#fafafa"),
))
```

All properties at every level are optional — only supply the ones you want to change. Available nested types: `TooltipColors`, `CandleColors`, `VolumeColors`, `UiColors`, `StreamColors`, `DrawingToolbarColors`, `TopBarColors`, `BottomBarColors`, `IndicatorOverlayColors`, `TradeLevelColors`, `TradePanelColors`.

> Raw JSON strings are still supported via `themeOverridesJson` / `setThemeOverrides(jsonString)` for backward compatibility.

### Mobile icon sizing

The chart automatically bumps top-bar icon buttons (settings, fullscreen, drawing toggle) and the floating trade ⊕ button to larger sizes when the container width drops below `uiConfig.drawingToolbar.mobileBreakpoint` (default `480px`). Defaults:

| Element | Desktop | Mobile |
|---------|---------|--------|
| Top-bar icon button container | 26px | 28px |
| Top-bar icon SVG | 14–15px | 16–17px |
| Trade ⊕ button container | 22px | 24px |
| Trade ⊕ icon SVG | 14px | 16px |

Override via `uiConfigJson`:

```kotlin
chart.init(
    theme = "dark",
    symbol = "AAPL",
    enableTrading = true,
    uiConfigJson = """
        {
          "topBar": {
            "mobileIconBtnSize": "30px",
            "mobileDrawBtnIconSize": "18px"
          },
          "tradeButton": {
            "mobileSize": 26,
            "mobileIconSize": 18
          }
        }
    """.trimIndent(),
)
```

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
| `onTradeLevelEdit` | `BridgeEvent.TradeLevelEdit` | User confirmed a TFC edit — `.label`, `.type`, `.data`, `.newLots?`, `.changes[]` (each with `.newLots?` on the `MAIN` change), `.isFullscreen`. When qty was edited this session, the `lots` field embedded in `.data` (and in the `MAIN` change's `data`) is overridden with the new value for convenience. |
| `onTradeLevelClose` | `BridgeEvent.TradeLevelClose` | User tapped × on a level — `.label`, `.type`, `.action`, `.data`, `.isFullscreen` |
| `onTradeLevelDrag` | `BridgeEvent.TradeLevelDrag` | Live price during drag, fires on every move — `.label`, `.newPrice`, `.bracketType?`, `.data`, `.isFullscreen` |
| `onTradeLevelEditOpen` | `BridgeEvent.TradeLevelEditOpen` | User tapped the pencil button **or** (when `hideLevelConfirmCancel=true`) tapped a trade level line — `.label`, `.type`, `.price`, `.side?`, `.stopLossPrice?`, `.takeProfitPrice?`, `.data`, `.isFullscreen` |
| `onTradeLevelBracketActivated` | `BridgeEvent.TradeLevelBracketActivated` | SL/TP bracket auto-placed via `addLevelBracket` or `addBracket` — use `.price` to pre-populate your bracket price input — `.label` (`""` for draft orders, OrderID string for existing levels), `.bracketType`, `.price`, `.isFullscreen` |
| `onTradeLevelConfirmed` | `BridgeEvent.TradeLevelConfirmed` | Chart ✓ button confirmed an edit — `.label`, `.type`, `.isFullscreen` |
| `onDraftInitiated` | `BridgeEvent.DraftInitiated` | New draft order shown — `.side`, `.price`, `.orderType`, `.isFullscreen` |
| `onDraftCancelled` | `BridgeEvent.DraftCancelled` | Draft order cancelled — `.label`, `.isFullscreen` |
| `onTfcToggle` | `BridgeEvent.TfcToggle` | TFC toggled on/off via top bar button or `setTfcActive()` — `.enabled` |
| `onDataRequest` | `BridgeEvent.DataRequest` | Chart requests data for a time range — `.requestId`, `.from`, `.to`, `.timeframe`; call `resolveDataRequest` to respond |
| `onSymbolClick` | `BridgeEvent.SymbolClick` | User tapped the symbol name (requires `onSymbolClick = true` in `init`) |
| `onStateSnapshot` | `BridgeEvent.StateSnapshot` | Response to `getState()` — `.stateJson` |
| `onError` | `BridgeEvent.Error` | Engine error — `.message`, `.code` |
| `onBridgeEvent` | `BridgeEvent` | Generic — fires for every event including those with typed callbacks |

> **`isFullscreen`** is `true` when the chart is in fullscreen mode at the time of the TFC action. Use it to gate toast notifications so they only appear while the chart is covering the full screen.

## Mobile mode — `hideLevelConfirmCancel`

Pass `hideLevelConfirmCancel = true` in `init()` to hide the on-canvas ✓/✗ buttons and drive the edit flow from your native UI instead.

Behaviour changes when this flag is active:

| Action | Result |
|--------|--------|
| Tap a trade level line | `onTradeLevelEditOpen` fires immediately (whole line is the edit target) |
| Tap empty canvas while a level is selected | Edit dismissed; pending drag changes reverted |
| Release a SL/TP bracket drag | `onTradeLevelEdit` fires automatically (no ✓ button needed) |

**Market orders from chart crosshair:** When live BID/ASK data is streaming and the crosshair trade button is tapped at a price inside the spread, `onDraftInitiated` fires with `orderType = "market"` — use this to open your market order form.

**Adding a bracket without a price:** Use `addBracket(bracketType, label?)` from your native form to auto-place a SL or TP bracket at a sensible default price:
- **Draft order (new order, no ID yet):** `chart.addBracket("sl")` — omit `label`; the chart operates on the active draft.
- **Existing order/position:** `chart.addBracket("sl", orderId)` — pass the OrderID/TradeID.

In both cases the chart fires `onTradeLevelBracketActivated` with the computed price — use it to populate your SL/TP input field. The event's `label` is `""` (empty string) for draft orders — check `label.isEmpty()` — and the OrderID string for existing levels.

To remove a bracket: use `removeBracket("sl")` (draft) or `removeBracket("sl", orderId)` (existing).

**Estimated P&L on bracket lines:** Call `setDraftBracketPnl("sl", "-$12.50")` to display a consumer-calculated P&L string next to the active bracket line on the chart. The text attaches to whichever level is the active bracket host — the draft order while drafting, or the currently selected existing pending order / position while modifying. Call `selectLevel(orderId)` (or have the user tap a level) before pushing the P&L text for an existing order. Pass `null` as the `pnlText` to clear.

## License

MIT
