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
package org.apache.aries.jndi.url;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;

public class ServiceRegistryListContext implements Context
{
  private Map<String, Object> env;
  /** The name parser for the service registry name space */
  private NameParser parser = new OsgiNameParser();
  
  public ServiceRegistryListContext(Map<String, Object> env, OsgiName validName)
  {
    this.env = new HashMap<String, Object>(env);
  }

  public Object addToEnvironment(String propName, Object propVal) throws NamingException
  {
    return env.put(propName, propVal);
  }

  public void bind(Name name, Object obj) throws NamingException
  {
    throw new OperationNotSupportedException();
  }

  public void bind(String name, Object obj) throws NamingException
  {
    throw new OperationNotSupportedException();
  }

  public void close() throws NamingException
  {
    env = null;
    parser = null;
  }

  public Name composeName(Name name, Name prefix) throws NamingException
  {
    // TODO Auto-generated method stub
    return null;
  }

  public String composeName(String name, String prefix) throws NamingException
  {
    // TODO Auto-generated method stub
    return null;
  }

  public Context createSubcontext(Name name) throws NamingException
  {
    throw new OperationNotSupportedException();
  }

  public Context createSubcontext(String name) throws NamingException
  {
    throw new OperationNotSupportedException();
  }

  public void destroySubcontext(Name name) throws NamingException
  {
    //No-op we don't support sub-contexts in our context
  }

  public void destroySubcontext(String name) throws NamingException
  {
    //No-op we don't support sub-contexts in our context
  }

  public Hashtable<?, ?> getEnvironment() throws NamingException
  {
    Hashtable<Object, Object> environment = new Hashtable<Object, Object>();
    environment.putAll(env);
    return environment;
  }

  public String getNameInNamespace() throws NamingException
  {
    // TODO Auto-generated method stub
    return null;
  }

  public NameParser getNameParser(Name name) throws NamingException
  {
    return parser;
  }

  public NameParser getNameParser(String name) throws NamingException
  {
    return parser;
  }

  public NamingEnumeration<NameClassPair> list(Name name) throws NamingException
  {
    return list(name.toString());
  }

  public NamingEnumeration<NameClassPair> list(String name) throws NamingException
  {
    // TODO Auto-generated method stub
    return null;
  }

  public NamingEnumeration<Binding> listBindings(Name name) throws NamingException
  {
    return listBindings(name.toString());
  }

  public NamingEnumeration<Binding> listBindings(String name) throws NamingException
  {
    // TODO Auto-generated method stub
    return null;
  }

  public Object lookup(Name name) throws NamingException
  {
    return lookup(name.toString());
  }

  public Object lookup(String name) throws NamingException
  {
    return null;
  }

  public Object lookupLink(Name name) throws NamingException
  {
    throw new OperationNotSupportedException();
  }

  public Object lookupLink(String name) throws NamingException
  {
    throw new OperationNotSupportedException();
  }

  public void rebind(Name name, Object obj) throws NamingException
  {
    throw new OperationNotSupportedException();
  }

  public void rebind(String name, Object obj) throws NamingException
  {
    throw new OperationNotSupportedException();
  }

  public Object removeFromEnvironment(String propName) throws NamingException
  {
    return env.remove(propName);
  }

  public void rename(Name oldName, Name newName) throws NamingException
  {
    throw new OperationNotSupportedException();
  }

  public void rename(String oldName, String newName) throws NamingException
  {
    throw new OperationNotSupportedException();
  }

  public void unbind(Name name) throws NamingException
  {
    throw new OperationNotSupportedException();
  }

  public void unbind(String name) throws NamingException
  {
    throw new OperationNotSupportedException();
  }
}