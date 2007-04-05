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
package scriptella.tools.template;

import scriptella.jdbc.GenericDriver;
import scriptella.jdbc.JdbcException;
import scriptella.spi.ConnectionParameters;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class DataMigrator extends TemplateManager {

    public static void main(final String args[]) {
        //Currently this class is just a sample of how to generate a migration script template
        //Rewite it

        ConnectionParameters params = new ConnectionParameters(null,null);
        GenericDriver jdbcDriver = new GenericDriver();


        final Connection con = jdbcDriver.connect(params).getNativeConnection();
        final Set<String> tables = sortTables(con, params);
        StringBuilder o = new StringBuilder();
        o.append("<etl>\n" +
                "    <connection id=\"in\" driver=\"com.sybase.jdbc2.jdbc.SybDriver\" url=\"jdbc:sybase:Tds:localhost:2638\" user=\"DBA\" password=\"SQL\"/>\n" +
                "    <connection id=\"out\" driver=\"org.hsqldb.jdbcDriver\" url=\"jdbc:hsqldb:file:D:/tools/hsqldb/data/dbm\" user=\"sa\" password=\"\"/>\n");

        for (String t : tables) {
            o.append("    <query connection-id=\"in\">\n")
                    .append("      select ");
            appendColumnNames(con, params, t, o).append(" from ").append(t);

            o.append("      <script connection-id=\"out\">\n")
                    .append("         insert into ").append(t).append("(");
            appendColumnNames(con, params, t, o).append(") VALUES (");
            appendColumnNames(con, params, t, o, ", ", "?").append(")")
                    .append("\n      </script>\n").
            append("\n    </query>\n");
        }

        o.append("</etl>\n");
        System.out.println("o = " + o);

        //        JdbcUtils.getTableColumns(con, c,)
    }

    private static StringBuilder appendColumnNames(
            final Connection con, ConnectionParameters params, final String table, final StringBuilder sql) {
        return appendColumnNames(con, params, table, sql, ", ", "");
    }

    private static StringBuilder appendColumnNames(
            final Connection con, ConnectionParameters params, final String table,
            final StringBuilder sql, final String separator, final String prefix) {
        final Set<String> tableColumns = getTableColumns(con, params, table);

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

    private static Set<String> sortTables(final Connection con, ConnectionParameters params) {
        List<String> tables = getTables(con, params);
        System.out.println("Sorting " + tables);

        int n = tables.size();
        int m[][] = new int[n][n];

        for (int i = 0; i < m.length; i++) {
            Arrays.fill(m[i], 0);
        }

        String tbls[] = tables.toArray(new String[n]);

        try {
            final DatabaseMetaData metaData = con.getMetaData();

            for (int i = 0; i < n; i++) {
                String t = tbls[i];
                final ResultSet rs = metaData.getExportedKeys(params.getCatalog(),
                        params.getSchema(), t);

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

                for (int j = 0; j < n; j++) { //choosing an available candidate

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
            throw new JdbcException(e.getMessage(), e);
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

    private static List<String> getTables(Connection con, ConnectionParameters params) {
        try {
            return getColumn(con.getMetaData()
                    .getTables(con.getCatalog(),
                    params.getSchema(), null, new String[]{"TABLE"}), 3);
        } catch (SQLException e) {
            throw new JdbcException(e.getMessage(), e);
        }
    }

    private static Set<String> getTableColumns(Connection con, ConnectionParameters params, final String tableName) {
        try {
            return new HashSet<String>(getColumn(
                    con.getMetaData()
                            .getColumns(params.getCatalog(), params.getSchema(), tableName, null),
                    4));
        } catch (SQLException e) {
            throw new JdbcException(e.getMessage(), e);
        }
    }

    /**
     * Iterates through the resultset and returns column values.
     * @param rs resultset to iterate.
     * @param columnPos column position. Starts at 1.
     * @return list of column values.
     */
    public static List<String> getColumn(final ResultSet rs, final int columnPos) {
        List<String> l = new ArrayList<String>();

        try {
            while (rs.next()) {
                l.add(rs.getString(columnPos));
            }
        } catch (SQLException e) {
            throw new JdbcException("Unable to get column #" + columnPos, e);
        }

        return l;
    }

}
