/*
 * Copyright (C) 2024 ankio(ankio@ankio.net)
 * Licensed under the Apache License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-3.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.ezbook.server.server

import fi.iki.elonen.NanoHTTPD.AsyncRunner
import fi.iki.elonen.NanoHTTPD.ClientHandler
import java.util.Collections
import java.util.concurrent.ExecutorService


internal class BoundRunner(private val executorService: ExecutorService) : AsyncRunner {
    private val running: MutableList<ClientHandler> = Collections.synchronizedList(ArrayList())

    override fun closeAll() {
        // copy of the list for concurrency
        for (clientHandler in ArrayList(this.running)) {
            clientHandler.close()
        }
    }

    override fun closed(clientHandler: ClientHandler) {
        running.remove(clientHandler)
    }

    override fun exec(clientHandler: ClientHandler) {
        executorService.submit(clientHandler)
        running.add(clientHandler)
    }
}