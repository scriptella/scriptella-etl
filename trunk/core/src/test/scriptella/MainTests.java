/*
 * Copyright 2006 The Scriptella Project Team.
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
package scriptella;

import junit.framework.Test;
import junit.framework.TestSuite;
import scriptella.configuration.XIncludeTest;
import scriptella.core.SQLParserBaseTest;
import scriptella.execution.ExecutionStatisticsTest;
import scriptella.expressions.PropertiesSubstitutorTest;


/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class MainTests {
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTestSuite(DBTableCopyTest.class);
        suite.addTestSuite(DialectsTest.class);
        suite.addTestSuite(FilePropertiesTest.class);
        suite.addTestSuite(NestedQueryTest.class);
        suite.addTestSuite(PropertiesTest.class);
        suite.addTestSuite(TxTest.class);
        suite.addTestSuite(XIncludeTest.class);
        suite.addTestSuite(SQLParametersTest.class);
        suite.addTestSuite(SQLParserBaseTest.class);
        suite.addTestSuite(ExecutionStatisticsTest.class);
        suite.addTestSuite(JDBCEscapingTest.class);
        suite.addTestSuite(ConditionsTest.class);
        suite.addTestSuite(PropertiesSubstitutorTest.class);
        suite.addTestSuite(ExecutionStatisticsTest.class);

        return suite;
    }
}
