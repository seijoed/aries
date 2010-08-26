/*
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
package org.apache.aries.samples.blog.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.bootDelegationPackages;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption;

import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.AriesApplicationContext;
import org.apache.aries.application.management.AriesApplicationManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;



@RunWith(JUnit4TestRunner.class)
public class JpaBlogSampleWithEbaTest extends AbstractIntegrationTest {

    @Test
    public void test() throws Exception {
	
	/* Install and start the blog eba */
	URL urlToEba = getUrlToEba("org.apache.aries.samples.blog", "org.apache.aries.samples.blog.jpa.eba");
	AriesApplicationManager manager = getOsgiService(AriesApplicationManager.class);
	AriesApplication app = manager.createApplication(urlToEba);
	AriesApplicationContext ctx = manager.install(app);
	ctx.start();

    /* Find and check all the blog sample bundles */

	Bundle bapi = getInstalledBundle("org.apache.aries.samples.blog.api");
    assertNotNull(bapi);
	assertEquals(Bundle.ACTIVE, bapi.getState());

	Bundle bweb = getInstalledBundle("org.apache.aries.samples.blog.web");
    assertNotNull(bweb);
	assertEquals(Bundle.ACTIVE, bweb.getState());

	Bundle bbiz = getInstalledBundle("org.apache.aries.samples.blog.biz");
    assertNotNull(bbiz);
	assertEquals(Bundle.ACTIVE, bbiz.getState());

	Bundle bper = getInstalledBundle("org.apache.aries.samples.blog.persistence.jpa");
    assertNotNull(bper);
	assertEquals(Bundle.ACTIVE, bper.getState());
 
    /* Datasource and transaction manager services are used by the blog sample */
	Bundle bds = getInstalledBundle("org.apache.aries.samples.blog.datasource");
	Bundle txs = getInstalledBundle("org.apache.aries.transaction.manager");

    /*Wait for all the required services to be registered */
    waitForServices(bbiz, "org.apache.aries.samples.blog.api.BloggingService");
    waitForServices(bper, "org.apache.aries.samples.blog.api.persistence.BlogPersistenceService");
    waitForServices(bds, "javax.sql.XADataSource");
    waitForServices(txs, "javax.transaction.TransactionManager");


    /*Check that they haven't timed out trying to register*/
	assertTrue("No services reistered for " + bbiz.getSymbolicName(), isServiceRegistered(bbiz));
	assertTrue("No services reistered for " + bper.getSymbolicName(), isServiceRegistered(bper));
	assertTrue("No services reistered for " + bds.getSymbolicName(), isServiceRegistered(bds));
	assertTrue("No services reistered for " + txs.getSymbolicName(), isServiceRegistered(txs));

	/*Check what services are registered - uncomment for additional debug */
	/*
	listBundleServices(bbiz);
	listBundleServices(bper);
	listBundleServices(bds);
	listBundleServices(txs);
    
	System.out.println("In test and trying to get connection....");
	*/

	HttpURLConnection conn = makeConnection("http://localhost:8080/org.apache.aries.samples.blog.web/ViewBlog");
    String response = getHTTPResponse(conn);

	/* Uncomment for additional debug */
	/*
	System.out.println("ZZZZZ " + response);
    System.out.println("ZZZZZ " + conn.getResponseCode());
    System.out.println("ZZZZZ " + HttpURLConnection.HTTP_OK);
	*/


    assertEquals(HttpURLConnection.HTTP_OK,
        conn.getResponseCode());

    assertTrue("The response did not contain the expected content", response.contains("Blog home"));
	ctx.stop();
    manager.uninstall(ctx);

    }


    @org.ops4j.pax.exam.junit.Configuration
    public static Option[] configuration() {
        Option[] options = options(
bootDelegationPackages("javax.transaction", "javax.transaction.*"),
			vmOption("-Dorg.osgi.framework.system.packages=javax.accessibility,javax.activation,javax.activity,javax.annotation,javax.annotation.processing,javax.crypto,javax.crypto.interfaces,javax.crypto.spec,javax.imageio,javax.imageio.event,javax.imageio.metadata,javax.imageio.plugins.bmp,javax.imageio.plugins.jpeg,javax.imageio.spi,javax.imageio.stream,javax.jws,javax.jws.soap,javax.lang.model,javax.lang.model.element,javax.lang.model.type,javax.lang.model.util,javax.management,javax.management.loading,javax.management.modelmbean,javax.management.monitor,javax.management.openmbean,javax.management.relation,javax.management.remote,javax.management.remote.rmi,javax.management.timer,javax.naming,javax.naming.directory,javax.naming.event,javax.naming.ldap,javax.naming.spi,javax.net,javax.net.ssl,javax.print,javax.print.attribute,javax.print.attribute.standard,javax.print.event,javax.rmi,javax.rmi.CORBA,javax.rmi.ssl,javax.script,javax.security.auth,javax.security.auth.callback,javax.security.auth.kerberos,javax.security.auth.login,javax.security.auth.spi,javax.security.auth.x500,javax.security.cert,javax.security.sasl,javax.sound.midi,javax.sound.midi.spi,javax.sound.sampled,javax.sound.sampled.spi,javax.sql,javax.sql.rowset,javax.sql.rowset.serial,javax.sql.rowset.spi,javax.swing,javax.swing.border,javax.swing.colorchooser,javax.swing.event,javax.swing.filechooser,javax.swing.plaf,javax.swing.plaf.basic,javax.swing.plaf.metal,javax.swing.plaf.multi,javax.swing.plaf.synth,javax.swing.table,javax.swing.text,javax.swing.text.html,javax.swing.text.html.parser,javax.swing.text.rtf,javax.swing.tree,javax.swing.undo,javax.tools,javax.xml,javax.xml.bind,javax.xml.bind.annotation,javax.xml.bind.annotation.adapters,javax.xml.bind.attachment,javax.xml.bind.helpers,javax.xml.bind.util,javax.xml.crypto,javax.xml.crypto.dom,javax.xml.crypto.dsig,javax.xml.crypto.dsig.dom,javax.xml.crypto.dsig.keyinfo,javax.xml.crypto.dsig.spec,javax.xml.datatype,javax.xml.namespace,javax.xml.parsers,javax.xml.soap,javax.xml.stream,javax.xml.stream.events,javax.xml.stream.util,javax.xml.transform,javax.xml.transform.dom,javax.xml.transform.sax,javax.xml.transform.stax,javax.xml.transform.stream,javax.xml.validation,javax.xml.ws,javax.xml.ws.handler,javax.xml.ws.handler.soap,javax.xml.ws.http,javax.xml.ws.soap,javax.xml.ws.spi,javax.xml.xpath,org.ietf.jgss,org.omg.CORBA,org.omg.CORBA.DynAnyPackage,org.omg.CORBA.ORBPackage,org.omg.CORBA.TypeCodePackage,org.omg.CORBA.portable,org.omg.CORBA_2_3,org.omg.CORBA_2_3.portable,org.omg.CosNaming,org.omg.CosNaming.NamingContextExtPackage,org.omg.CosNaming.NamingContextPackage,org.omg.Dynamic,org.omg.DynamicAny,org.omg.DynamicAny.DynAnyFactoryPackage,org.omg.DynamicAny.DynAnyPackage,org.omg.IOP,org.omg.IOP.CodecFactoryPackage,org.omg.IOP.CodecPackage,org.omg.Messaging,org.omg.PortableInterceptor,org.omg.PortableInterceptor.ORBInitInfoPackage,org.omg.PortableServer,org.omg.PortableServer.CurrentPackage,org.omg.PortableServer.POAManagerPackage,org.omg.PortableServer.POAPackage,org.omg.PortableServer.ServantLocatorPackage,org.omg.PortableServer.portable,org.omg.SendingContext,org.omg.stub.java.rmi,org.w3c.dom,org.w3c.dom.bootstrap,org.w3c.dom.css,org.w3c.dom.events,org.w3c.dom.html,org.w3c.dom.ls,org.w3c.dom.ranges,org.w3c.dom.stylesheets,org.w3c.dom.traversal,org.w3c.dom.views,org.xml.sax,org.xml.sax.ext,org.xml.sax.helpers,javax.transaction;partial=true;mandatory:=partial,javax.transaction.xa;partial=true;mandatory:=partial"),
            // Log
            mavenBundle("org.ops4j.pax.logging", "pax-logging-api"),
            mavenBundle("org.ops4j.pax.logging", "pax-logging-service"),
            // Felix mvn url handler - do we need this?
            mavenBundle("org.ops4j.pax.url", "pax-url-mvn"),


            // this is how you set the default log level when using pax logging (logProfile)
            systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("DEBUG"),

            // Bundles
            mavenBundle("org.eclipse.equinox", "cm"),
            mavenBundle("org.eclipse.osgi", "services"),

            mavenBundle("org.ops4j.pax.web", "pax-web-extender-war"),
            mavenBundle("org.ops4j.pax.web", "pax-web-jetty-bundle"),
            mavenBundle("org.ops4j.pax.web", "pax-web-jsp"),
            mavenBundle("org.apache.derby", "derby"),
            mavenBundle("org.apache.geronimo.specs", "geronimo-jpa_2.0_spec"),

            mavenBundle("org.apache.geronimo.specs", "geronimo-jta_1.1_spec"),
            mavenBundle("org.apache.geronimo.specs", "geronimo-j2ee-connector_1.5_spec"),
            mavenBundle("org.apache.geronimo.components", "geronimo-transaction"),
            mavenBundle("org.apache.openjpa", "openjpa"),
            mavenBundle("commons-lang", "commons-lang"),
            mavenBundle("commons-collections", "commons-collections"),
            mavenBundle("commons-pool", "commons-pool"),
            mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.serp"),
            mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint" ),
            mavenBundle("org.apache.aries", "org.apache.aries.util" ),
            mavenBundle("org.apache.aries.jndi", "org.apache.aries.jndi" ),
            mavenBundle("org.apache.aries.application", "org.apache.aries.application.install" ),
            mavenBundle("org.apache.aries.application", "org.apache.aries.application.api" ),
            mavenBundle("org.apache.aries.application", "org.apache.aries.application.management" ),
            mavenBundle("org.apache.aries.application", "org.apache.aries.application.runtime" ),
            mavenBundle("org.apache.aries.application", "org.apache.aries.application.utils" ),
            mavenBundle("org.apache.aries.application", "org.apache.aries.application.modeller"),
            mavenBundle("org.apache.aries.application", "org.apache.aries.application.deployment.management"),
            mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.api" ),
            mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.container" ),
            mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.blueprint.aries" ),
            mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.container.context" ),
            mavenBundle("org.apache.aries.transaction", "org.apache.aries.transaction.manager" ),
            mavenBundle("org.apache.aries.transaction", "org.apache.aries.transaction.blueprint" ),
            mavenBundle("org.apache.aries.transaction", "org.apache.aries.transaction.wrappers" ),
            mavenBundle("org.apache.aries.samples.blog", "org.apache.aries.samples.blog.datasource" ),
            mavenBundle("asm", "asm-all" ),
            equinox().version("3.5.0")
        );
        options = updateOptions(options);
        return options;
    }

}
