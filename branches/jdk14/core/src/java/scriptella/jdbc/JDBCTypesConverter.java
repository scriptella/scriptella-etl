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
package scriptella.jdbc;

import scriptella.util.IOUtils;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a converter for prepared statement parameters and result set columns.
 * <p>This class defines a strategy for handling specific parameters like {@link URL}
 * and by default provides a generic behaviour for any objects.
 * <p>Configuration by exception is the general phylosophy of this class, i.e.
 * most of the conversions must be performed by a provided resultset/preparedstatement and
 * custom conversions are applied only in rare cases. One of these cases is BLOB/CLOB handling.
 * <p>Specific adapters of JDBC drivers may provide a subclass of this class to
 * allow custom conversion conforming with the general contract of this class.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
class JDBCTypesConverter implements Closeable {
    private List<InputStream> streams;

    /**
     * Gets the value of the designated column in the current row of this ResultSet
     * object as an Object in the Java programming language.
     *
     * @param rs
     * @param index
     * @return
     * @throws SQLException
     * @see ResultSet#getObject(int)
     */
    public Object getObject(final ResultSet rs, final int index) throws SQLException {
        return rs.getObject(index);
    }

    /**
     * Sets the value of the designated parameter using the given object.
     * <p>Depending on the value type the concrete subclass of JDBCTypesConverter is chosen.
     *
     * @param preparedStatement prepared statement to set object.
     * @param index he first parameter is 1, the second is 2, ...
     * @param value the object containing the input parameter value
     * @throws SQLException
     */
    public void setObject(final PreparedStatement preparedStatement, final int index, final Object value) throws SQLException {
        //Choosing a setter strategy
        if (value==null) {
            preparedStatement.setObject(index, null);
        } else if (value instanceof InputStream) {//several drivers(e.g. H2) return streams for BLOBs
            setStreamObject(preparedStatement, index, (InputStream) value);
        } else if (value instanceof Reader) {//several drivers(e.g. H2) return readers for CLOBs
            setStreamObject(preparedStatement, index, (InputStream) value);
        } else if (value instanceof URL) {
            setURLObject(preparedStatement, index, (URL) value);
        } else {
            preparedStatement.setObject(index, value);
        }
    }

    /**
     * Sets input stream for statement.
     */
    protected void setStreamObject(final PreparedStatement ps, final int index, final InputStream stream) throws SQLException {
        try {
            //A hack is used to determine stream size, but most drivers do so
            //and available=size for memory and file streams.
            //May be we provide a better solution in future
            //Option converter.stream_to_array is also reasonable
            ps.setBinaryStream(index, stream, stream.available());
        } catch (IOException e) {
            throw (SQLException)new SQLException("Failed to check binary stream: "+e.getMessage()).initCause(e);
        }
    }

    /**
     * Sets a content of the file specified by URL.
     */
    protected void setURLObject(final PreparedStatement ps, final int index, final URL url) throws SQLException {
        try {
            final URLConnection c = url.openConnection();
            final InputStream is = new BufferedInputStream(c.getInputStream());

            if (streams == null) {
                streams = new ArrayList<InputStream>();
            }

            streams.add(is);

            int len = c.getContentLength();

            if (len < 0) {
                throw new SQLException("Unknown content-length for file " + url);
            }

            ps.setBinaryStream(index, is, len);
        } catch (IOException e) {
            throw (SQLException) new SQLException("Unable to read content for file " + url +
                    ": " + e.getMessage()).initCause(e);
        }


    }


    /**
     * Closes any resources opened during this object lifecycle.
     */
    public void close() {
        if (streams != null) {
            for (InputStream is : streams) {
                IOUtils.closeSilently(is);
            }
            streams = null;
        }
    }


}
