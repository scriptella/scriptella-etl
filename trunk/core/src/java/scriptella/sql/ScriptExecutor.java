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
package scriptella.sql;

import scriptella.configuration.ContentEl;
import scriptella.configuration.Location;
import scriptella.configuration.ScriptEl;

import java.sql.Connection;
import java.util.logging.Logger;


/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ScriptExecutor implements SQLExecutableElement {
    private static final Logger LOG = Logger.getLogger(ScriptExecutor.class.getName());
    private ScriptEl scriptEl;

    public ScriptExecutor(ScriptEl scriptEl) {
        this.scriptEl = scriptEl;
    }

    public void execute(final SQLContext ctx) {
        Connection c = ctx.getConnection();
        final Script script = convert(scriptEl, ctx);

        if (script != null) {
            script.execute(c, ctx);
        } else {
            LOG.info("Script " + scriptEl.getLocation() +
                    " has no supported dialects");
        }
    }

    private Script convert(final ScriptEl s, final SQLContext ctx) {
        final ContentEl content = s.getContent(ctx.getDialectIdentifier());

        if (content == null) {
            return null;
        } else {
            return new Script(content);
        }
    }

    public Location getLocation() {
        return scriptEl.getLocation();
    }

    public static SQLExecutableElement prepare(final ScriptEl s) {
        SQLExecutableElement se = new ScriptExecutor(s);
        se = StatisticInterceptor.prepare(se, s.getLocation());
        se = TxInterceptor.prepare(se, s);
        se = ConnectionInterceptor.prepare(se, s);
        se = ExceptionInterceptor.prepare(se, s.getLocation());
        se = IfInterceptor.prepare(se, s);

        return se;
    }
}
