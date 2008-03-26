package scriptella.driver.as400;

import scriptella.jdbc.GenericDriver;

/**
 * Scriptella Adapter for an IBM AS/400 System.
 *
 * <p>For configuration details and examples see <a href="package-summary.html">overview page</a>.
 *
 * @author Kirill Volgin
 * @version 1.0
 */
public class Driver extends GenericDriver {
    public static final String AS400_DRIVER_NAME = "com.ibm.as400.access.AS400JDBCDriver";


    public Driver() {
        loadDrivers(AS400_DRIVER_NAME);
    }

}
