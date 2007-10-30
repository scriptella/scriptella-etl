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
package scriptella.driver.janino;

import org.codehaus.janino.ScriptEvaluator;
import org.codehaus.janino.util.LocatedException;
import scriptella.expression.LineIterator;
import scriptella.spi.Resource;
import scriptella.util.ExceptionUtils;
import scriptella.util.IOUtils;

import java.io.IOException;
import java.io.Reader;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Compiles Janino scripts.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
final class CodeCompiler {
    //Allowed exceptions to be thrown by a script
    private static final Class[] THROWN_EXCEPTIONS = new Class[]{Exception.class};
    //Compiled scripts(Static methods) cache
    private Map<Resource, Object> objectCache = new IdentityHashMap<Resource, Object>();

    public JaninoScript compileScript(final Resource resource) {
        return (JaninoScript) compile(resource, false);
    }

    public JaninoQuery compileQuery(final Resource resource) {
        return (JaninoQuery) compile(resource, true);
    }

    private Object compile(final Resource content, final boolean query) {
        Object ctx = objectCache.get(content);
        if (ctx == null) {
            ScriptEvaluator evaluator = new ScriptEvaluator();
            //Exception are not required to be handled
            evaluator.setThrownExceptions(THROWN_EXCEPTIONS);
            evaluator.setParentClassLoader(getClass().getClassLoader());
            Class<?> type = query ? JaninoQuery.class : JaninoScript.class;
            evaluator.setExtendedType(type);
            evaluator.setStaticMethod(false);
            evaluator.setMethodName("execute");
            evaluator.setClassName(type.getName() + "_Generated");

            Reader r = null;
            try {
                r = content.open();
                evaluator.cook(content.toString(), r);
            } catch (Exception e) {
                throw guestErrorStatement(new JaninoProviderException("Compilation failed", e), content);
            } finally {
                IOUtils.closeSilently(r);
            }
            Class<?> cl = evaluator.getMethod().getDeclaringClass();
            try {
                ctx = cl.newInstance();
            } catch (Exception e) {
                throw new JaninoProviderException("Unable to instantiate compiled class", e);
            }
            objectCache.put(content, ctx);
        }
        return ctx;
    }

    /**
     * Finds error statement which caused compilation error.
     */
    private static JaninoProviderException guestErrorStatement(JaninoProviderException pe, Resource r) {
        Throwable cause = pe.getCause();
        if (cause instanceof LocatedException) {
            LocatedException le = (LocatedException) cause;
            if (le.getLocation() != null) {
                String line = getLine(r, le.getLocation().getLineNumber());
                pe.setErrorStatement(line);
            }
        }
        return pe;
    }

    static String getLine(Resource resource, int line) {
        LineIterator it = null;
        try {
            it = new LineIterator(resource.open());
            return it.getLineAt(line - 1);
        } catch (IOException e) {
            ExceptionUtils.ignoreThrowable(e);
        } finally {
            IOUtils.closeSilently(it);
        }
        return null;
    }


}
