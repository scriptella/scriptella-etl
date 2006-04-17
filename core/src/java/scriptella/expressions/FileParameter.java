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
package scriptella.expressions;

import scriptella.sql.JDBCException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


/**
 * Represents reference to a file
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class FileParameter {
    private URL url;
    private List<InputStream> streams;

    public FileParameter(URL url) {
        this.url = url;
    }

    /**
     * <br>TODO: Extract interface
     *
     * @param ps    statement to insert parameter value
     * @param index this parameter index
     */
    public void insert(final PreparedStatement ps, final int index)
            throws SQLException {
        try {
            final URLConnection c = url.openConnection();
            final InputStream is = new BufferedInputStream(c.getInputStream());

            if (streams == null) {
                streams = new ArrayList<InputStream>(2);
            }

            streams.add(is);

            int len = c.getContentLength();

            if (len < 0) {
                throw new JDBCException("Unknown content-length for file " +
                        url);
            }

            ps.setBinaryStream(index, is, len);
        } catch (IOException e) {
            throw new JDBCException("Unable to read content for file " + url +
                    ": " + e.getMessage());
        }
    }

    public String toString() {
        return "FileParameter{" + "url=" + url + "}";
    }

    public void close() {
        if (streams != null) {
            for (InputStream is : streams) {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                    }
                }
            }

            streams = null;
        }
    }
}
