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
package scriptella.jdbc;

import scriptella.spi.ParametersCallback;

import java.util.ArrayList;
import java.util.List;

/** Immutable syntax for a statement containing only simple named JDBC parameters. */
final class SqlBindingPlan {
    private final String sql;
    private final String[] names;

    private SqlBindingPlan(String sql, String[] names) {
        this.sql = sql;
        this.names = names;
    }

    /** Uses the existing parser and tokenizer offsets; null means use the normal parser. */
    static SqlBindingPlan compile(String sql, int[] injections) {
        if (injections == null || injections.length == 0) {
            return null;
        }
        Compiler compiler = new Compiler();
        compiler.handleStatement(sql, injections);
        // Unrecognized markers and markers nested in expressions are not eligible.
        if (!compiler.eligible || compiler.names.size() != injections.length) {
            return null;
        }
        return new SqlBindingPlan(compiler.sql,
                compiler.names.toArray(new String[compiler.names.size()]));
    }

    String getSql() {
        return sql;
    }

    void bind(ParametersCallback callback, List<Object> parameters) {
        for (String name : names) {
            parameters.add(callback.getParameter(name));
        }
    }

    private static final class Compiler extends SqlParserBase {
        private final List<String> names = new ArrayList<String>();
        private boolean eligible = true;
        private String sql;

        @Override
        protected String handleParameter(String name, boolean expression, boolean jdbcParam) {
            if (expression || !jdbcParam) {
                eligible = false;
            }
            names.add(name);
            return "?";
        }

        @Override
        protected void statementParsed(String sql) {
            this.sql = sql;
        }
    }
}
