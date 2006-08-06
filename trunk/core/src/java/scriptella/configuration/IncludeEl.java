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
package scriptella.configuration;

import scriptella.spi.Resource;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class IncludeEl extends XMLConfigurableBase implements Resource {
    private URL url;
    private String href;
    private Charset charset;
    private static final Logger LOG = Logger.getLogger(IncludeEl.class.getName());
    private FallbackEl fallbackEl;

    public IncludeEl(XMLElement element) {
        configure(element);
    }

    public String getHref() {
        return href;
    }

    public void setHref(final String href) {
        this.href = href;
    }

    public Charset getCharset() {
        return charset;
    }

    public void setCharset(final Charset charset) {
        this.charset = charset;
    }

    public FallbackEl getFallbackEl() {
        return fallbackEl;
    }

    public void setFallbackEl(FallbackEl fallbackEl) {
        this.fallbackEl = fallbackEl;
    }

    public void configure(final XMLElement element) {
        url = element.getDocumentURL();
        setRequiredProperty(element, "href");

        final String enc = element.getAttribute("encoding");

        if (enc == null) {
            charset = Charset.defaultCharset();
        } else {
            if (!Charset.isSupported(enc)) {
                throw new ConfigurationException("Encoding " + enc +
                        " is not supported", element);
            }

            charset = Charset.forName(enc);
        }
        final XMLElement fallbackElement = element.getChild("fallback");
        if (fallbackElement != null) {
            fallbackEl = new FallbackEl(fallbackElement);
        }
    }

    public Reader open() throws IOException {
        try {
            URL u = new URL(url, href);
            return new InputStreamReader(u.openStream(), charset);
        } catch (MalformedURLException e) {
            throw (IOException) new IOException("Malformed include url: " + href).initCause(e);
        } catch (IOException e) {
            if (fallbackEl != null) {
                LOG.log(Level.FINE, e.getMessage());
                return fallbackEl.open();
            } else {
                throw e;
            }
        }
    }
}
