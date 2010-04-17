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

import scriptella.AbstractTestCase;
import scriptella.configuration.ConfigurationException;
import scriptella.configuration.MockConnectionEl;
import scriptella.configuration.StringResource;
import scriptella.spi.ConnectionParameters;
import scriptella.spi.MockDriverContext;
import scriptella.spi.MockParametersCallbacks;

import javax.mail.Message;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests for {@link MailConnection}.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class MaiConnectionTest extends AbstractTestCase {
    /**
     * Test for text email
     */
    public void testText() {

        Map<String, String> parameters = new HashMap<String, String>();
        ConnectionParameters cp = new ConnectionParameters(new MockConnectionEl(parameters,
                "mailto:scriptella@gmail.com"), MockDriverContext.INSTANCE);
        MailConnection mc = new MailConnection(cp) {
            @Override
            protected void send(MimeMessage message) {
                try {
                    assertEquals("scriptella@gmail.com", message.getRecipients(Message.RecipientType.TO)[0].toString());
                    assertEquals("Message. *example*", message.getContent());
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }

            }
        };
        mc.executeScript(new StringResource("Message. $example"), MockParametersCallbacks.SIMPLE);
    }

    /**
     * Test for html email
     */
    public void testHtml() {

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("type", "html");
        ConnectionParameters cp = new ConnectionParameters(new MockConnectionEl(parameters,
                "mailto:scriptella@gmail.com?subject=Hello"), MockDriverContext.INSTANCE);
        MailConnection mc = new MailConnection(cp) {
            @Override
            protected void send(MimeMessage message) {
                try {
                    assertEquals("scriptella@gmail.com", message.getRecipients(Message.RecipientType.TO)[0].toString());
                    assertEquals("Hello", message.getSubject());
                    assertEquals("Message. *example*",
                            ((MimeMultipart)message.getContent()).getBodyPart(0).getContent());
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }

            }
        };
        mc.executeScript(new StringResource("Message. $example"), MockParametersCallbacks.SIMPLE);
    }

    public void testValidation() {
        Map<String, String> parameters = new HashMap<String, String>();
        ConnectionParameters cp = new ConnectionParameters(new MockConnectionEl(parameters,
                null), MockDriverContext.INSTANCE);
        try {
            new MailConnection(cp);
            fail("URL cannot be empty required");
        } catch (ConfigurationException e) {
            //OK
        }
        cp = new ConnectionParameters(new MockConnectionEl(parameters,
                "test"), MockDriverContext.INSTANCE);
        try {
            new MailConnection(cp);
            fail("URL must be a valid mailto");
        } catch (ConfigurationException e) {
            //OK
        }

    }

    /**
     * Tests mailto with bind variables
     */
    public void testDynamicMailto() {
        Map<String, String> parameters = new HashMap<String, String>();
        ConnectionParameters cp = new ConnectionParameters(new MockConnectionEl(parameters,
                "mailto:$address?subject=$subject"), MockDriverContext.INSTANCE);
        final Map<String,String> params = new HashMap<String, String>();
        MailConnection mc = new MailConnection(cp) {
            @Override
            protected void send(MimeMessage message) {
                try {
                    assertEquals(params.get("address"), message.getRecipients(Message.RecipientType.TO)[0].toString());
                    assertEquals("Message. "+params.get("example"), message.getContent());
                    assertEquals(params.get("subject"), message.getSubject());
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }

            }
        };
        params.put("address", "scriptella@gmail.com");
        params.put("example", "*example*");
        params.put("subject", "Hello1");
        mc.executeScript(new StringResource("Message. $example"), MockParametersCallbacks.fromMap(params));
        params.put("address", "scriptella@javaforge.com");
        params.put("subject", "Hello2");
        mc.executeScript(new StringResource("Message. $example"), MockParametersCallbacks.fromMap(params));
        params.put("address", "////@");
        try {
            mc.executeScript(new StringResource("Message. $example"), MockParametersCallbacks.fromMap(params));
        } catch (MailProviderException e) {
            assertTrue("Invalid address must be reported", e.getMessage().indexOf("////@")>=0);
        }


    }

}
