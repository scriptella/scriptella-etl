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
package scriptella.driver.mail;

import scriptella.configuration.ConfigurationException;
import scriptella.expression.PropertiesSubstitutor;
import scriptella.spi.AbstractConnection;
import scriptella.spi.ConnectionParameters;
import scriptella.spi.ParametersCallback;
import scriptella.spi.ProviderException;
import scriptella.spi.QueryCallback;
import scriptella.spi.Resource;
import scriptella.util.CollectionUtils;
import scriptella.util.StringUtils;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;
import java.io.Reader;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a JavaMail connection.
 * <p>For configuration details and examples see <a href="package-summary.html">overview page</a>.
 * <p/>
 * <p>TODO:
 * <ul>
 * <li>Support for attachments
 * <li>Increase batch send performance by reusing tranport connection
 * </ul>
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class MailConnection extends AbstractConnection {
    private static final Logger LOG = Logger.getLogger(MailConnection.class.getName());

    /**
     * Name of the <code>type</code> connection property.
     * Specifies type of E-Mail message content: {@link #TYPE_TEXT}, {@link #TYPE_HTML}.
     */
    public static final String TYPE = "type";

    /**
     * Name of the <code>subject</code> connection property.
     * Specifies e-mail subject.
     */
    public static final String SUBJECT = "subject";

    public static final String TYPE_TEXT = "text";
    public static final String TYPE_HTML = "html";

    private static final Pattern ADDRESS_PTR = Pattern.compile("mailto:([^\\&\\?]*)");
    private String to;
    private Session session;
    private String type;
    private String subject;


    /**
     * Creates an email connection.
     *
     * @param parameters connection parameters.
     */
    public MailConnection(ConnectionParameters parameters) {
        super(Driver.DIALECT, parameters);
        String url = parameters.getUrl();
        if (StringUtils.isEmpty(url)) {
            throw new ConfigurationException("URL connection attribute is requred");
        }
        Matcher m = ADDRESS_PTR.matcher(url);
        if (!m.find()) {
            throw new ConfigurationException("URL connection attribute is not valid: " + url);
        }
        to = m.group(1).trim();
        if (to.length()==0) {
            throw new ConfigurationException("List of email addresses cannot be empty");
        }
        if (to.indexOf('$')>=0) { //Validate if mailto has no bind variable
            try {
                InternetAddress.parse(to, false);
            } catch (AddressException e) {
                throw new ConfigurationException("URL connection attribute must represent comma separated list of " +
                        "email addresses and follow RFC822 syntax: " + url, e);
            }
        }

        Properties properties = CollectionUtils.asProperties(parameters.getProperties());
        properties.putAll(parameters.getUrlQueryMap());
        type = properties.getProperty(TYPE);
        if (type != null && !type.equalsIgnoreCase(TYPE_TEXT) && !type.equalsIgnoreCase(TYPE_HTML)) {
            throw new ConfigurationException("Type parameter value must be one of text or html");
        }
        subject = properties.getProperty(SUBJECT);
        if (subject == null) {
            LOG.fine("EMail subject is not set for connection!");
        }

        session = Session.getInstance(properties);

        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Mail session initialized");
        }
    }

    public void executeScript(Resource scriptContent, ParametersCallback parametersCallback) throws ProviderException {
        PropertiesSubstitutor ps = new PropertiesSubstitutor(parametersCallback);
        MimeMessage mimeMessage;
        String addresslist = ps.substitute(to);
        try {
            mimeMessage = format(scriptContent.open(), ps);
            InternetAddress[] mailto = InternetAddress.parse(addresslist, false);
            mimeMessage.addRecipients(Message.RecipientType.TO, mailto);
        } catch (AddressException e) {
            throw new MailProviderException("URL connection attribute must represent comma separated list of " +
                    "email addresses and follow RFC822 syntax: " + addresslist, e);
        } catch (MessagingException e) {
            throw new MailProviderException("Failed to prepare message", e);
        } catch (IOException e) {
            throw new MailProviderException("Unable to read message text", e);
        }

        if (isReadonly()) {
            if (LOG.isLoggable(Level.INFO)) {
                LOG.info("Readonly Mode - Not sending a message to " + addresslist);
            }
        } else {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Sending a message to " + addresslist);
            }
            try {
                send(mimeMessage);
            } catch (MessagingException e) {
                throw new MailProviderException("Failed to send message", e);
            }
        }
    }

    protected MimeMessage format(Reader reader, PropertiesSubstitutor ps) throws MessagingException, IOException {
        //Read message body content
        String text = ps.substitute(reader);
        //Create message and set headers
        MimeMessage message = new MimeMessage(session);

        message.setFrom(InternetAddress.getLocalAddress(session));

        if (subject != null) {
            message.setSubject(ps.substitute(subject));
        }
        //if html content
        if (TYPE_HTML.equalsIgnoreCase(type)) {
            BodyPart body = new MimeBodyPart();
            body.setContent(text, "text/html");
            Multipart mp = new MimeMultipart("related");
            mp.addBodyPart(body);
            message.setContent(mp);
        } else {
            message.setText(text);
        }
        return message;
    }

    /**
     * Template method to decouple transport dependency, overriden in test classes.
     *
     * @param message message to send.
     */
    protected void send(MimeMessage message) throws MessagingException {
        Transport.send(message);
    }

    public void executeQuery(Resource queryContent, ParametersCallback parametersCallback, QueryCallback queryCallback) throws ProviderException {
        throw new MailProviderException("Queries are not supported");
    }

    public void close() throws ProviderException {
    }
}
