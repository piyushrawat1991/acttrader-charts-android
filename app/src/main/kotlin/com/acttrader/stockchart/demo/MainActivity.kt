package com.acttrader.stockchart.demo

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.acttrader.stockchart.OHLCVBar
import com.acttrader.stockchart.StockChartView
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var chart: StockChartView
    private var isDark = true
    private var smaAdded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))

        chart = findViewById(R.id.chartView)

        chart.onReady = {
            chart.init(enableTrading = true)
            chart.setSymbol("DEMO")
            chart.loadData(generateBars())
        }

        chart.onCrosshair = { event ->
            Log.d("StockChart", "crosshair  t=${event.time}  o=${event.open}  c=${event.close}")
        }

        chart.onPlaceOrder = { event ->
            Log.d("StockChart", "placeOrder  side=${event.side}  price=${event.price}  type=${event.orderType}")
        }

        chart.onError = { event ->
            Log.e("StockChart", "error: ${event.message} (code=${event.code})")
        }

        findViewById<Button>(R.id.btnToggleTheme).setOnClickListener {
            isDark = !isDark
            chart.setTheme(if (isDark) "dark" else "light")
        }

        findViewById<Button>(R.id.btnAddSma).setOnClickListener {
            if (!smaAdded) {
                chart.addIndicator("SMA", mapOf("period" to 20))
                smaAdded = true
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        chart.destroy()
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
