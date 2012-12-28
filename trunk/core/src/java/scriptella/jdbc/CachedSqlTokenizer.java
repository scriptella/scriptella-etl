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
package scriptella.jdbc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This tokenizer uses target to tokenize statements and remembers
 * parsed statements and injections, so it can be reset and iterated several times.
 * Call {@link #close()} to reset tokenizer.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
final class CachedSqlTokenizer implements SqlTokenizer {
    private SqlTokenizer target;
    private List<String> statements;
    private String[] statementsArray; //For better performance
    private List<int[]> injections;
    private int[][] injectionsArray; //For better performance
    private int index=-1;


    /**
     * Instantiates tokenizer based on the specified target.
     * @param target target to use. The statements are cached lazily as they
     * are obtained by calling {@link #nextStatement()}.
     */
    public CachedSqlTokenizer(SqlTokenizer target) {
        this.target = target;
    }

    public String nextStatement() throws IOException {
        if (target != null) {
            String st = target.nextStatement();
            if (st != null) {
                if (statements == null) {
                    statements = new ArrayList<String>();
                    injections = new ArrayList<int[]>();
                }
                statements.add(st);
            }
            return st;
        } else {
            index++;
            if (statementsArray == null || index >= statementsArray.length) {
                return null;
            }
            return statementsArray[index];
        }
    }

    /**
     * This method returns list of injections for the last returned statement.
     *
     * @return injections for the last returned statement.
     */
    public int[] getInjections() {
        if (target != null) {
            int[] inj = target.getInjections();
            injections.add(inj);
            return inj;
        } else {
            if (injectionsArray != null && index<injectionsArray.length) {
                return injectionsArray[index];
            }
            return null;
        }
    }

    /**
     * Important notes:
     * <ul>
     * <li>calling close first time closes target tokenizer,
     * subsequent close invocations reset index to start iteration.
     * <li>After close is called for the first time, unread statements
     * are skipped and not returned in subsequent iterations.
     * </ul>
     *
     */
    public void close() throws IOException {
        if (target != null) {
            if (statements!=null) {
                statementsArray=statements.toArray(new String[statements.size()]);
                injectionsArray=injections.toArray(new int[injections.size()][]);
                statements=null;
                injections=null;
            }
            try {
                target.close();
            } finally {
                target = null; //We do not need target anymore
            }
        } else {
            index = -1;
        }

    }

}
