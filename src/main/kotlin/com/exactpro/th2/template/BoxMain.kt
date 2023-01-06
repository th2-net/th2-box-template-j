/*
 * Copyright 2023 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:JvmName("BoxMain")

package com.exactpro.th2.template

import com.exactpro.th2.common.event.Event
import com.exactpro.th2.common.event.EventUtils.createMessageBean
import com.exactpro.th2.common.metrics.registerLiveness
import com.exactpro.th2.common.metrics.registerReadiness
import com.exactpro.th2.common.schema.factory.CommonFactory
import com.exactpro.th2.common.schema.factory.extensions.getCustomConfiguration
import mu.KotlinLogging
import java.util.Deque
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.system.exitProcess

private val LOGGER = KotlinLogging.logger { }

@Suppress("SpellCheckingInspection")
private val LIVENESS = registerLiveness("main")
private val READINESS = registerReadiness("main")

private const val LOAD_DICTIONATY_EVENT = "Load dictionaries"

fun main(args: Array<String>) {
    LOGGER.info { "Starting the box" }
    // Here is an entry point to the th2-box.

    // Configure shutdown hook for closing all resources
    // and the lock condition to await termination.
    //
    // If you use the logic that doesn't require additional threads,
    // and you can run everything on main thread
    // you can omit the part with locks (but please keep the resources queue)
    val resources: Deque<AutoCloseable> = ConcurrentLinkedDeque()
    val lock = ReentrantLock()
    val condition: Condition = lock.newCondition()
    configureShutdownHook(resources, lock, condition)

    try {
        // You need to initialize the CommonFactory

        // You can use custom paths to each config that is required for the CommonFactory
        // If args are empty the default path will be chosen.
        val factory = CommonFactory.createFromArguments(*args)
        // do not forget to add resource to the resources queue
        resources += factory

        // The BOX is alive
        LIVENESS.enable()

        val eventRouter = factory.eventBatchRouter
        eventRouter.sendAll(
            Event.start()
                .bodyData(createMessageBean("I am a template th2-box"))
                .toBatchProto(factory.rootEventId)
        )

        // Loading dictionnaies example
        val loadDictionaryEvent = Event.start()
            .name("Load dictionaries")
            .type(LOAD_DICTIONATY_EVENT)
        val configuration = factory.getCustomConfiguration<Configuration>()

        factory.loadDictionary(configuration.dictinary).close()
        loadDictionaryEvent.addSubEventWithSamePeriod()
            .name("Loaded single dictionary by ${configuration.dictinary} alias")
            .type(LOAD_DICTIONATY_EVENT)

        val loadDictionarySetEvent = loadDictionaryEvent.addSubEventWithSamePeriod()
            .name("Loaded dictionary set")
            .type(LOAD_DICTIONATY_EVENT)
        configuration.dictionarySet.forEach { alias ->
            factory.loadDictionary(alias).close()
            loadDictionarySetEvent.bodyData(createMessageBean("Loaded dictionary by $alias alias"))
        }

        val loadDictionaryMapEvent = loadDictionaryEvent.addSubEventWithSamePeriod()
            .name("Loaded dictionary map")
            .type(LOAD_DICTIONATY_EVENT)
        configuration.dictionaryMap.forEach { (name, alias) ->
            factory.loadDictionary(alias).close()
            loadDictionaryMapEvent.bodyData(createMessageBean("Loaded $name dictionary by $alias alias"))
        }
        eventRouter.sendAll(loadDictionaryEvent.toBatchProto(factory.rootEventId))

        // Do additional initialization required to your logic

        // The BOX is ready to work
        READINESS.enable()

        awaitShutdown(lock, condition)
    } catch (ex: Exception) {
        LOGGER.error(ex) { "Cannot start the box" }
        exitProcess(1)
    }
}

private fun configureShutdownHook(resources: Deque<AutoCloseable>, lock: ReentrantLock, condition: Condition) {
    Runtime.getRuntime().addShutdownHook(thread(
        start = false,
        name = "Shutdown hook"
    ) {
        LOGGER.info { "Shutdown start" }
        READINESS.disable()
        try {
            lock.lock()
            condition.signalAll()
        } finally {
            lock.unlock()
        }
        resources.descendingIterator().forEachRemaining { resource ->
            try {
                resource.close()
            } catch (e: Exception) {
                LOGGER.error(e) { "Cannot close resource ${resource::class}" }
            }
        }
        LIVENESS.disable()
        LOGGER.info { "Shutdown end" }
    })
}

@Throws(InterruptedException::class)
private fun awaitShutdown(lock: ReentrantLock, condition: Condition) {
    try {
        lock.lock()
        LOGGER.info { "Wait shutdown" }
        condition.await()
        LOGGER.info { "App shutdown" }
    } finally {
        lock.unlock()
    }
}