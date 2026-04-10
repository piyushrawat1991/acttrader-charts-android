package com.acttrader.acttradercharts.demo

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.acttrader.acttradercharts.OHLCVBar
import com.acttrader.acttradercharts.ActtraderChart
import com.acttrader.acttradercharts.ActtraderChartsView
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.random.Random

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                var chartView by remember { mutableStateOf<ActtraderChartsView?>(null) }
                var isDark     by remember { mutableStateOf(true) }
                var smaAdded   by remember { mutableStateOf(false) }

                Column(modifier = Modifier.fillMaxSize()) {
                    TopAppBar(
                        title = { Text("ActtraderCharts Demo") },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                        actions = {
                            Row(modifier = Modifier.padding(end = 8.dp)) {
                                Button(
                                    onClick = {
                                        isDark = !isDark
                                        chartView?.setTheme(if (isDark) "dark" else "light")
                                    },
                                    modifier = Modifier.padding(end = 8.dp),
                                ) {
                                    Text("Theme")
                                }
                                Button(
                                    onClick = {
                                        if (!smaAdded) {
                                            chartView?.addIndicator("SMA", mapOf("period" to 20))
                                            smaAdded = true
                                        }
                                    },
                                ) {
                                    Text("SMA")
                                }
                            }
                        },
                    )

                    ActtraderChart(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        onReady = {
                            chartView?.init(enableTrading = true)
                            chartView?.setSymbol("DEMO")
                            chartView?.loadData(generateBars())
                        },
                        onCrosshair = { event ->
                            Log.d("ActtraderCharts", "crosshair  t=${event.time}  o=${event.open}  c=${event.close}")
                        },
                        onPlaceOrder = { event ->
                            Log.d("ActtraderCharts", "placeOrder  side=${event.side}  price=${event.price}  type=${event.orderType}")
                        },
                        onError = { event ->
                            Log.e("ActtraderCharts", "error: ${event.message} (code=${event.code})")
                        },
                        ref = { chartView = it },
                    )
                }
            }
        }
    }

    // ── Synthetic data ────────────────────────────────────────────────────────

    private fun generateBars(): List<OHLCVBar> {
        val bars = mutableListOf<OHLCVBar>()
        val dayMs = TimeUnit.DAYS.toMillis(1)
        var time = System.currentTimeMillis() - dayMs * 200
        var price = 100.0

        repeat(200) {
            val change = (Random.nextDouble() - 0.48) * 2.0   // slight upward drift
            val open   = price
            val close  = (price + change).coerceAtLeast(1.0)
            val high   = maxOf(open, close) + abs(Random.nextDouble())
            val low    = minOf(open, close) - abs(Random.nextDouble())
            val volume = Random.nextDouble(500_000.0, 5_000_000.0)

            bars += OHLCVBar(open = open, high = high, low = low,
                close = close, volume = volume, time = time)
            price = close
            time += dayMs
        }
        return bars
    }
}
