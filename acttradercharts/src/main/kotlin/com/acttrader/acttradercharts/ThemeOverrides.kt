package com.acttrader.acttradercharts

import org.json.JSONObject

/**
 * Per-theme deep-partial color overrides applied on top of the built-in
 * dark / light themes. Only the keys you supply are replaced; everything
 * else falls back to `DARK_THEME` / `LIGHT_THEME`.
 *
 * ```kotlin
 * val overrides = ThemeOverrides(
 *     dark = ChartThemeOverride(
 *         background = "#0a0a0a",
 *         candle = CandleColors(up = "#00e676", down = "#ff1744")
 *     )
 * )
 * chart.setThemeOverrides(overrides)
 * ```
 */
data class ThemeOverrides(
    val dark: ChartThemeOverride? = null,
    val light: ChartThemeOverride? = null,
) {
    /** Serialises to the JSON string expected by the bridge. */
    fun toJsonString(): String = JSONObject().apply {
        dark?.let { put("dark", it.toJson()) }
        light?.let { put("light", it.toJson()) }
    }.toString()
}

/**
 * Deep-partial mirror of the JS `ChartTheme` interface.
 * Every property is optional — supply only the colors you want to override.
 */
data class ChartThemeOverride(
    val background: String? = null,
    val grid: String? = null,
    val axisText: String? = null,
    val axisBorder: String? = null,
    val crosshair: String? = null,
    val tooltip: TooltipColors? = null,
    val candle: CandleColors? = null,
    val volume: VolumeColors? = null,
    val ui: UiColors? = null,
    val drawingToolbar: DrawingToolbarColors? = null,
    val topBar: TopBarColors? = null,
    val bottomBar: BottomBarColors? = null,
    val indicatorOverlay: IndicatorOverlayColors? = null,
    val tradeLevels: TradeLevelColors? = null,
    val tradePanel: TradePanelColors? = null,
) {
    internal fun toJson(): JSONObject = JSONObject().apply {
        background?.let { put("background", it) }
        grid?.let { put("grid", it) }
        axisText?.let { put("axisText", it) }
        axisBorder?.let { put("axisBorder", it) }
        crosshair?.let { put("crosshair", it) }
        tooltip?.let { put("tooltip", it.toJson()) }
        candle?.let { put("candle", it.toJson()) }
        volume?.let { put("volume", it.toJson()) }
        ui?.let { put("ui", it.toJson()) }
        drawingToolbar?.let { put("drawingToolbar", it.toJson()) }
        topBar?.let { put("topBar", it.toJson()) }
        bottomBar?.let { put("bottomBar", it.toJson()) }
        indicatorOverlay?.let { put("indicatorOverlay", it.toJson()) }
        tradeLevels?.let { put("tradeLevels", it.toJson()) }
        tradePanel?.let { put("tradePanel", it.toJson()) }
    }
}

// ── Nested color types ───────────────────────────────────────────────────────

data class TooltipColors(
    val background: String? = null,
    val text: String? = null,
    val border: String? = null,
) {
    internal fun toJson(): JSONObject = JSONObject().apply {
        background?.let { put("background", it) }
        text?.let { put("text", it) }
        border?.let { put("border", it) }
    }
}

data class CandleColors(
    val up: String? = null,
    val down: String? = null,
    val wickUp: String? = null,
    val wickDown: String? = null,
    val borderUp: String? = null,
    val borderDown: String? = null,
) {
    internal fun toJson(): JSONObject = JSONObject().apply {
        up?.let { put("up", it) }
        down?.let { put("down", it) }
        wickUp?.let { put("wickUp", it) }
        wickDown?.let { put("wickDown", it) }
        borderUp?.let { put("borderUp", it) }
        borderDown?.let { put("borderDown", it) }
    }
}

data class VolumeColors(
    val up: String? = null,
    val down: String? = null,
) {
    internal fun toJson(): JSONObject = JSONObject().apply {
        up?.let { put("up", it) }
        down?.let { put("down", it) }
    }
}

data class StreamColors(
    val connected: String? = null,
    val reconnecting: String? = null,
    val disconnected: String? = null,
) {
    internal fun toJson(): JSONObject = JSONObject().apply {
        connected?.let { put("connected", it) }
        reconnecting?.let { put("reconnecting", it) }
        disconnected?.let { put("disconnected", it) }
    }
}

data class UiColors(
    val accent: String? = null,
    val accentText: String? = null,
    val accentBg: String? = null,
    val accentBgStrong: String? = null,
    val disabledText: String? = null,
    val soonBadgeText: String? = null,
    val stream: StreamColors? = null,
) {
    internal fun toJson(): JSONObject = JSONObject().apply {
        accent?.let { put("accent", it) }
        accentText?.let { put("accentText", it) }
        accentBg?.let { put("accentBg", it) }
        accentBgStrong?.let { put("accentBgStrong", it) }
        disabledText?.let { put("disabledText", it) }
        soonBadgeText?.let { put("soonBadgeText", it) }
        stream?.let { put("stream", it.toJson()) }
    }
}

data class DrawingToolbarColors(
    val iconColor: String? = null,
    val activeIconColor: String? = null,
    val flyoutLabelColor: String? = null,
    val flyoutHeadingColor: String? = null,
) {
    internal fun toJson(): JSONObject = JSONObject().apply {
        iconColor?.let { put("iconColor", it) }
        activeIconColor?.let { put("activeIconColor", it) }
        flyoutLabelColor?.let { put("flyoutLabelColor", it) }
        flyoutHeadingColor?.let { put("flyoutHeadingColor", it) }
    }
}

data class TopBarColors(
    val btnColor: String? = null,
    val activeBtnColor: String? = null,
    val flyoutRowColor: String? = null,
    val flyoutCategoryColor: String? = null,
) {
    internal fun toJson(): JSONObject = JSONObject().apply {
        btnColor?.let { put("btnColor", it) }
        activeBtnColor?.let { put("activeBtnColor", it) }
        flyoutRowColor?.let { put("flyoutRowColor", it) }
        flyoutCategoryColor?.let { put("flyoutCategoryColor", it) }
    }
}

data class BottomBarColors(
    val btnColor: String? = null,
    val activeBtnBg: String? = null,
    val activeBtnText: String? = null,
    val activeBtnBorder: String? = null,
) {
    internal fun toJson(): JSONObject = JSONObject().apply {
        btnColor?.let { put("btnColor", it) }
        activeBtnBg?.let { put("activeBtnBg", it) }
        activeBtnText?.let { put("activeBtnText", it) }
        activeBtnBorder?.let { put("activeBtnBorder", it) }
    }
}

data class IndicatorOverlayColors(
    val pillBg: String? = null,
    val iconColor: String? = null,
    val dropdownHoverBg: String? = null,
) {
    internal fun toJson(): JSONObject = JSONObject().apply {
        pillBg?.let { put("pillBg", it) }
        iconColor?.let { put("iconColor", it) }
        dropdownHoverBg?.let { put("dropdownHoverBg", it) }
    }
}

data class TradeLevelColors(
    val tradeLine: String? = null,
    val positionLine: String? = null,
    val pendingBuyLine: String? = null,
    val pendingSellLine: String? = null,
    val labelText: String? = null,
    val closeBtn: String? = null,
    val closeBtnText: String? = null,
    val boxBg: String? = null,
    val boxBgHover: String? = null,
    val dragHandle: String? = null,
    val dragHandleHover: String? = null,
) {
    internal fun toJson(): JSONObject = JSONObject().apply {
        tradeLine?.let { put("tradeLine", it) }
        positionLine?.let { put("positionLine", it) }
        pendingBuyLine?.let { put("pendingBuyLine", it) }
        pendingSellLine?.let { put("pendingSellLine", it) }
        labelText?.let { put("labelText", it) }
        closeBtn?.let { put("closeBtn", it) }
        closeBtnText?.let { put("closeBtnText", it) }
        boxBg?.let { put("boxBg", it) }
        boxBgHover?.let { put("boxBgHover", it) }
        dragHandle?.let { put("dragHandle", it) }
        dragHandleHover?.let { put("dragHandleHover", it) }
    }
}

data class TradePanelColors(
    val background: String? = null,
    val border: String? = null,
    val headerBg: String? = null,
    val tabActive: String? = null,
    val tabInactive: String? = null,
    val rowHoverBg: String? = null,
    val rowText: String? = null,
    val rowSubText: String? = null,
) {
    internal fun toJson(): JSONObject = JSONObject().apply {
        background?.let { put("background", it) }
        border?.let { put("border", it) }
        headerBg?.let { put("headerBg", it) }
        tabActive?.let { put("tabActive", it) }
        tabInactive?.let { put("tabInactive", it) }
        rowHoverBg?.let { put("rowHoverBg", it) }
        rowText?.let { put("rowText", it) }
        rowSubText?.let { put("rowSubText", it) }
    }
}
