/**
 * $Header: $
 * $Revision: $
 * $Date: $
 *
 * Copyright 2003-2005
 * All rights reserved.
 */
package scriptella.driver.jexl;

import scriptella.spi.ProviderException;

/**
 * Thrown to indicate a problem with JEXL script.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class JexlProviderException extends ProviderException {
    public JexlProviderException() {
    }

    public JexlProviderException(String message) {
        super(message);
    }

    public JexlProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    public JexlProviderException(Throwable cause) {
        super(cause);
    }

    public String getProviderName() {
        return Driver.DIALECT.getName();
    }
}
