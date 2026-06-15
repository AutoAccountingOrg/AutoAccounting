/*
 * Copyright (C) 2026 ankio(ankio@ankio.net)
 * Licensed under the Apache License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-3.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.ankio.tap

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.channels.FileChannel

@RunWith(AndroidJUnit4::class)
class TapTfRuntimeInstrumentedTest {

    @Test
    fun bundledModelsLoadWithLiteRtInterpreter() {
        val assets = InstrumentationRegistry.getInstrumentation().targetContext.assets

        TapBuiltinModel.entries.forEach { model ->
            assets.openFd(model.assetPath).use { descriptor ->
                FileInputStream(descriptor.fileDescriptor).use { stream ->
                    val buffer = stream.channel.map(
                        FileChannel.MapMode.READ_ONLY,
                        descriptor.startOffset,
                        descriptor.declaredLength,
                    )
                    Interpreter(buffer).use { interpreter ->
                        assertTrue("${model.id} has no input tensors", interpreter.inputTensorCount > 0)
                        assertTrue("${model.id} has no output tensors", interpreter.outputTensorCount > 0)
                    }
                }
            }
        }
    }
}
