/*
 * Copyright 2020-2020 Exactpro (Exactpro Systems Limited)
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
import com.exactpro.th2.common.event.EventUtils
import com.exactpro.th2.common.grpc.EventBatch
import com.exactpro.th2.common.metrics.liveness
import com.exactpro.th2.common.metrics.readiness
import com.exactpro.th2.common.schema.factory.CommonFactory
import mu.KotlinLogging

private val LOGGER = KotlinLogging.logger { }

fun main(args: Array<String>) {
    LOGGER.info { "Starting the box" }
    // Here is an entry point to the th2-box.
    // You need to initialize the CommonFactory

    // You can use custom paths to each config that is required for the CommonFactory
    // If args are empty the default path will be chosen.
    val factory = CommonFactory.createFromArguments(*args)

    // The BOX is alive
    liveness = true
    try {
        val eventRouter = factory.eventBatchRouter
        eventRouter.send(
            EventBatch.newBuilder()
                .addEvents(
                    Event.start().endTimestamp()
                        .bodyData(EventUtils.createMessageBean("I am a template th2-box"))
                        .toProtoEvent(null/* no parent, the root event */)
                )
                .build()
        )
        // The BOX is ready to work
        readiness = true

        // do some work

    } finally {
        readiness = false
        liveness = false
        // need to close resources
        // factory closes all related resources (routers, gRPC, Cradle, RabbitMQ) as well
        factory.close()
    }
}