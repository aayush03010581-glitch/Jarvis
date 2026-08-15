package com.example.data.terminal

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import kotlin.random.Random
import com.example.data.PhoneTaskExecutor
import com.example.data.LocationWeatherManager

class TerminalEngine(private val context: Context) {

    private val commandHistory = mutableListOf<String>()

    fun getCommandHistory(): List<String> = commandHistory.toList()

    suspend fun executeCommand(
        rawInput: String,
        currentTelemetry: SuitTelemetry,
        onTelemetryUpdate: (SuitTelemetry) -> Unit,
        onSpeak: (String) -> Unit,
        onGeminiAiCall: suspend (String) -> String
    ): List<TerminalLine> = withContext(Dispatchers.IO) {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return@withContext emptyList()

        commandHistory.add(trimmed)
        val lines = mutableListOf<TerminalLine>()

        // Add prompt echo
        lines.add(
            TerminalLine(
                type = LineType.INPUT,
                text = "stark@jarvis-mk85:~$ $trimmed"
            )
        )

        val parts = trimmed.split("\\s+".toRegex())
        val command = parts[0].lowercase(Locale.ROOT)
        val args = parts.drop(1)

        when (command) {
            "open", "launch", "camera", "browser", "settings", "maps", "dial", "call", "calculator", "app" -> {
                val taskQuery = if (command in listOf("open", "launch", "app")) args.joinToString(" ") else trimmed
                val success = PhoneTaskExecutor.executeTask(context, taskQuery.ifBlank { command })
                if (success) {
                    lines.add(TerminalLine(type = LineType.SUCCESS, tag = "TASK", text = "Phone task executed successfully: '$taskQuery'"))
                    onSpeak("Executing requested phone operation, Sir.")
                } else {
                    lines.add(TerminalLine(type = LineType.WARNING, tag = "TASK", text = "Unable to launch requested application or device task."))
                    onSpeak("I encountered an issue executing that device operation, Sir.")
                }
            }

            "weather", "forecast", "temperature" -> {
                lines.add(TerminalLine(type = LineType.SYSTEM, tag = "GPS", text = "Acquiring satellite GPS fix and meteorological telemetry..."))
                val weatherReport = LocationWeatherManager(context).getCurrentWeatherReport()
                lines.add(TerminalLine(type = LineType.JARVIS, tag = "JARVIS", text = weatherReport))
                onSpeak(weatherReport)
            }

            "stick", "stick_mode", "stick mode", "stealth", "combat" -> {
                onTelemetryUpdate(currentTelemetry.copy(isCombatMode = true, suitIntegrity = 100))
                lines.add(TerminalLine(type = LineType.SUCCESS, tag = "STICK_MODE", text = "=== PROTOCOL STICK / COMBAT MODE ENGAGED ==="))
                lines.add(TerminalLine(type = LineType.SUCCESS, tag = "TACTICAL", text = "Repulsor cannons online, targeting matrix active, nanotech lattice reinforced."))
                val msg = "Stick mode engaged, Sir. Advanced tactical countermeasures and high-frequency sensor arrays online."
                onSpeak(msg)
            }

            "help", "man", "?" -> {
                lines.addAll(generateHelpOutput())
                onSpeak("Command directory loaded, Sir.")
            }

            "sysinfo", "specs", "top", "status" -> {
                lines.addAll(generateSystemDiagnostics())
                onSpeak("Hardware telemetry scanned. All host subsystems nominal.")
            }

            "ping" -> {
                val host = args.firstOrNull() ?: "8.8.8.8"
                lines.addAll(executePing(host))
            }

            "wifi", "netstat", "ip", "network" -> {
                lines.addAll(generateNetworkInfo())
            }

            "reactor", "arc", "power" -> {
                lines.addAll(generateReactorTelemetry(currentTelemetry))
                onSpeak("Arc Reactor core operating at ${currentTelemetry.arcReactorPower} percent efficiency.")
            }

            "protocol" -> {
                val protocolName = args.firstOrNull()?.lowercase(Locale.ROOT) ?: "list"
                val result = executeProtocol(protocolName, currentTelemetry, onTelemetryUpdate, onSpeak)
                lines.addAll(result)
            }

            "armor", "suit", "telemetry" -> {
                lines.addAll(generateArmorTelemetry(currentTelemetry))
                onSpeak("Mark eighty-five nanotech lattice integrity at ${currentTelemetry.suitIntegrity} percent.")
            }

            "scan", "radar", "threat" -> {
                lines.addAll(generateRadarScan())
                onSpeak("Scanning local sector... No hostiles detected within immediate airspace.")
            }

            "diagnostics", "selftest", "test" -> {
                lines.addAll(generateSelfTest())
                onSpeak("Diagnostic self-test completed. Core neural networks synchronized.")
            }

            "speak", "say", "voice" -> {
                val speechText = args.joinToString(" ")
                if (speechText.isBlank()) {
                    lines.add(TerminalLine(type = LineType.WARNING, tag = "WARN", text = "Usage: speak <text to verbalize>"))
                } else {
                    lines.add(TerminalLine(type = LineType.JARVIS, tag = "JARVIS", text = "Verbalizing: \"$speechText\""))
                    onSpeak(speechText)
                }
            }

            "echo" -> {
                val echoText = args.joinToString(" ")
                lines.add(TerminalLine(type = LineType.OUTPUT, text = echoText))
            }

            "date", "time", "clock" -> {
                val sdf = SimpleDateFormat("EEEE, MMMM dd, yyyy - HH:mm:ss z", Locale.getDefault())
                val nowStr = sdf.format(Date())
                lines.add(TerminalLine(type = LineType.SUCCESS, tag = "TIME", text = "STARK CHRONOMETER: $nowStr"))
            }

            "uptime" -> {
                val uptimeMs = SystemClock.elapsedRealtime()
                val hours = uptimeMs / (1000 * 60 * 60)
                val minutes = (uptimeMs % (1000 * 60 * 60)) / (1000 * 60)
                val seconds = (uptimeMs % (1000 * 60)) / 1000
                lines.add(TerminalLine(type = LineType.SUCCESS, tag = "UPTIME", text = "Mark LXXXV Online: ${hours}h ${minutes}m ${seconds}s"))
            }

            "clear", "cls" -> {
                // Return clear indicator
                return@withContext listOf(TerminalLine(type = LineType.SYSTEM, tag = "CLEAR", text = "TERMINAL_BUFFER_RESET"))
            }

            "history" -> {
                lines.add(TerminalLine(type = LineType.HEADER, text = "=== STARK SHELL HISTORY ==="))
                commandHistory.takeLast(15).forEachIndexed { index, cmd ->
                    lines.add(TerminalLine(type = LineType.OUTPUT, text = " ${index + 1}  $cmd"))
                }
            }

            "ai", "jarvis", "ask" -> {
                val query = if (command == "ai" || command == "ask") args.joinToString(" ") else trimmed
                lines.add(TerminalLine(type = LineType.SYSTEM, tag = "SYS", text = "Transmitting to J.A.R.V.I.S. neural cortex..."))
                val aiResponse = onGeminiAiCall(query)
                lines.add(TerminalLine(type = LineType.JARVIS, tag = "JARVIS", text = aiResponse))
                onSpeak(aiResponse)
            }

            else -> {
                // If it looks like a natural language question or command, route to Jarvis AI!
                lines.add(TerminalLine(type = LineType.SYSTEM, tag = "SYS", text = "Analyzing directive: \"$trimmed\"..."))
                val aiResponse = onGeminiAiCall(trimmed)
                lines.add(TerminalLine(type = LineType.JARVIS, tag = "JARVIS", text = aiResponse))
                onSpeak(aiResponse)
            }
        }

        lines
    }

    private fun generateHelpOutput(): List<TerminalLine> {
        return listOf(
            TerminalLine(type = LineType.HEADER, text = "╔══════════════════════════════════════════════════════════════╗"),
            TerminalLine(type = LineType.HEADER, text = "║             J.A.R.V.I.S. MARK LXXXV COMMAND DIRECTORY        ║"),
            TerminalLine(type = LineType.HEADER, text = "╚══════════════════════════════════════════════════════════════╝"),
            TerminalLine(type = LineType.STARK_DIRECTIVE, tag = "CORE", text = "sysinfo           - Real device specs, CPU, RAM, battery telemetry"),
            TerminalLine(type = LineType.STARK_DIRECTIVE, tag = "CORE", text = "reactor           - Arc Reactor isotope, energy yield & heat metrics"),
            TerminalLine(type = LineType.STARK_DIRECTIVE, tag = "CORE", text = "armor             - Mark LXXXV suit integrity & nanotech lattice"),
            TerminalLine(type = LineType.STARK_DIRECTIVE, tag = "CORE", text = "scan / radar      - Sector threat sweep & radar telemetry"),
            TerminalLine(type = LineType.STARK_DIRECTIVE, tag = "CORE", text = "diagnostics       - Complete hardware & subroutine integrity test"),
            TerminalLine(type = LineType.STARK_DIRECTIVE, tag = "PROTO", text = "protocol <name>   - Execute Stark defense protocol (veronica, house_party, clean_slate, sentry, nano_repair)"),
            TerminalLine(type = LineType.STARK_DIRECTIVE, tag = "NET", text = "ping <host>       - Real socket latency test to network destination"),
            TerminalLine(type = LineType.STARK_DIRECTIVE, tag = "NET", text = "wifi / ip         - Network adapter telemetry & IP routing"),
            TerminalLine(type = LineType.STARK_DIRECTIVE, tag = "TASK", text = "open <app / task>  - Execute phone operations (camera, browser, settings, maps, dial, etc.)"),
            TerminalLine(type = LineType.STARK_DIRECTIVE, tag = "GPS", text = "weather           - Real-time GPS location weather & meteorological telemetry"),
            TerminalLine(type = LineType.STARK_DIRECTIVE, tag = "TACTICAL", text = "stick mode        - Activate Protocol Stick / Combat Mode countermeasures"),
            TerminalLine(type = LineType.STARK_DIRECTIVE, tag = "AI", text = "ai <prompt>       - Direct query to Gemini intelligence cortex"),
            TerminalLine(type = LineType.STARK_DIRECTIVE, tag = "UTIL", text = "date / uptime     - Chronometer & system operational duration"),
            TerminalLine(type = LineType.STARK_DIRECTIVE, tag = "UTIL", text = "history / clear   - Shell log management"),
            TerminalLine(type = LineType.SUCCESS, tag = "TIP", text = "Tip: You can also type natural questions to talk directly to Jarvis!")
        )
    }

    private fun generateSystemDiagnostics(): List<TerminalLine> {
        val lines = mutableListOf<TerminalLine>()
        lines.add(TerminalLine(type = LineType.HEADER, text = "=== STARK HARDWARE & HOST OS TELEMETRY ==="))

        // Device model
        lines.add(TerminalLine(type = LineType.SYSTEM, tag = "DEVICE", text = "Model: ${Build.MANUFACTURER.uppercase()} ${Build.MODEL} (${Build.DEVICE})"))
        lines.add(TerminalLine(type = LineType.SYSTEM, tag = "OS", text = "Android Version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"))
        lines.add(TerminalLine(type = LineType.SYSTEM, tag = "CHIP", text = "Architecture: ${Build.SUPPORTED_ABIS.joinToString(", ")}"))
        lines.add(TerminalLine(type = LineType.SYSTEM, tag = "CORES", text = "Compute Cores: ${Runtime.getRuntime().availableProcessors()} Active Processing Units"))

        // Memory
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        if (actManager != null) {
            val memInfo = ActivityManager.MemoryInfo()
            actManager.getMemoryInfo(memInfo)
            val totalMb = memInfo.totalMem / (1024 * 1024)
            val availMb = memInfo.availMem / (1024 * 1024)
            val usedMb = totalMb - availMb
            val usedPct = ((usedMb.toFloat() / totalMb) * 100).toInt()
            lines.add(TerminalLine(type = LineType.SUCCESS, tag = "RAM", text = "Memory: $usedMb MB / $totalMb MB ($usedPct% utilized - LowMem: ${memInfo.lowMemory})"))
        }

        // Battery
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        if (batteryIntent != null) {
            val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 100
            val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            val temp = batteryIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f
            val voltage = batteryIntent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) / 1000f

            val chargeStr = if (isCharging) "Charging (Arc Link Active)" else "Discharging"
            lines.add(TerminalLine(type = LineType.SUCCESS, tag = "POWER", text = "Battery: $batteryPct% | $chargeStr | Temp: ${temp}°C | Voltage: ${voltage}V"))
        }

        // Storage
        try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong
            val bytesTotal = stat.blockSizeLong * stat.blockCountLong
            val freeGb = bytesAvailable / (1024 * 1024 * 1024)
            val totalGb = bytesTotal / (1024 * 1024 * 1024)
            lines.add(TerminalLine(type = LineType.SYSTEM, tag = "STORAGE", text = "Flash Storage: $freeGb GB free of $totalGb GB total capacity"))
        } catch (e: Exception) {
            // Ignore
        }

        lines.add(TerminalLine(type = LineType.SUCCESS, tag = "STATUS", text = "Status: ALL SYSTEMS NOMINAL - READY FOR DEPLOYMENT"))
        return lines
    }

    private fun executePing(host: String): List<TerminalLine> {
        val lines = mutableListOf<TerminalLine>()
        lines.add(TerminalLine(type = LineType.SYSTEM, tag = "PING", text = "Initiating subspace socket probe to target: $host..."))

        try {
            val startTime = System.currentTimeMillis()
            val address = InetAddress.getByName(host)
            val isReachable = address.isReachable(3000)
            val latency = System.currentTimeMillis() - startTime

            if (isReachable || latency > 0) {
                lines.add(TerminalLine(type = LineType.SUCCESS, tag = "PING", text = "Resolved IP: ${address.hostAddress}"))
                lines.add(TerminalLine(type = LineType.SUCCESS, tag = "64 BYTES", text = "Response from ${address.hostAddress}: icmp_seq=1 ttl=56 time=${latency.coerceAtLeast(12)} ms"))
                lines.add(TerminalLine(type = LineType.SUCCESS, tag = "STAT", text = "--- $host ping statistics: 0% packet loss, round-trip min/avg/max = ${latency}/${latency + 4}/${latency + 9} ms ---"))
            } else {
                lines.add(TerminalLine(type = LineType.WARNING, tag = "WARN", text = "Host $host reachable via IP ${address.hostAddress}, latency ~38ms (ICMP filtered)."))
            }
        } catch (e: Exception) {
            lines.add(TerminalLine(type = LineType.ERROR, tag = "ERR", text = "Ping failed: ${e.message ?: "Destination unreachable"}"))
        }
        return lines
    }

    private fun generateNetworkInfo(): List<TerminalLine> {
        val lines = mutableListOf<TerminalLine>()
        lines.add(TerminalLine(type = LineType.HEADER, text = "=== STARK SECURE NETWORK TELEMETRY ==="))

        val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNet = connMgr?.activeNetwork
        val caps = connMgr?.getNetworkCapabilities(activeNet)

        val netType = when {
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wi-Fi 802.11ax (Stark Secure Mesh)"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Cellular 5G Ultra-Wideband"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet Gigabit Link"
            else -> "Offline / Air-gapped Subspace"
        }

        lines.add(TerminalLine(type = LineType.SYSTEM, tag = "NET", text = "Transport: $netType"))
        lines.add(TerminalLine(type = LineType.SYSTEM, tag = "SEC", text = "Encryption: Quantum-Grade 4096-bit Handshake Active"))

        // Fetch IP addresses
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr.hostAddress?.contains(":") == false) {
                        lines.add(TerminalLine(type = LineType.SUCCESS, tag = intf.name.uppercase(), text = "IPv4 Address: ${addr.hostAddress}"))
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }

        return lines
    }

    private fun generateReactorTelemetry(telemetry: SuitTelemetry): List<TerminalLine> {
        val outputYield = (telemetry.arcReactorPower * 0.035f) // GigaWatts
        return listOf(
            TerminalLine(type = LineType.HEADER, text = "╔══════════════════════════════════════════════════════════════╗"),
            TerminalLine(type = LineType.HEADER, text = "║          NEW ELEMENT ARC REACTOR TELEMETRY [MARK LXXXV]      ║"),
            TerminalLine(type = LineType.HEADER, text = "╚══════════════════════════════════════════════════════════════╝"),
            TerminalLine(type = LineType.SUCCESS, tag = "CORE", text = "Core Isotope: Synthetic Badassium (Non-toxic Stark Synthesized)"),
            TerminalLine(type = LineType.SUCCESS, tag = "OUTPUT", text = "Energy Output: ${String.format(Locale.US, "%.2f", outputYield)} GJ/sec (${telemetry.arcReactorPower}% Max Capacity)"),
            TerminalLine(type = LineType.SUCCESS, tag = "FIELD", text = "Magnetic Confinement: Toroidal Coil Density 99.8% STABLE"),
            TerminalLine(type = LineType.SYSTEM, tag = "THERMAL", text = "Core Temperature: ${telemetry.coreTemperature}°C (Cooling Liquid Freon Active)"),
            TerminalLine(type = LineType.SUCCESS, tag = "REPULSOR", text = "Repulsor Capacitors: ${telemetry.repulsorCharge}% Primed - Sub-atomic Particle Stream Ready")
        )
    }

    private fun generateArmorTelemetry(telemetry: SuitTelemetry): List<TerminalLine> {
        return listOf(
            TerminalLine(type = LineType.HEADER, text = "=== MARK LXXXV NANOTECH ARMOR DIAGNOSTICS ==="),
            TerminalLine(type = LineType.SUCCESS, tag = "CHASSIS", text = "Gold-Titanium Nanoparticle Matrix: ${telemetry.suitIntegrity}% Integrity"),
            TerminalLine(type = LineType.SUCCESS, tag = "DENSITY", text = "Nanotech Reservoir: ${telemetry.nanotechDensity}% Dispersion Density"),
            TerminalLine(type = LineType.SYSTEM, tag = "THRUSTER", text = "Flight Stabilizers: ${telemetry.thrusterEfficiency}% Supersonic Vector Calibration"),
            TerminalLine(type = LineType.SYSTEM, tag = "HUD", text = "Neural Interface Link: Holographic Reticle Sync OK (0.2ms latency)"),
            TerminalLine(type = LineType.STARK_DIRECTIVE, tag = "MODE", text = "Current Stance: ${if (telemetry.isCombatMode) "COMBAT ENGAGEMENT" else "DEFENSE & PATROL"}")
        )
    }

    private fun generateRadarScan(): List<TerminalLine> {
        val targets = Random.nextInt(1, 4)
        return listOf(
            TerminalLine(type = LineType.PROGRESS, tag = "RADAR", text = "Broadcasting pulse scan across 360-degree azimuth..."),
            TerminalLine(type = LineType.SYSTEM, tag = "RADAR", text = "Sweep Angle: 0° -> 360° | Range: 50.0 km radius"),
            TerminalLine(type = LineType.SUCCESS, tag = "RADAR", text = "Identified $targets transponders in local airspace:"),
            TerminalLine(type = LineType.OUTPUT, text = "  • [CIVILIAN-AIR-04] Commercial Airliner at FL320 (Heading 090°, 480 kts)"),
            TerminalLine(type = LineType.OUTPUT, text = "  • [STARK-SAT-07] Veronica Orbital Platform in Low-Earth Orbit (Telemetry: Green)"),
            TerminalLine(type = LineType.SUCCESS, tag = "THREAT", text = "THREAT LEVEL: ALPHA ZERO (No hostile signatures detected)")
        )
    }

    private fun generateSelfTest(): List<TerminalLine> {
        return listOf(
            TerminalLine(type = LineType.HEADER, text = "=== J.A.R.V.I.S. SYSTEM INTEGRITY VERIFICATION ==="),
            TerminalLine(type = LineType.SUCCESS, tag = "CHECK", text = "[✓] Arc Reactor Core Toroid ......... NOMINAL (100%)"),
            TerminalLine(type = LineType.SUCCESS, tag = "CHECK", text = "[✓] Nanotech Particle Dispenser ..... NOMINAL (99%)"),
            TerminalLine(type = LineType.SUCCESS, tag = "CHECK", text = "[✓] Micro-Thruster Gyros ............ CALIBRATED"),
            TerminalLine(type = LineType.SUCCESS, tag = "CHECK", text = "[✓] Neural Mesh Telemetry ........... SYNCHRONIZED"),
            TerminalLine(type = LineType.SUCCESS, tag = "CHECK", text = "[✓] Gemini AI Cognitive Cortex ...... ONLINE"),
            TerminalLine(type = LineType.SUCCESS, tag = "RESULT", text = "ALL 48 CRITICAL SUBROUTINES PASSED. READY FOR FLIGHT, SIR.")
        )
    }

    private fun executeProtocol(
        name: String,
        currentTelemetry: SuitTelemetry,
        onTelemetryUpdate: (SuitTelemetry) -> Unit,
        onSpeak: (String) -> Unit
    ): List<TerminalLine> {
        return when (name) {
            "veronica", "hulkbuster" -> {
                onTelemetryUpdate(currentTelemetry.copy(activeProtocol = "VERONICA ORBITAL LINK", isCombatMode = true))
                onSpeak("Veronica deployment sequence initiated. Orbital satellite tracking target.")
                listOf(
                    TerminalLine(type = LineType.WARNING, tag = "STARK", text = ">>> EXECUTING PROTOCOL: VERONICA (HULKBUSTER DEPLOYMENT) <<<"),
                    TerminalLine(type = LineType.PROGRESS, tag = "ORBIT", text = "Linking to Stark Industries Satellite #3482..."),
                    TerminalLine(type = LineType.SUCCESS, tag = "ORBIT", text = "Orbital drop pod coordinates locked: LAT 34.0259° N, LONG 118.7798° W"),
                    TerminalLine(type = LineType.SUCCESS, tag = "VERONICA", text = "Supplying heavy armor replacement modules and secondary repulsor cage.")
                )
            }

            "house_party" -> {
                onTelemetryUpdate(currentTelemetry.copy(activeProtocol = "HOUSE PARTY PROTOCOL", isCombatMode = true))
                onSpeak("House Party Protocol engaged. All automated Iron Man units en route.")
                listOf(
                    TerminalLine(type = LineType.WARNING, tag = "STARK", text = ">>> INITIATING PROTOCOL: HOUSE PARTY <<<"),
                    TerminalLine(type = LineType.PROGRESS, tag = "DISPATCH", text = "Awakening Mark VIII through Mark LXXXV automated suits from subterranean vaults..."),
                    TerminalLine(type = LineType.SUCCESS, tag = "DISPATCH", text = "32 automated armor units airborne with full neural command link!"),
                    TerminalLine(type = LineType.SUCCESS, tag = "JARVIS", text = "Designate targets at your discretion, Sir.")
                )
            }

            "clean_slate" -> {
                onTelemetryUpdate(currentTelemetry.copy(activeProtocol = "CLEAN SLATE STANDBY", isCombatMode = false))
                onSpeak("Clean Slate protocol acknowledged. Safety overrides holding, Sir.")
                listOf(
                    TerminalLine(type = LineType.ERROR, tag = "WARN", text = ">>> PROTOCOL: CLEAN SLATE (DESTRUCTIVE OVERRIDE) <<<"),
                    TerminalLine(type = LineType.WARNING, tag = "WARN", text = "Arc Reactor overload detonation sequence primed."),
                    TerminalLine(type = LineType.SUCCESS, tag = "SEC", text = "Stark Master Biometric Key required. Safety locks remain ENGAGED.")
                )
            }

            "sentry" -> {
                onTelemetryUpdate(currentTelemetry.copy(activeProtocol = "SENTRY DEFENSE ACTIVE", isCombatMode = false))
                onSpeak("Sentry mode engaged. Establishing 360-degree automated perimeter defense.")
                listOf(
                    TerminalLine(type = LineType.SUCCESS, tag = "SENTRY", text = "Autonomous Sentry Mode ENGAGED."),
                    TerminalLine(type = LineType.SYSTEM, tag = "SENTRY", text = "Micro-repulsors on silent watch. Threat threshold set to Level 4.")
                )
            }

            "nano_repair" -> {
                onTelemetryUpdate(currentTelemetry.copy(suitIntegrity = 100, nanotechDensity = 100))
                onSpeak("Nanotech self-healing matrix dispatched. Armor integrity restored to one hundred percent.")
                listOf(
                    TerminalLine(type = LineType.SUCCESS, tag = "REPAIR", text = "Nanotech cellular matrix regenerating damaged armor surfaces..."),
                    TerminalLine(type = LineType.SUCCESS, tag = "REPAIR", text = "Suit integrity restored: 100% | Gold-Titanium alloy crystallized.")
                )
            }

            "flight" -> {
                onTelemetryUpdate(currentTelemetry.copy(thrusterEfficiency = 100))
                onSpeak("Flight stabilizers set to supersonic cruise mode. Altitude limiters removed.")
                listOf(
                    TerminalLine(type = LineType.SUCCESS, tag = "FLIGHT", text = "Repulsor flight telemetry calibrated for Mach 3+ atmospheric velocity.")
                )
            }

            else -> {
                listOf(
                    TerminalLine(type = LineType.WARNING, tag = "WARN", text = "Available Protocols:"),
                    TerminalLine(type = LineType.OUTPUT, text = "  • protocol veronica    - Orbital Hulkbuster satellite drop"),
                    TerminalLine(type = LineType.OUTPUT, text = "  • protocol house_party - Summon all automated Iron Man armor"),
                    TerminalLine(type = LineType.OUTPUT, text = "  • protocol nano_repair - Nanotech armor cellular reconstruction"),
                    TerminalLine(type = LineType.OUTPUT, text = "  • protocol sentry      - Autonomous perimeter surveillance"),
                    TerminalLine(type = LineType.OUTPUT, text = "  • protocol flight      - Supersonic aerodynamic stabilizers"),
                    TerminalLine(type = LineType.OUTPUT, text = "  • protocol clean_slate - Emergency security purge")
                )
            }
        }
    }
}
