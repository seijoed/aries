/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.blueprint.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.text.SimpleDateFormat;
import java.util.Currency;
import java.util.Hashtable;
import java.util.Set;

import org.apache.aries.blueprint.sample.Bar;
import org.apache.aries.blueprint.sample.Foo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

@RunWith(JUnit4TestRunner.class)
public class BlueprintAnnotationTest extends AbstractIntegrationTest {

    @Test
    public void test() throws Exception {
        BlueprintContainer blueprintContainer = getBlueprintContainerForBundle(
                 bundleContext, "org.apache.aries.blueprint.sample-annotation",
                5000);

        assertNotNull(blueprintContainer);

        Object obj = blueprintContainer.getComponentInstance("bar");
        assertNotNull(obj);
        assertEquals(Bar.class, obj.getClass());
        Bar bar = (Bar) obj;
        assertEquals("Hello FooBar", bar.getValue());
        
        obj = blueprintContainer.getComponentInstance("foo");
        assertNotNull(obj);
        assertEquals(Foo.class, obj.getClass());
        Foo foo = (Foo) obj;
        assertEquals(5, foo.getA());
       // assertEquals(10, foo.getB());
        assertSame(bar, foo.getBar());
        assertEquals(Currency.getInstance("PLN"), foo.getCurrency());
        assertEquals(new SimpleDateFormat("yyyy.MM.dd").parse("2009.04.17"),
                foo.getDate());

        assertTrue(foo.isInitialized());
        assertFalse(foo.isDestroyed());

       obj = getOsgiService(bundleContext, Foo.class, null, 5000);
        assertNotNull(obj);
        assertEquals(foo.toString(), obj.toString());
    }

    @org.ops4j.pax.exam.junit.Configuration
    public static Option[] configuration() {
        Option[] options = options(
            // Log
            mavenBundle("org.ops4j.pax.logging", "pax-logging-api"),
            mavenBundle("org.ops4j.pax.logging", "pax-logging-service"),
            // Felix Config Admin
            mavenBundle("org.apache.felix", "org.apache.felix.configadmin"),
            // Felix mvn url handler
            mavenBundle("org.ops4j.pax.url", "pax-url-mvn"),


            // this is how you set the default log level when using pax logging (logProfile)
            systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("DEBUG"),

            // Bundles
            mavenBundle("org.apache.aries", "org.apache.aries.util"),
            mavenBundle("asm", "asm-all"),
            mavenBundle("org.apache.xbean", "xbean-finder"),
            mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.annotation.api"),
            mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint"),
            mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.annotation.impl"),
            mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.sample-annotation"),
            //mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.sample"),
            mavenBundle("org.osgi", "org.osgi.compendium"),
            //org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption("-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"),

            equinox().version("3.5.0")
        );
        options = updateOptions(options);
        return options;
    }

}
