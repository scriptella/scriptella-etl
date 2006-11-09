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
import java.net.URL;
import java.net.URLConnection;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
class JdbcTypesConverter implements Closeable {
    private List<InputStream> resources;

    /**
     * Gets the value of the designated column in the current row of this ResultSet
     * object as an Object in the Java programming language.
     *
     * @param rs
     * @param index column index.
     * @param jdbcType column {@link java.sql.Types JDBC type}
     * @return
     * @throws SQLException
     * @see ResultSet#getObject(int)
     */
    public Object getObject(final ResultSet rs, final int index, final int jdbcType) throws SQLException {
        //TODO for longvarchar/longvarbinary maybe use get...stream, and convert it to blob?
        switch(jdbcType) {
            case Types.DATE: //For date/timestamp use getTimestamp to keep hh,mm,ss if possible
            case Types.TIMESTAMP:
                return rs.getTimestamp(index);
            case Types.TIME:
                return rs.getTime(index);
            case Types.BLOB:
                return rs.getBlob(index);
            case Types.CLOB:
                return rs.getClob(index);
        }
        return rs.getObject(index);
    }

    /**
     * Sets the value of the designated parameter using the given object.
     * <p>Depending on the value type the concrete subclass of JdbcTypesConverter is chosen.
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
        } else if (value instanceof InputStream) {
            setStreamObject(preparedStatement, index, (InputStream) value);
        } else if (value instanceof URL) {
            setURLObject(preparedStatement, index, (URL) value);
        //For BLOBs/CLOBs use JDBC 1.0 methods for compatibility
        } else if (value instanceof Blob) {
            Blob b = (Blob) value;
            preparedStatement.setBinaryStream(index, b.getBinaryStream(), (int) b.length());
        } else if (value instanceof Clob) {
            Clob c = (Clob) value;
            preparedStatement.setCharacterStream(index, c.getCharacterStream(), (int) c.length());
        } else if (value instanceof Date) {
            setDateObject(preparedStatement, index, (Date) value);
        } else if (value instanceof Calendar) {
            preparedStatement.setTimestamp(index, new Timestamp(((Calendar)value).getTimeInMillis()), (Calendar) value);
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
     * Sets the {@link java.util.Date} or its descendant as a statement parameter.
     */
    protected void setDateObject(final PreparedStatement ps, final int index, final Date date) throws SQLException {
        if (date instanceof Timestamp) {
            ps.setTimestamp(index, (Timestamp) date);
        } else if (date instanceof java.sql.Date) {
            ps.setDate(index, (java.sql.Date) date);
        } else if (date instanceof Time) {
            ps.setTime(index, (Time) date);
        } else {
            ps.setTimestamp(index, new Timestamp(date.getTime()));
        }
    }


    /**
     * Sets a content of the file specified by URL.
     */
    protected void setURLObject(final PreparedStatement ps, final int index, final URL url) throws SQLException {
        try {
            final URLConnection c = url.openConnection();
            final InputStream is = new BufferedInputStream(c.getInputStream());

            if (resources == null) {
                resources = new ArrayList<InputStream>();
            }

            resources.add(is);

            int len = c.getContentLength();

            if (len < 0) {
                //todo move this code to a lobs factory
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
        if (resources != null) {
            for (InputStream is : resources) {
                IOUtils.closeSilently(is);
            }
            resources = null;
        }
    }


}
