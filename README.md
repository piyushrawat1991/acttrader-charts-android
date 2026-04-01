# stockchart-android

Android Kotlin library that renders [`@acttrader/stockchart`](https://github.com/acttrader/stockchart) inside a `WebView`.

## Requirements

- minSdk 24 (Android 7.0)
- Kotlin 1.9+

## Installation

Add GitHub Packages to your project's `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/acttrader/stockchart-android")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
                password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
```

Then add the dependency:

```kotlin
dependencies {
    implementation("com.acttrader:stockchart:0.1.0")
}
```

## Basic usage

```kotlin
// In your Activity or Fragment
val chart = StockChartView(context)

chart.onReady = {
    chart.loadData(bars)           // List<OHLCVBar>
    chart.setTheme("dark")
    chart.setSymbol("AAPL")
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
<com.acttrader.stockchart.StockChartView
    android:id="@+id/stockChart"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

## Commands

| Method | Description |
|--------|-------------|
| `loadData(bars, fitAll)` | Replace full dataset |
| `pushTick(bid, ask, timestamp)` | Stream a live tick |
| `setTheme("dark" \| "light")` | Switch theme |
| `setSeries(type)` | Change chart type (`"candlestick"`, `"line"`, `"area"`, `"ohlc"`) |
| `setSymbol(symbol)` | Update displayed symbol name |
| `addIndicator(name, params?)` | Add study (e.g. `"SMA"`, `"RSI"`) |
| `removeIndicator(name)` | Remove study |
| `setDrawingTool(tool?)` | Activate drawing tool, `null` to deactivate |
| `clearAllDrawings()` | Remove all drawings |
| `getState()` | Request state snapshot (async → `onStateSnapshot`) |
| `setState(stateJson)` | Restore a prior state |
| `destroy()` | Release WebView resources |

## Events

| Callback | Type | Description |
|----------|------|-------------|
| `onReady` | `() -> Unit` | Engine ready |
| `onCrosshair` | `BridgeEvent.Crosshair` | Crosshair moved |
| `onBarClick` | `BridgeEvent.BarClick` | Bar tapped |
| `onViewportChange` | `BridgeEvent.ViewportChange` | Pan / zoom |
| `onSeriesChange` | `BridgeEvent.SeriesChange` | Series type changed |
| `onTimeframeChange` | `BridgeEvent.TimeframeChange` | Timeframe changed |
| `onDataLoaded` | `BridgeEvent.DataLoaded` | `loadData` complete |
| `onNewBar` | `BridgeEvent.NewBar` | New bar appended at live edge |
| `onStateSnapshot` | `BridgeEvent.StateSnapshot` | Response to `getState()` |
| `onError` | `BridgeEvent.Error` | Engine error |
| `onBridgeEvent` | `BridgeEvent` | Generic — fires for every event |

## License

MIT
