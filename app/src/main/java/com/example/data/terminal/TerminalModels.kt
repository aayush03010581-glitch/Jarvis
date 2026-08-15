package com.example.data.terminal

enum class LineType {
    INPUT,
    OUTPUT,
    SYSTEM,
    SUCCESS,
    WARNING,
    ERROR,
    JARVIS,
    STARK_DIRECTIVE,
    PROGRESS,
    HEADER
}

data class TerminalLine(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: LineType,
    val tag: String = "",
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isAnimated: Boolean = false
)

data class SuitTelemetry(
    val arcReactorPower: Int = 100, // 0 - 100%
    val suitIntegrity: Int = 98,   // 0 - 100%
    val repulsorCharge: Int = 100,  // 0 - 100%
    val thrusterEfficiency: Int = 95,// 0 - 100%
    val nanotechDensity: Int = 99,  // 0 - 100%
    val coreTemperature: Float = 42.5f, // Celsius
    val isCombatMode: Boolean = false,
    val activeProtocol: String = "STANDBY"
)
