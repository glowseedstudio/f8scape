package com.f8scape.diagnostics

import com.f8scape.util.TimeUtils

object DiagnosticExporter {
    fun formatMarkdown(report: DiagnosticsReport): String {
        val sb = StringBuilder()
        sb.appendLine("# F8scape Hardware Diagnostic Report")
        sb.appendLine("**Generated:** ${TimeUtils.formatIsoTimestamp(report.timestampMs)}")
        sb.appendLine()

        sb.appendLine("## 1. Device Hardware & Build")
        sb.appendLine("- **Manufacturer:** ${report.hardware.manufacturer}")
        sb.appendLine("- **Model:** ${report.hardware.model}")
        sb.appendLine("- **Device Name:** ${report.hardware.device}")
        sb.appendLine("- **Android Version:** ${report.hardware.androidRelease} (API ${report.hardware.sdkVersion})")
        sb.appendLine("- **Standard Fold 8 Candidate:** ${if (report.hardware.isStandardFold8Candidate) "CONFIRMED / YES" else "UNVERIFIED"}")
        sb.appendLine("- **Build Board:** ${report.hardware.board}")
        sb.appendLine()

        sb.appendLine("## 2. Display Configuration")
        sb.appendLine("- **Active Display Count:** ${report.displays.activeDisplayCount}")
        sb.appendLine("- **Hosting Display ID:** ${report.displays.currentActivityDisplayId}")
        sb.appendLine("- **Cover Screen Active:** ${if (report.isCoverScreenActive) "YES" else "NO"}")
        sb.appendLine()
        sb.appendLine("| Display ID | Name | Resolution | Density | Refresh Rate | State | Cover Candidate |")
        sb.appendLine("|---|---|---|---|---|---|---|")
        report.displays.items.forEach { d ->
            sb.appendLine("| ${d.displayId} | ${d.name} | ${d.widthPx}x${d.heightPx} | ${d.densityDpi} dpi | ${d.refreshRateHz} Hz | ${d.state} | ${if (d.isCoverCandidate) "YES" else "NO"} |")
        }
        sb.appendLine()

        sb.appendLine("## 3. Posture & Sensor Live State")
        sb.appendLine("- **Inferred Posture:** ${report.currentPosture}")
        sb.appendLine("- **Stability Confidence:** ${"%.2f".format(report.stabilityScore * 100)}%")
        sb.appendLine("- **Live Hinge Angle:** ${report.liveHingeAngleDeg?.let { "%.1f°".format(it) } ?: "Not reported / N/A"}")
        sb.appendLine()

        sb.appendLine("## 4. Automation & Capabilities")
        sb.appendLine("- **Accessibility Service Connected:** ${report.isAccessibilityServiceConnected}")
        sb.appendLine("- **Overlay Permission Granted:** ${report.isOverlayPermissionGranted}")
        sb.appendLine("- **Shizuku Installed:** ${report.shizukuStatus.isInstalled}")
        sb.appendLine("- **Shizuku Running:** ${report.shizukuStatus.isRunning}")
        sb.appendLine("- **Shizuku Authorized:** ${report.shizukuStatus.isAuthorized}")
        sb.appendLine("- **Shizuku Details:** ${report.shizukuStatus.details}")
        sb.appendLine()

        sb.appendLine("## 5. Detected Sensors (${report.sensors.size} total)")
        sb.appendLine("| Type | Name | Vendor | Resolution | Max Range |")
        sb.appendLine("|---|---|---|---|---|")
        report.sensors.forEach { s ->
            sb.appendLine("| ${s.typeString} | ${s.name} | ${s.vendor} | ${s.resolution} | ${s.maximumRange} |")
        }
        sb.appendLine()

        sb.appendLine("## 6. Recent Local Diagnostic Logs")
        sb.appendLine("```text")
        if (report.recentLogs.isEmpty()) {
            sb.appendLine("(No entries recorded yet)")
        } else {
            report.recentLogs.forEach { log -> sb.appendLine(log) }
        }
        sb.appendLine("```")

        return sb.toString()
    }
}
