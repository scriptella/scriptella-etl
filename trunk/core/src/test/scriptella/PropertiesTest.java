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

import scriptella.configuration.ConfigurationEl;
import scriptella.core.ConnectionManager;
import scriptella.core.SqlTestHelper;
import scriptella.execution.ScriptsContext;
import scriptella.execution.ScriptsExecutor;
import scriptella.execution.ScriptsExecutorException;
import scriptella.interactive.ProgressIndicator;
import scriptella.spi.ConnectionParameters;

import java.util.Map;


/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class PropertiesTest extends AbstractTestCase {
    private ScriptsContext ctx; //execution context
    private ConnectionParameters params;

    public void test() throws ScriptsExecutorException {
        final ScriptsExecutor se = newScriptsExecutor("PropertiesTest.xml");
        se.execute();

        assertEquals("jdbc:hsqldb:mem:propertiestest", params.getUrl());
        assertEquals("sa", params.getUser());
        assertEquals("", params.getPassword());

        //check substituted properties in a context
        assertEquals("1", ctx.getParameter("a"));
        assertEquals("bar", ctx.getParameter("foo"));
        assertEquals("1", ctx.getParameter("var"));
        assertEquals("1|1|1|1|1|1", ctx.getParameter("b"));
        assertEquals("jdbc:hsqldb:mem", ctx.getParameter("url.prefix"));
        assertEquals("propertiestest", ctx.getParameter("dbname"));
        assertEquals("org.hsqldb.jdbcDriver", ctx.getParameter("driver"));
        assertEquals("org.hsqldb.jdbcDriver", ctx.getParameter("driver"));
        assertEquals("jdbc:hsqldb:mem:propertiestest", ctx.getParameter("url"));
        assertEquals("sa", ctx.getParameter("user"));
        assertEquals("", ctx.getParameter("password"));
    }

    @Override
    protected ScriptsExecutor newScriptsExecutor(
            final ConfigurationEl configuration) {
        return new ScriptsExecutor(configuration) {
            //overrides prepare method to get ctx and params for connection
            protected ScriptsContext prepare(
                    final ProgressIndicator indicator) {
                ctx = super.prepare(indicator); //store ctx for assertions
                Map<String, ConnectionManager> connections = SqlTestHelper.getConnections(ctx.getSession());
                ConnectionManager con = connections.entrySet().iterator().next().getValue();
                params = SqlTestHelper.getConnectionParameters(con);
                return ctx;
            }
        };
    }
}
