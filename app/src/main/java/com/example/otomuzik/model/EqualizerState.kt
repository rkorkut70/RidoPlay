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

data class EqualizerState(
    val isEnabled: Boolean = true,
    val bands: List<EqBand> = emptyList(),
    val presets: List<String> = emptyList(),
    val currentPreset: String = "Normal",
    val bassBoostStrength: Short = 0, // 0..1000
    val isBassBoostSupported: Boolean = true,
    val isLoudnessEnhancerEnabled: Boolean = false
)
