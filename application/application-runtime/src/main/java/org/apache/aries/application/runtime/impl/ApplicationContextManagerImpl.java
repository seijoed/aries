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
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.aries.application.runtime.impl;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.aries.application.DeploymentMetadata;
import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.AriesApplicationContext;
import org.apache.aries.application.management.ManagementException;
import org.apache.aries.application.management.UpdateException;
import org.apache.aries.application.management.AriesApplicationContext.ApplicationState;
import org.apache.aries.application.management.spi.runtime.AriesApplicationContextManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

public class ApplicationContextManagerImpl implements AriesApplicationContextManager {

  private ConcurrentMap<AriesApplication, ApplicationContextImpl> _appToContextMap;
  private BundleContext _bundleContext;
  
  public ApplicationContextManagerImpl () { 
    _appToContextMap = new ConcurrentHashMap<AriesApplication, ApplicationContextImpl>();
  }
  
  public void setBundleContext (BundleContext b) { 
    _bundleContext = b;
  }
  
  public AriesApplicationContext getApplicationContext(AriesApplication app) throws BundleException, ManagementException {
    ApplicationContextImpl result;
    if (_appToContextMap.containsKey(app)) { 
      result = _appToContextMap.get(app);
    } else { 
      result = new ApplicationContextImpl (_bundleContext, app);
      ApplicationContextImpl previous = _appToContextMap.putIfAbsent(app, result);
      if (previous != null) { 
        result = previous;
      }
    }
    return result;
  }

  public Set<AriesApplicationContext> getApplicationContexts() {
    Set<AriesApplicationContext> result = new HashSet<AriesApplicationContext>();
    for (Map.Entry<AriesApplication, ApplicationContextImpl> entry: _appToContextMap.entrySet()) {
      result.add (entry.getValue());
    }
    return result;
  }

  public void remove(AriesApplicationContext app)
  {
    Iterator<Map.Entry<AriesApplication, ApplicationContextImpl>> it = _appToContextMap.entrySet().iterator();
    
    while (it.hasNext()) {
      Map.Entry<AriesApplication, ApplicationContextImpl> entry = it.next();
      
      ApplicationContextImpl potentialMatch = entry.getValue();
      
      if (potentialMatch == app) {
        it.remove();

        uninstall(potentialMatch);

        break;
      }
    }
  }

  private void uninstall(ApplicationContextImpl app)
  {
    Set<Bundle> bundles = app.getApplicationContent();
    for (Bundle b : bundles) {
      try {
        b.uninstall();
      } catch (BundleException be) {
        // TODO ignoring this feels wrong, but I'm not sure how to communicate to the caller multiple failures. 
      }
    }
    app.setState(ApplicationState.UNINSTALLED);
  }
  
  public void close()
  {
    for (ApplicationContextImpl ctx : _appToContextMap.values()) {
      uninstall(ctx);
    }
    
    _appToContextMap.clear();
  }

  public AriesApplicationContext update(AriesApplication app, DeploymentMetadata oldMetadata) throws UpdateException {
    ApplicationContextImpl oldCtx = _appToContextMap.get(app);
    
    if (oldCtx == null) {
      throw new IllegalArgumentException("AriesApplication "+
          app.getApplicationMetadata().getApplicationSymbolicName() + "/" + app.getApplicationMetadata().getApplicationVersion() + 
          " cannot be updated because it is not installed");
    }
    
    uninstall(oldCtx);
    try {
      AriesApplicationContext newCtx = getApplicationContext(app);
      if (oldCtx.getApplicationState() == ApplicationState.ACTIVE) {
        newCtx.start();
      }
      
      return newCtx;
    } catch (BundleException e) {
      throw new UpdateException("Update failed: "+e.getMessage(), e, false, null);
    } catch (ManagementException e) {
      throw new UpdateException("Update failed: "+e.getMessage(), e, false, null);
    }
  }
}