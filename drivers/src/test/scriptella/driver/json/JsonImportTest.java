/*
 * Copyright 2026 The Scriptella Project Team.
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
package scriptella.driver.json;

import scriptella.AbstractTestCase;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;

/** Exercises driver discovery, expressions and JDBC typed binding together. */
public class JsonImportTest extends AbstractTestCase {
    public void testImport() throws Exception {
        Class.forName("org.h2.Driver");
        try (Connection db = DriverManager.getConnection("jdbc:h2:mem:json_phase2", "sa", "")) {
            newEtlExecutor().execute();
            try (ResultSet rows = db.createStatement().executeQuery("SELECT * FROM CUSTOMER ORDER BY ID")) {
                assertTrue(rows.next());
                assertEquals(1, rows.getInt("ID"));
                assertEquals("Ada \"Ace\"", rows.getString("CUSTOMER_NAME"));
                assertTrue(rows.getBoolean("ACTIVE"));
                assertEquals(new BigDecimal("12.50"), rows.getBigDecimal("FIRST_PRICE"));
                assertEquals("A-1", rows.getString("EXTERNAL_CODE"));
                assertNull(rows.getString("NOTE"));
                assertNull(rows.getString("ABSENT"));
                assertEquals(new BigDecimal("9223372036854775808"), rows.getBigDecimal("LARGE"));
                assertTrue(rows.next());
                assertEquals(2, rows.getInt("ID"));
                assertEquals("Lin", rows.getString("CUSTOMER_NAME"));
                assertFalse(rows.getBoolean("ACTIVE"));
                assertEquals(new BigDecimal("7.25"), rows.getBigDecimal("FIRST_PRICE"));
                assertEquals("B-2", rows.getString("EXTERNAL_CODE"));
                assertEquals("follow up", rows.getString("NOTE"));
                assertFalse(rows.next());
            }
        }
    }
}
