/*
 * Copyright 2006-2012 The Scriptella Project Team.
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

/**
 * Performance test for {@link ColumnsMap}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ColumnsMapPerfTest extends AbstractTestCase {
    private ColumnsMap cm;
    private static final int SEARCH_LOOP_COUNT = 300000;
    private static final String[] colNames=new String[400];
    private static final String[] colIndeces=new String[colNames.length];
    static {
        for (int i=0;i<colNames.length;i++) {
            colNames[i]="col"+i;
            colIndeces[i]=String.valueOf(i);
        }
    }

    protected void setUp() throws Exception {
        super.setUp();
        cm=new ColumnsMap();
        for (int i=1;i<200;i++) {
            cm.registerColumn(colNames[i].toUpperCase(),i);
        }
    }

    /**
     * History:
     * 06.09.2006 - Duron 1.7Mhz - 1046 ms
     * 11.09.2006 - Duron 1.7Mhz - 422 ms
     */
    public void testNamedSearch() {
        for (int i=1;i<SEARCH_LOOP_COUNT;i++) {
            cm.find(colNames[i%250]); //20% misses
        }
    }

    /**
     * History:
     * 06.09.2006 - Duron 1.7Mhz - 563 ms
     * 11.09.2006 - Duron 1.7Mhz - 25 ms
     */
    public void testIndexedSearch() {
        for (int i=1;i<SEARCH_LOOP_COUNT;i++) {
            cm.find(colIndeces[i%250]); //20% misses
        }
    }


    /**
     * History:
     * 06.09.2006 - Duron 1.7Mhz - 891 ms
     * 11.09.2006 - Duron 1.7Mhz - 359 ms
     */
    public void testFill() {
        for (int k=1;k<700;k++) {
            for (int i=1;i<colNames.length;i++) {
                cm.registerColumn(colNames[i],i);
            }
        }
    }
}
