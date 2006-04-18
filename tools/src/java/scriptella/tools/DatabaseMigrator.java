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
package scriptella.tools;

import scriptella.configuration.ConnectionEl;
import scriptella.sql.ConnectionFactory;
import scriptella.sql.JDBCException;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;


/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class DatabaseMigrator {
    public static void main(final String args[]) {
        ConnectionEl c = new ConnectionEl();
        c.setDriver("org.hsqldb.jdbcDriver");
        c.setUrl("jdbc:hsqldb:file:D:/tools/hsqldb/data/dbm");
        c.setUser("sa");

        final ConnectionFactory con = new ConnectionFactory(c);
        final Set<String> tables = sortTables(con);
        StringBuilder o = new StringBuilder();
        o.append("<sql-scripts version=\"1\">\n" +
                "    <connection id=\"in\" driver=\"com.sybase.jdbc2.jdbc.SybDriver\" url=\"jdbc:sybase:Tds:localhost:2638\" user=\"DBA\" password=\"SQL\"/>\n" +
                "    <connection id=\"out\" driver=\"org.hsqldb.jdbcDriver\" url=\"jdbc:hsqldb:file:D:/tools/hsqldb/data/dbm\" user=\"sa\" password=\"\"/>\n");

        for (String t : tables) {
            o.append("    <query id=\"").append("in.").append(t)
                    .append("\" type=\"sql\" connection-id=\"in\">\n")
                    .append("      select ");
            appendColumnNames(con, t, o).append(" from ").append(t)
                    .append("\n    </datasource>\n");

            o.append("    <script query-id=\"").append("in.").append(t)
                    .append("\" type=\"sql\" connection-id=\"out\">\n")
                    .append("      insert into ").append(t).append("(");
            appendColumnNames(con, t, o).append(") VALUES (");
            appendColumnNames(con, t, o, ", ", "$").append(")")
                    .append("\n    </script>\n");
        }

        o.append("</sql-scripts>\n");
        System.out.println("o = " + o);

        //        JDBCUtils.getTableColumns(con, c,)
    }

    private static StringBuilder appendColumnNames(
            final ConnectionFactory con, final String table, final StringBuilder sql) {
        return appendColumnNames(con, table, sql, ", ", "");
    }

    private static StringBuilder appendColumnNames(
            final ConnectionFactory con, final String table,
            final StringBuilder sql, final String separator, final String prefix) {
        final Set<String> tableColumns = con.getTableColumns(table);

        for (Iterator<String> it = tableColumns.iterator(); it.hasNext();) {
            String s = it.next();
            sql.append(prefix);
            sql.append(s);

            if (it.hasNext()) {
                sql.append(separator);
            }
        }

        return sql;
    }

    private static Set<String> sortTables(final ConnectionFactory con) {
        List<String> tables = con.getTables();
        System.out.println("Sorting " + tables);

        int n = tables.size();
        int m[][] = new int[n][n];

        for (int i = 0; i < m.length; i++) {
            Arrays.fill(m[i], 0);
        }

        String tbls[] = tables.toArray(new String[n]);

        try {
            final DatabaseMetaData metaData = con.getConnection().getMetaData();

            for (int i = 0; i < n; i++) {
                String t = tbls[i];
                final ResultSet rs = metaData.getExportedKeys(con.getCatalog(),
                        con.getSchema(), t);

                while (rs.next()) {
                    //                    if (rs.getInt(11) != 2) { //Only not null FKs are taken into account
                    String t2 = rs.getString(7);
                    int i2 = indexOf(tbls, t2);

                    if (i2 >= 0) {
                        m[i][i2] += ((rs.getInt(11) != 2) ? 10 : 1);
                    }

                    //                    }
                }

                rs.close();
            }

            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    System.out.print(m[i][j]);
                    System.out.print((m[i][j] >= 10) ? " " : "  ");
                }

                System.out.println(tbls[i]);
            }

            boolean free[] = new boolean[n];
            Arrays.fill(free, true);

            Set<String> res = new LinkedHashSet<String>();

            for (int i = 0; i < n; i++) {
                //on each i iteration we choose the best candidate (having minimal relationships)
                int min = Integer.MAX_VALUE;
                int minI = -1;

                for (int j = 0; j < n; j++) { //choosing an available candiate

                    int s = 0;

                    if (free[j]) {
                        for (int k = 0; k < n; k++) { //checking incoming relationships

                            if ((k != j) && free[k]) {
                                s += m[k][j];
                            }
                        }

                        if (s < min) {
                            min = s;
                            minI = j;
                        }
                    }
                }

                if (minI >= 0) {
                    free[minI] = false;
                    res.add(tbls[minI]);
                }
            }

            return res;
        } catch (SQLException e) {
            throw new JDBCException(e);
        }
    }

    private static int indexOf(final String list[], final String element) {
        for (int i = 0; i < list.length; i++) {
            if (list[i].equalsIgnoreCase(element)) {
                return i;
            }
        }

        return -1;
    }
}
