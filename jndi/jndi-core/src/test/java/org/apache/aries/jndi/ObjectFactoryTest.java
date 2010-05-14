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
package org.apache.aries.jndi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.lang.reflect.Field;
import java.util.Hashtable;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.NamingManager;
import javax.naming.spi.ObjectFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.jndi.JNDIConstants;

import org.apache.aries.unittest.mocks.MethodCall;
import org.apache.aries.unittest.mocks.Skeleton;
import org.apache.aries.jndi.ContextHelper;
import org.apache.aries.jndi.OSGiObjectFactoryBuilder;
import org.apache.aries.jndi.startup.Activator;
import org.apache.aries.mocks.BundleContextMock;

public class ObjectFactoryTest
{
  private BundleContext bc;
  private Hashtable env;

  /**
   * This method does the setup .
   * @throws NoSuchFieldException 
   * @throws SecurityException 
   * @throws IllegalAccessException 
   * @throws IllegalArgumentException 
   */
  @Before
  public void setup() throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException
  {
    bc =  Skeleton.newMock(new BundleContextMock(), BundleContext.class);
    new Activator().start(bc);
        
    env = new Hashtable();
    env.put(JNDIConstants.BUNDLE_CONTEXT, bc);
  }

  /**
   * Make sure we clear the caches out before the next test.
   */
  @After
  public void teardown()
  {
    new Activator().stop(bc);
    BundleContextMock.clear();
  }

  @Test
  public void testURLReferenceWithNoMatchingHandler() throws Exception
  {
    Reference ref = new Reference(null);
    ref.add(new StringRefAddr("URL", "wibble"));
    Object obj = NamingManager.getObjectInstance(ref, null, null, env);

    assertSame("The naming manager should have returned the reference object", ref, obj);
  }

  @Test
  public void testURLReferenceWithMatchingHandler() throws Exception
  {
    String testObject = "Test object";
    ObjectFactory factory = Skeleton.newMock(ObjectFactory.class);
    Skeleton.getSkeleton(factory).setReturnValue(new MethodCall(ObjectFactory.class, "getObjectInstance", Object.class, Name.class, Context.class, Hashtable.class), testObject);

    Properties props = new Properties();
    props.setProperty("osgi.jndi.urlScheme", "wibble");

    bc.registerService(ObjectFactory.class.getName(), factory, props);

    Reference ref = new Reference(null);
    ref.add(new StringRefAddr("URL", "wibble"));
    Object obj = NamingManager.getObjectInstance(ref, null, null, env);
    
    assertEquals("The naming manager should have returned the test object", testObject, obj);
  }

  @Test
  public void testReferenceWithNoClassName() throws Exception
  {
    String testObject = "Test object";
    ObjectFactory factory = Skeleton.newMock(ObjectFactory.class);
    Skeleton.getSkeleton(factory).setReturnValue(new MethodCall(ObjectFactory.class, "getObjectInstance", Object.class, Name.class, Context.class, Hashtable.class), testObject);

    bc.registerService(ObjectFactory.class.getName(), factory, null);

    Reference ref = new Reference(null);
    Object obj = NamingManager.getObjectInstance(ref, null, null, env);
    
    assertEquals("The naming manager should have returned the test object", testObject, obj);
  }

  @Test
  public void testSpecifiedFactoryWithMatchingFactory() throws Exception
  {
    String testObject = "Test object";
    ObjectFactory factory = Skeleton.newMock(ObjectFactory.class);
    Skeleton.getSkeleton(factory).setReturnValue(new MethodCall(ObjectFactory.class, "getObjectInstance", Object.class, Name.class, Context.class, Hashtable.class), testObject);

    Reference ref = new Reference("dummy.class.name", factory.getClass().getName(), "");

    bc.registerService(new String[] {ObjectFactory.class.getName(), factory.getClass().getName()}, 
                       factory, null);

    Object obj = NamingManager.getObjectInstance(ref, null, null, env);
    
    assertEquals("The naming manager should have returned the test object", testObject, obj);
  }

  @Test
  public void testSpecifiedFactoryWithRegisteredButNotMatchingFactory() throws Exception
  {
    String testObject = "Test object";
    ObjectFactory factory = Skeleton.newMock(ObjectFactory.class);
    Skeleton.getSkeleton(factory).setReturnValue(new MethodCall(ObjectFactory.class, "getObjectInstance", Object.class, Name.class, Context.class, Hashtable.class), testObject);

    Reference ref = new Reference("dummy.class.name", "dummy.factory.class.name", "");

    bc.registerService(new String[] {ObjectFactory.class.getName(), factory.getClass().getName()}, 
                       factory, null);

    Object obj = NamingManager.getObjectInstance(ref, null, null, env);

    assertSame("The naming manager should have returned the reference object", ref, obj);
  }

  @Test
  public void testSpecifiedFactoryWithNoMatchingFactory() throws Exception
  {
    Reference ref = new Reference("dummy.class.name");

    Object obj = NamingManager.getObjectInstance(ref, null, null, env);

    assertSame("The naming manager should have returned the reference object", ref, obj);
  }
}