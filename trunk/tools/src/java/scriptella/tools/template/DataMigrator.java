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

import scriptella.core.SystemException;
import scriptella.expression.PropertiesSubstitutor;
import scriptella.jdbc.JdbcException;
import scriptella.jdbc.JdbcUtils;

import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data migration template manager.
 * <p>Configuration properties:
 * <ul>
 * <li>driver - JDBC driver class name. Required.
 * <li>url - JDBC URL of the database. Required.
 * <li>user - database user name.
 * <li>password - database user password.
 * <li>catalog - database catalog name.
 * <li>catalog - database schema name.
 * </ul>
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class DataMigrator extends TemplateManager {
    static final String DRIVER_PROPERTY_NAME = "driver";
    static final String URL_PROPERTY_NAME = "url";
    static final String USER_PROPERTY_NAME = "user";
    static final String PASSWORD_PROPERTY_NAME = "password";
    static final String CATALOG_PROPERTY_NAME = "catalog";
    static final String SCHEMA_PROPERTY_NAME = "schema";

    private static final String DATA_MIGRATOR_ETL_XML = "dataMigrator.etl.xml";
    private static final String DATA_MIGRATOR_ETL_PROPERTIES = "dataMigrator.etl.properties";
    private static final String DATA_MIGRATOR_BLOCK_ETL_XML = "dataMigratorBlock.etl.xml";
    private static Logger LOG = Logger.getLogger(DataMigrator.class.getName());
    static boolean DEBUG = LOG.isLoggable(Level.FINE);


    public void create(Map<String, ?> properties) throws IOException {
        String baseName = defineName();
        String xmlName = baseName + XML_EXT;
        String propsName = baseName + PROPS_EXT;

        String etlXml = loadResourceAsString(DATA_MIGRATOR_ETL_XML);
        String block = loadResourceAsString(DATA_MIGRATOR_BLOCK_ETL_XML);
        DbSchema schema = DbSchema.initialize(properties);
        final Set<String> tables = sortTables(schema);
        StringBuilder queriesXml = new StringBuilder(block.length() * tables.size());
        final Map<String, String> params = new HashMap<String, String>();
        final PropertiesSubstitutor ps = new PropertiesSubstitutor(params);
        StringBuilder tmp = new StringBuilder();
        for (String table : tables) {
            params.put("table", table);
            tmp.setLength(0);
            appendColumnNames(schema, table, tmp);
            params.put("columns", tmp.toString());
            tmp.setLength(0);
            appendColumnNames(schema, table, tmp, ", ", "?");
            params.put("values", tmp.toString());
            queriesXml.append(ps.substitute(block));
        }
        params.put("etl.properties", propsName);
        params.put("queries", queriesXml.toString());
        //Writing an ETL file
        Writer w = newFileWriter(xmlName);
        w.write(ps.substitute(etlXml));
        w.close();
        //Writing properties
        w = newFileWriter(propsName);
        w.write(loadResourceAsString(DATA_MIGRATOR_ETL_PROPERTIES));
        w.close();
    }


    private static StringBuilder appendColumnNames(DbSchema schema, final String table, final StringBuilder sql) {
        return appendColumnNames(schema, table, sql, ", ", "");
    }

    private static StringBuilder appendColumnNames(DbSchema schema, final String table,
                                                   final StringBuilder sql, final String separator, final String prefix) {
        final Set<String> tableColumns = schema.getTableColumns(table);

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

    private static Set<String> sortTables(final DbSchema schema) {
        List<String> tables = schema.getTables();
        LOG.fine("Sorting " + tables);

        int n = tables.size();

        String tbls[] = tables.toArray(new String[n]);

        try {
            int[][] m = getTablesMatrix(schema, tbls);
            StringBuilder msg = DEBUG ? new StringBuilder() : null;


            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (DEBUG) {
                        msg.append(m[i][j]);
                        msg.append((m[i][j] >= 10) ? " " : "  ");
                    }
                }
                if (DEBUG) {
                    msg.append(tbls[i]).append('\n');
                }
            }
            if (DEBUG) {
                LOG.fine("Tables dependencies matrix: \n" + msg);
            }

            boolean free[] = new boolean[n];
            Arrays.fill(free, true);

            Set<String> res = new LinkedHashSet<String>();

            for (int i = 0; i < n; i++) {
                //on each i iteration we choose the best candidate (having minimum number of incoming relationships)
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

    private static int[][] getTablesMatrix(DbSchema schema, String[] tables) throws SQLException {
        final DatabaseMetaData metaData = schema.getMetaData();
        int n = tables.length;
        int m[][] = new int[n][n];

        for (int[] a : m) {
            Arrays.fill(a, 0);
        }

        for (int i = 0; i < n; i++) {
            final ResultSet rs = metaData.getExportedKeys(schema.getCatalog(), schema.getSchema(), tables[i]);
            while (rs.next()) {
                String t2 = rs.getString("FKTABLE_NAME");
                int i2 = indexOf(tables, t2);

                if (i2 >= 0) {
                    m[i][i2] += ((rs.getInt("DELETE_RULE") != 2) ? 10 : 1);
                }
            }

            rs.close();
        }
        return m;
    }

    private static int indexOf(final String list[], final String element) {
        for (int i = 0; i < list.length; i++) {
            if (list[i].equalsIgnoreCase(element)) {
                return i;
            }
        }

        return -1;
    }

    static class DbSchema {
        private Connection connection;
        private DatabaseMetaData metaData;
        private String catalog;
        private String schema;

        public DbSchema(Connection connection, String catalog, String schema) {
            this.connection = connection;
            this.catalog = catalog;
            this.schema = schema;
        }

        static DbSchema initialize(Map<String, ?> props) {
            String driver = (String) props.get(DRIVER_PROPERTY_NAME);
            if (driver == null) {
                throw new IllegalArgumentException(DRIVER_PROPERTY_NAME + " property is required");
            }
            String jdbcUrl = (String) props.get(URL_PROPERTY_NAME);
            if (jdbcUrl == null) {
                throw new IllegalArgumentException(URL_PROPERTY_NAME + " property is required");
            }

            String user = (String) props.get(USER_PROPERTY_NAME);
            String password = (String) props.get(PASSWORD_PROPERTY_NAME);
            String catalog = (String) props.get(CATALOG_PROPERTY_NAME);
            String schema = (String) props.get(SCHEMA_PROPERTY_NAME);
            try {
                Class.forName(driver);
                return new DbSchema(DriverManager.getConnection(jdbcUrl, user, password), catalog, schema);
            } catch (ClassNotFoundException e) {
                throw new SystemException("Cannot lookup JDBC driver " + driver, e);
            } catch (SQLException e) {
                throw new SystemException("Cannot initialize JDBC connection " + jdbcUrl, e);
            }
        }


        List<String> getTables() {
            try {
                return getColumn(getMetaData()
                        .getTables(catalog, schema, null, new String[]{"TABLE"}), 3);
            } catch (SQLException e) {
                throw new JdbcException(e.getMessage(), e);
            }
        }

        Set<String> getTableColumns(final String tableName) {
            try {
                return new HashSet<String>(
                        getColumn(getMetaData().getColumns(catalog, schema, tableName, null), 4));
            } catch (SQLException e) {
                throw new JdbcException(e.getMessage(), e);
            }
        }


        /**
         * Iterates through the resultset and returns column values.
         *
         * @param rs        resultset to iterate.
         * @param columnPos column position. Starts at 1.
         * @return list of column values.
         */
        static List<String> getColumn(final ResultSet rs, final int columnPos) {
            List<String> l = new ArrayList<String>();
            try {
                while (rs.next()) {
                    l.add(rs.getString(columnPos));
                }
            } catch (SQLException e) {
                throw new JdbcException("Unable to get column #" + columnPos, e);
            } finally {
                JdbcUtils.closeSilent(rs);
            }

            return l;
        }

        /**
         * Returns lazily initialized meta data.
         *
         * @return db meta data.
         */
        public DatabaseMetaData getMetaData() {
            if (metaData == null) {
                try {
                    metaData = connection.getMetaData();
                } catch (SQLException e) {
                    throw new JdbcException("Unable to get database metadata");
                }
            }
            return metaData;
        }


        public String getCatalog() {
            return catalog;
        }

        public String getSchema() {
            return schema;
        }
    }

}

