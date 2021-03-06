package com.lynbrookrobotics.kapuchin.logging

import com.lynbrookrobotics.kapuchin.timing.scope
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard
import info.kunalsheth.units.generated.Quan
import info.kunalsheth.units.generated.Second
import info.kunalsheth.units.generated.Time
import info.kunalsheth.units.generated.UomConverter
import kotlinx.coroutines.launch
import java.io.Closeable
import java.io.File
import java.io.Flushable

actual fun printAtLevel(level: Level, formattedMessage: String) = when (level) {
    Level.Error -> println("ERROR $formattedMessage")
    Level.Warning -> println("WARNING $formattedMessage")
    Level.Debug -> println("DEBUG $formattedMessage")
}

actual class Grapher<Q : Quan<Q>> private actual constructor(parent: Named, of: String, private val withUnits: UomConverter<Q>) :
        Named by Named("$of (${withUnits.unitName})", parent),
        Flushable, Closeable,
        (Time, Q) -> Unit {

    private var running = scope.launch { }
    private val safeName = name.replace("""[^\w\d]""".toRegex(), "_")
    private val printer = File("/tmp/$safeName.csv")
            .printWriter(Charsets.US_ASCII).also { it.println("seconds,${withUnits.unitName}") }

    actual override fun invoke(x: Time, y: Q) {
        if (running.isCompleted) scope.launch {
            SmartDashboard.putNumber(name, withUnits(y))
            printer.println("${x.Second},${withUnits(y)}")
            printer.flush()
        }.also { running = it }
    }

    actual override fun flush() = printer.flush()
    actual override fun close() = printer.close()

    actual companion object {
        actual fun <Q : Quan<Q>> Named.graph(of: String, withUnits: UomConverter<Q>) =
                Grapher(this, of, withUnits)
    }
}

actual typealias StackTraceElement = java.lang.StackTraceElement

actual val Throwable.platformStackTrace: Array<StackTraceElement> get() = stackTrace

actual fun nameLayer(parent: Named?, child: String): String = "${parent?.name?.plus('/') ?: ""}$child"