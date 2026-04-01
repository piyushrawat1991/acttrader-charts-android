package com.acttrader.stockchart

/**
 * A single OHLCV price bar.
 *
 * @param time Unix timestamp in milliseconds (UTC).
 */
data class OHLCVBar(
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double,
    val time: Long,
)
