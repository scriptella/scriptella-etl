/*
 * Copyright 2006-2007 The Scriptella Project Team.
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
package scriptella.util;

import scriptella.AbstractTestCase;

import java.sql.SQLException;

/**
 * Tests for {@link ExceptionUtils}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ExceptionUtilsTest extends AbstractTestCase {
    public void testGetCause() {
        SQLException e = new SQLException();
        SQLException e2 = new SQLException("2");
        e.setNextException(e2);
        assertTrue(e2==ExceptionUtils.getCause(e));
        assertNull(ExceptionUtils.getCause(new Throwable()));
    }

    public void testThrowUnchecked() {
        Error er = new Error();
        try {
            ExceptionUtils.throwUnchecked(er);
            fail("Error must be rethrown");
        } catch (Error e) {
            assertTrue(er==e);
        }
        RuntimeException re = new RuntimeException();
        try {
            ExceptionUtils.throwUnchecked(re);
            fail("Runtime ex must be rethrown");
        } catch (RuntimeException e) {
            assertTrue(re==e);
        }
        try {
            ExceptionUtils.throwUnchecked(new Exception());
            fail("Runtime ex must be thrown");
        } catch (IllegalStateException e) {
        }


    }

}
