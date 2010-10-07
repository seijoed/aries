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
package org.apache.aries.application.runtime.itests;

import static org.junit.Assert.assertEquals;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.io.File;
import java.io.FileOutputStream;

import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.AriesApplicationContext;
import org.apache.aries.application.management.AriesApplicationManager;
import org.apache.aries.application.utils.filesystem.FileSystem;
import org.apache.aries.sample.HelloWorld;
import org.apache.aries.unittest.fixture.ArchiveFixture;
import org.apache.aries.unittest.fixture.ArchiveFixture.ZipFixture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;

@RunWith(JUnit4TestRunner.class)
public class BasicNoOpResolverTest extends AbstractIntegrationTest {
  
  /* Use @Before not @BeforeClass so as to ensure that these resources
   * are created in the paxweb temp directory, and not in the svn tree 
   */
  static boolean createdApplications = false;
  @Before
  public static void createApplications() throws Exception {
    if (createdApplications) { 
      return;
    }
    ZipFixture testEba = ArchiveFixture.newZip()
      .jar("sample.jar")
        .manifest().symbolicName("org.apache.aries.sample")
          .attribute("Bundle-Version", "1.0.0")
          .attribute("Import-Package", "org.apache.aries.sample")
          .end()
        .binary("org/apache/aries/sample/impl/HelloWorldImpl.class", 
            BasicAppManagerTest.class.getClassLoader().getResourceAsStream("org/apache/aries/sample/impl/HelloWorldImpl.class"))
        .binary("OSGI-INF/blueprint/sample-blueprint.xml", 
            BasicAppManagerTest.class.getClassLoader().getResourceAsStream("basic/sample-blueprint.xml"))
        .end();
      
    FileOutputStream fout = new FileOutputStream("test.eba");
    testEba.writeOut(fout);
    fout.close();
    
    ZipFixture testEba2 = testEba.binary("META-INF/APPLICATION.MF", 
        BasicAppManagerTest.class.getClassLoader().getResourceAsStream("basic/APPLICATION.MF"))
        .end();
    fout = new FileOutputStream("test2.eba");
    testEba2.writeOut(fout);
    fout.close();
    createdApplications = true;
  }
  
  @Test
  public void testAppWithoutApplicationManifest() throws Exception {
    
    AriesApplicationManager manager = getOsgiService(AriesApplicationManager.class);
    AriesApplication app = manager.createApplication(FileSystem.getFSRoot(new File("test.eba")));
    
    // application name should be equal to eba name since application.mf is not provided
    assertEquals("test.eba", app.getApplicationMetadata().getApplicationName());
    AriesApplicationContext ctx = manager.install(app);
    ctx.start();
    
    HelloWorld hw = getOsgiService(HelloWorld.class);
    String result = hw.getMessage();
    assertEquals (result, "hello world");
    
    ctx.stop();
    manager.uninstall(ctx);
  }

  @Test
  public void testAppWithApplicationManifest() throws Exception {
    AriesApplicationManager manager = getOsgiService(AriesApplicationManager.class);
    AriesApplication app = manager.createApplication(FileSystem.getFSRoot(new File("test2.eba")));
    
    // application name should equal to whatever Application name provided in the application.mf
    assertEquals("test application 2", app.getApplicationMetadata().getApplicationName());
    
    AriesApplicationContext ctx = manager.install(app);
    ctx.start();
    
    HelloWorld hw = getOsgiService(HelloWorld.class);
    String result = hw.getMessage();
    assertEquals (result, "hello world");
    
    ctx.stop();
    manager.uninstall(ctx);
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

        // this is how you set the default log level when using pax
        // logging (logProfile)
        systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("DEBUG"),

        // Bundles
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.api"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.utils"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.deployment.management"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.modeller"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.noop.platform.repo"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.noop.postresolve.process"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.management"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.runtime"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.resolver.noop"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.runtime.itest.interfaces"),
        mavenBundle("org.apache.aries", "org.apache.aries.util"),
        mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint"), 
        mavenBundle("org.osgi", "org.osgi.compendium"),
        mavenBundle("org.apache.aries.testsupport", "org.apache.aries.testsupport.unit"),
        
        
        /* For debugging, uncomment the next two lines
        vmOption ("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5006"),
        waitForFrameworkStartup(),
        
        and add these imports:
        import static org.ops4j.pax.exam.CoreOptions.waitForFrameworkStartup;
        import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption;
        */

        equinox().version("3.5.0"));
    options = updateOptions(options);
    return options;
  }
}
