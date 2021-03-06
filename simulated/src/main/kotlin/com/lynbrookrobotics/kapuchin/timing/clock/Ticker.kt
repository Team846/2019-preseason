package com.lynbrookrobotics.kapuchin.timing.clock

import com.lynbrookrobotics.kapuchin.logging.Named
import com.lynbrookrobotics.kapuchin.timing.Priority
import com.lynbrookrobotics.kapuchin.timing.currentTime
import info.kunalsheth.units.generated.Second
import info.kunalsheth.units.generated.Time
import info.kunalsheth.units.math.milli
import info.kunalsheth.units.math.nano
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.system.measureNanoTime

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

    init {
        exec.scheduleAtFixedRate(
                {
                    computeTime = measureNanoTime {
                        try {
                            tick(currentTime)
                        } catch (e: Throwable) {
                            e.printStackTrace()
                            throw e
                        }
                    }.nano(Second)
                },
                100, period.milli(Second).toLong(), TimeUnit.MILLISECONDS
        )
    }

    actual fun waitOnTick(): Time {
        // todo do this correctly
        Thread.sleep(period.milli(Second).toLong())
        return currentTime
    }

    actual companion object {
        actual fun Named.ticker(priority: Priority, period: Time, name: String) = com.lynbrookrobotics.kapuchin.timing.clock.Ticker(this, priority, period, name)
        private val exec = Executors.newSingleThreadScheduledExecutor()!!
    }
}