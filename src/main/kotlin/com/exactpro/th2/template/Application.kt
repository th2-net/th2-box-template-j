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

package com.exactpro.th2.template

import com.exactpro.th2.common.event.Event
import com.exactpro.th2.common.event.EventUtils
import com.exactpro.th2.common.metrics.registerLiveness
import com.exactpro.th2.common.metrics.registerReadiness
import com.exactpro.th2.common.schema.factory.CommonFactory

class Application(
    private val factory: CommonFactory,
): AutoCloseable {
    fun run() {
        // th2 component is alive
        LIVENESS.enable()

        val eventRouter = factory.eventBatchRouter
        eventRouter.sendAll(
            Event.start()
                .bodyData(EventUtils.createMessageBean("I am a template th2 component"))
                .toBatchProto(factory.rootEventId)
        )

        // Do additional initialization required to your logic

        // th2 component is ready to work
        READINESS.enable()
    }

    override fun close() {
        READINESS.disable()
        LIVENESS.disable()
    }

    companion object {
        @Suppress("SpellCheckingInspection")
        private val LIVENESS = registerLiveness("main")
        private val READINESS = registerReadiness("main")
    }
}