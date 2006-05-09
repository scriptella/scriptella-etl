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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a setter for prepared statement parameters.
 * <p>This class defines a strategy for handling specific parameters like {@link URL}
 * and by default provides a generic behaviour for any objects.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class JDBCSetter implements Closeable {
    private PreparedStatement ps;
    private UrlSetter urlSetter; //Strategy for files

    /**
     * For internal purposes only
     */
    protected JDBCSetter() {
    }

    /**
     * Creates setter for <code>preparedStatement</code>.
     *
     * @param preparedStatement preparedStatement to set parameters.
     */
    public JDBCSetter(PreparedStatement preparedStatement) {
        ps = preparedStatement;
    }

    /**
     * Sets the value of the designated parameter using the given object.
     * <p>Depending on the value type the concrete subclass of JDBCSetter is chosen.
     *
     * @param index he first parameter is 1, the second is 2, ...
     * @param value the object containing the input parameter value
     * @throws SQLException
     */
    public void setObject(int index, Object value) throws SQLException {
        //Choosing a setter strategy
        if (value instanceof URL) {
            if (urlSetter == null) {
                urlSetter = new UrlSetter();
            }
            urlSetter.setObject(index, value);
        } else {
            ps.setObject(index, value);

        }
    }

    /**
     * Closes any resources opened during this object lifecycle.
     */
    public void close() {
        ps = null;
        if (urlSetter != null) {
            urlSetter.close();
            urlSetter = null;
        }
    }

    /**
     * {@link URL} handling strategy.
     * URLs are treated as reference to content which should be downloaded
     * as set as stream parameter.
     */
    class UrlSetter extends JDBCSetter {
        private List<InputStream> streams;

        public void setObject(int index, Object value) throws SQLException {
            URL url = (URL) value;
            try {
                final URLConnection c = url.openConnection();
                final InputStream is = new BufferedInputStream(c.getInputStream());

                if (streams == null) {
                    streams = new ArrayList<InputStream>(2);
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

        public void close() {
            if (streams != null) {
                for (InputStream is : streams) {
                    IOUtils.closeSilently(is);
                }
                streams = null;
            }
        }


    }


}
