package scriptella.driver.odbc;

import scriptella.jdbc.GenericDriver;

/**
 * Scriptella Adapter for Sun's JDBC-ODBC driver.
 *
 * <p>For configuration details and examples see <a href="package-summary.html">overview page</a>.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class Driver extends GenericDriver {
    public static final String ODBC_DRIVER_NAME = "sun.jdbc.odbc.JdbcOdbcDriver";

    public Driver() {
        loadDrivers(ODBC_DRIVER_NAME);
    }

}