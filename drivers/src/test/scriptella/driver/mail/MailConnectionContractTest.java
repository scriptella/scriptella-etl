/*
 * Copyright 2006-2012 The Scriptella Project Team.
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
 * Characterization of the JavaMail driver contracts used by Scriptella.
 * <p>Coordinate upgrades to {@code com.sun.mail:javax.mail} must keep the
 * {@code javax.mail} package and these formatting rules.
 *
 * @author Scriptella Project Team
 */
public class MailConnectionContractTest extends AbstractTestCase {

    public void testMailtoUrlRequired() {
        ConnectionParameters cp = new ConnectionParameters(
                new MockConnectionEl(new HashMap<String, String>(), null),
                MockDriverContext.INSTANCE);
        try {
            new MailConnection(cp);
            fail("Empty URL must be rejected");
        } catch (ConfigurationException e) {
            // expected
        }
    }

    public void testTextMessageFormatting() {
        Map<String, String> parameters = new HashMap<String, String>();
        ConnectionParameters cp = new ConnectionParameters(
                new MockConnectionEl(parameters, "mailto:user@example.com"),
                MockDriverContext.INSTANCE);
        final boolean[] sent = {false};
        MailConnection mc = new MailConnection(cp) {
            @Override
            protected void send(MimeMessage message) {
                try {
                    assertEquals("user@example.com",
                            message.getRecipients(Message.RecipientType.TO)[0].toString());
                    assertEquals("body *token*", message.getContent());
                    sent[0] = true;
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        };
        mc.executeScript(new StringResource("body $token"), MockParametersCallbacks.SIMPLE);
        assertTrue(sent[0]);
    }

    public void testHtmlMessageFormatting() {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(MailConnection.TYPE, MailConnection.TYPE_HTML);
        ConnectionParameters cp = new ConnectionParameters(
                new MockConnectionEl(parameters, "mailto:user@example.com?subject=Subj"),
                MockDriverContext.INSTANCE);
        MailConnection mc = new MailConnection(cp) {
            @Override
            protected void send(MimeMessage message) {
                try {
                    assertEquals("Subj", message.getSubject());
                    Object content = ((MimeMultipart) message.getContent())
                            .getBodyPart(0).getContent();
                    assertEquals("<b>*x*</b>", content);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        };
        mc.executeScript(new StringResource("<b>$x</b>"), MockParametersCallbacks.SIMPLE);
    }
}
