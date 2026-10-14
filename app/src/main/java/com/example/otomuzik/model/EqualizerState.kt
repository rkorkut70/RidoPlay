package com.example.otomuzik.model

data class EqBand(
    val index: Short,
    val centerFreqHz: Int,
    val levelMb: Short,
    val minLevelMb: Short = -1500,
    val maxLevelMb: Short = 1500
) {
    val frequencyLabel: String
        get() = if (centerFreqHz >= 1000) "${centerFreqHz / 1000} kHz" else "$centerFreqHz Hz"
}

val DEFAULT_EQ_BANDS = listOf(
    EqBand(0, 60, 0, -1500, 1500),
    EqBand(1, 230, 0, -1500, 1500),
    EqBand(2, 910, 0, -1500, 1500),
    EqBand(3, 3600, 0, -1500, 1500),
    EqBand(4, 14000, 0, -1500, 1500)
)

data class EqualizerState(
    val isEnabled: Boolean = true,
    val bands: List<EqBand> = DEFAULT_EQ_BANDS,
    val presets: List<String> = emptyList(),
    val currentPreset: String = "Normal",
    val bassBoostStrength: Short = 0, // 0..1000
    val isBassBoostSupported: Boolean = true,
    val isLoudnessEnhancerEnabled: Boolean = false
)
