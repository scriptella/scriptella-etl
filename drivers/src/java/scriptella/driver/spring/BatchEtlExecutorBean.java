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
package scriptella.driver.spring;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import scriptella.interactive.ProgressIndicator;

import java.io.IOException;
import java.util.Map;

/**
 * Batched implementation to run {@link scriptella.execution.EtlExecutor ETL executors} for Spring IoC container.
 * <p>This class exposes a set of configurable properties and provides
 * a {@link Runnable} invocation interface to avoid dependency on Scriptella
 * in application code.
 * <p>Use init-method="run" to automatically start execution of ETL files on Spring initialization.
 * <h2>Example</h2>
 * Spring XML configuration file:
 * <pre><code>
 * &lt;bean class="scriptella.driver.spring.BatchEtlExecutorBean" init-method="run">
 *       &lt;property name="configLocations">&lt;list>
 *           &lt;value>/scriptella/driver/spring/batch1.etl.xml</value>
 *           &lt;value>/scriptella/driver/spring/batch2.etl.xml</value>
 *       &lt;/list>&lt;/property>
 *       &lt;property name="properties">
 *           &lt;map>
 *               &lt;entry key="tableName" value="Batch"/>
 *           &lt;/map>
 *       &lt;/property>
 * &lt;/bean>
 * </code></pre>
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class BatchEtlExecutorBean implements Runnable, InitializingBean, BeanFactoryAware {
    private BeanFactory beanFactory;
    private ProgressIndicator progressIndicator;
    private boolean jmxEnabled;
    private Map<String, ?> properties;
    private Resource[] configLocations;
    private EtlExecutorBean[] etlExecutors;

    /**
     * Creates scripts executor.
     */
    public BatchEtlExecutorBean() {
    }

    public void setJmxEnabled(boolean jmxEnabled) {
        this.jmxEnabled = jmxEnabled;
    }

    /**
     * Sets progress indicator to use.
     * <p>By default no progress shown.
     *
     * @param progressIndicator progress indicator to use.
     */
    public void setProgressIndicator(ProgressIndicator progressIndicator) {
        this.progressIndicator = progressIndicator;
    }

    /**
     * Sets configuration locations.
     *
     * @param resources configuration resources.
     * @throws java.io.IOException if I/O error occurs
     */
    public void setConfigLocations(Resource[] resources) throws IOException {
        configLocations = resources;
    }

    public void setProperties(Map<String, ?> properties) {
        this.properties = properties;
    }

    public void afterPropertiesSet() throws Exception {
        if (configLocations == null) {
            throw new IllegalStateException("configLocations must be specified");
        } else { //Initialize configuration
            int n = configLocations.length;
            etlExecutors = new EtlExecutorBean[n];
            for (int i = 0; i < n; i++) {
                EtlExecutorBean bean = new EtlExecutorBean();
                bean.setConfigLocation(configLocations[i]);
                bean.setProgressIndicator(progressIndicator);
                bean.setProperties(properties);
                bean.setJmxEnabled(jmxEnabled);
                bean.setBeanFactory(beanFactory);
                bean.afterPropertiesSet();
                etlExecutors[i] = bean;
            }
        }
    }

    /**
     * Executes ETL files in a batch.
     */
    public void run() {
        for (EtlExecutorBean executor : etlExecutors) {
            executor.run();
        }
    }

    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
}