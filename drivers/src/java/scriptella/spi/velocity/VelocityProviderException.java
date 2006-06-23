/**
 * $Header: $
 * $Revision: $
 * $Date: $
 *
 * Copyright 2003-2005
 * All rights reserved.
 */
package scriptella.spi.velocity;

import scriptella.spi.ProviderException;

/**
 * Thrown by Velocity Provider to indicate velocity failure.
 */
public class VelocityProviderException extends ProviderException {
    public VelocityProviderException() {
    }

    public VelocityProviderException(String message) {
        super(message);
    }

    public VelocityProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    public VelocityProviderException(Throwable cause) {
        super(cause);
    }

}
