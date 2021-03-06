package com.lynbrookrobotics.kapuchin.timing.clock

import com.lynbrookrobotics.kapuchin.logging.Level.Warning
import com.lynbrookrobotics.kapuchin.logging.Named
import com.lynbrookrobotics.kapuchin.logging.log
import com.lynbrookrobotics.kapuchin.logging.withDecimals
import com.lynbrookrobotics.kapuchin.timing.PlatformThread.Companion.platformThread
import com.lynbrookrobotics.kapuchin.timing.Priority
import com.lynbrookrobotics.kapuchin.timing.currentTime
import edu.wpi.first.wpilibj.hal.NotifierJNI
import info.kunalsheth.units.generated.Each
import info.kunalsheth.units.generated.Second
import info.kunalsheth.units.generated.Time
import info.kunalsheth.units.generated.div
import info.kunalsheth.units.math.micro

actual class Ticker private actual constructor(
        parent: Named,
        priority: Priority,
        val period: Time,
        name: String
) :
        Named by Named(name, parent),
        Clock {

    actual var computeTime = 0.Second
        private set

    override var jobs: List<(tickStart: Time) -> Unit> = emptyList()

    actual fun waitOnTick(): Time {
        updateAlarm()
        Thread.yield()
        return NotifierJNI.waitForNotifierAlarm(notifierHandle).micro(Second)
    }

    private val notifierHandle = NotifierJNI.initializeNotifier()
    private val startTime = currentTime
    private var periodIndex = -1L
    private fun updateAlarm() {
        val dt = currentTime - startTime
        val nextPeriodIndex = (dt / period).Each.toLong() + 1

        if (nextPeriodIndex != periodIndex) NotifierJNI.updateNotifierAlarm(
                notifierHandle,
                (startTime + period * nextPeriodIndex).micro(Second).toLong()
        )

        periodIndex = nextPeriodIndex
    }

    private val thread = platformThread(name, priority) {
        while (true) {
            val startTime = waitOnTick()
            tick(startTime)
            computeTime = currentTime - startTime

            if (computeTime > period) log(Warning) {
                "$name overran its ${period withDecimals 4} loop by ${(computeTime - period) withDecimals 4}"
            }
        }
    }

    actual companion object {
        actual fun Named.ticker(priority: Priority, period: Time, name: String) = Ticker(this, priority, period, name)
    }
}