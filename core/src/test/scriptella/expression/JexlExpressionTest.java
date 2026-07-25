/*
 * Copyright 2006-2026 The Scriptella Project Team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package scriptella.expression;

import scriptella.AbstractTestCase;
import scriptella.spi.MockParametersCallbacks;
import scriptella.spi.ParametersCallback;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Characterizes the JEXL expression behavior that Scriptella exposes.
 */
public class JexlExpressionTest extends AbstractTestCase {
    public void testMissingAndNullVariables() {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("nullValue", null);
        ParametersCallback callback = MockParametersCallbacks.fromMap(parameters);

        assertNull(Expression.compile("missing").evaluate(callback));
        assertNull(Expression.compile("nullValue").evaluate(callback));
        assertEquals("fallback", Expression.compile("text:ifNull(missing, 'fallback')").evaluate(callback));
    }

    public void testArithmeticComparisonConcatenationAndDivision() {
        ParametersCallback callback = MockParametersCallbacks.fromMap(Collections.<String, Object>emptyMap());

        assertEquals(7, ((Number) Expression.compile("1 + 2 * 3").evaluate(callback)).intValue());
        assertEquals(Boolean.TRUE, Expression.compile("'10' == 10").evaluate(callback));
        assertEquals("value=10", Expression.compile("'value=' + 10").evaluate(callback));
        assertEquals(2, ((Number) Expression.compile("5 / 2").evaluate(callback)).intValue());
    }

    public void testExternalCallbackMethod() {
        ParametersCallback callback = MockParametersCallbacks.fromMap(
                Collections.<String, Object>singletonMap("callback", new Callback()));

        assertEquals("received:value", Expression.compile("callback.accept('value')").evaluate(callback));
    }

    public void testSharedEngineSupportsConcurrentEvaluation() throws Exception {
        final int threadCount = 8;
        final int iterations = 100;
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(threadCount);
        final AtomicReference<Throwable> failure = new AtomicReference<Throwable>();

        for (int thread = 0; thread < threadCount; thread++) {
            final int value = thread;
            new Thread(new Runnable() {
                public void run() {
                    try {
                        start.await();
                        ParametersCallback callback = MockParametersCallbacks.fromMap(
                                Collections.<String, Object>singletonMap("value", value));
                        for (int i = 0; i < iterations; i++) {
                            Number result = (Number) Expression.compile("value * 2 + 1").evaluate(callback);
                            assertEquals(value * 2 + 1, result.intValue());
                        }
                    } catch (Throwable e) {
                        failure.compareAndSet(null, e);
                    } finally {
                        done.countDown();
                    }
                }
            }, "jexl-expression-" + thread).start();
        }

        start.countDown();
        done.await();
        if (failure.get() != null) {
            throw new AssertionError("Concurrent JEXL evaluation failed", failure.get());
        }
    }

    public static final class Callback {
        public String accept(String value) {
            return "received:" + value;
        }
    }
}
