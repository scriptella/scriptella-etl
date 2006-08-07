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
package scriptella.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Parses URIs string into tokens and returns reolved URLs as array.
 * <p>; and : are used as separators. This class is simlar to Ant's PathTokenizer but has
 * 2 important differences:
 * <ul>
 * <li>List of URLs instead of Paths
 * <li>This implementation does not take into accout the operating system we are currently running on
 * </ul>
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class UrlPathTokenizer {
    //Path separator regexp,
    // \\s* - means extra whitespaces
    // \\:(?=[^/]{2} - means do not treat protocol:// as two paths - protocol and //
    private static final Pattern SEPARATOR = Pattern.compile("\\s*(\\;|(\\:(?=[^/]{2})))\\s*");


    private final URL baseURL;

    public UrlPathTokenizer(URL baseURL) {
        this.baseURL = baseURL;
    }

    /**
     * Splits a string with set of URIs into array of URLs resolved relatively to baseURL.
     *
     * @param urls not null string with ; or : separated URIs.
     * @return array of resolved URLs.
     * @throws MalformedURLException if urls contain a malformed URI or URL
     */
    public URL[] split(String urls) throws MalformedURLException {
        if (urls == null) {
            throw new IllegalArgumentException("urls cannot be null");
        }
        String[] strings = SEPARATOR.split(urls);
        List<URL> res = new ArrayList<URL>(strings.length);
        for (String s : strings) {
            String u = s.trim();
            if (u.length() > 0) {
                res.add(new URL(baseURL, u));
            }
        }
        return res.toArray(new URL[res.size()]);
    }

}
