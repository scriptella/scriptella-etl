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
package scriptella.jdbc;

import scriptella.expression.Expression;
import scriptella.spi.DriverContext;
import scriptella.spi.ParametersCallback;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Parses parameter expressions in SQL statements.
 * This class use {@link Expression} parsing mechanism except the
 * file reference case.
 * <p>The syntax of the file reference is the following:
 * <p>?{file &lt;Expression&gt;}
 * <p>The result of expression evaluation must be of String or URL type.
 * <p>Examples:
 * <ul>
 * <li>?{file 'http://site.com/img.gif')
 * <li>?{file 'file:/path/img.gif')
 * <li>?{file 'file:img.gif') - represents a reference relative to a directory where script file located.
 * </ul>
 * @see Expression
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ParametersParser {
    private DriverContext driverContext;


    /**
     * Creates a file reference parser.
     * @param driverContext drivers content to use for URL resolution.
     */
    public ParametersParser(DriverContext driverContext) {
        this.driverContext = driverContext;
    }

    /**
     * Parses specified expression and returns the result of evaluation.
     * @param expression expression to parse.
     * @return result of parse.
     */
    public Object evaluate(final String expression, final ParametersCallback parameters) {
        if (expression.startsWith("file ")) {
            //now checking the syntax
            try {
                final Expression ex = Expression.compile(expression.substring(5));//file prefix removed
                final Object o = ex.evaluate(parameters);
                if (o==null) {
                    throw new JdbcException("Failed to evaluate file URL", expression);
                }
                if (o instanceof URL) {
                    return (URL) o;
                } else {
                    try {
                        return driverContext.resolve(String.valueOf(o));
                    } catch (MalformedURLException e) {
                        throw new JdbcException("Wrong file URL \""+o+"\"", e, expression);
                    }
                }
            } catch (Expression.ParseException e) {
                //If parsing fails try to evaluate the whole expression not the file reference
            }
        }
        return Expression.compile(expression).evaluate(parameters);
    }

}
