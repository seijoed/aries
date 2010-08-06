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
package org.apache.aries.jpa.container.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;

import org.apache.aries.jpa.container.ManagedPersistenceUnitInfo;
import org.apache.aries.jpa.container.PersistenceUnitConstants;
import org.apache.aries.jpa.container.parsing.ParsedPersistenceUnit;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * This class manages the lifecycle of Persistence Units and their associated
 * {@link EntityManagerFactory} objects.
 */
public class EntityManagerFactoryManager {

  /**
   * A callback for a named persistence units
   */
  class NamedCallback {
    private final Set<String> names;
    private final DestroyCallback callback;
    public NamedCallback(Collection<String> names, DestroyCallback countdown) {
      this.names = new HashSet<String>(names);
      callback = countdown;
    }

    public void callback(String name) {
      boolean winner;
      synchronized (this) {
        winner = !!!names.isEmpty() && names.remove(name) && names.isEmpty();
      }
      if(winner)
        callback.callback();
    }
  }

  /** The container's {@link BundleContext} */
  private final BundleContext containerContext;
  /** The persistence bundle */
  private final Bundle bundle;
  /** The {@link PersistenceProvider} to use */
  private ServiceReference provider;
  /** The persistence units to manage */
  private Collection<ManagedPersistenceUnitInfo> persistenceUnits;
  /** The original parsed data */
  private Collection<ParsedPersistenceUnit> parsedData;
  /** A Map of created {@link EntityManagerFactory}s */
  private Map<String, CountingEntityManagerFactory> emfs = null;
  /** The {@link ServiceRegistration} objects for the {@link EntityManagerFactory}s */
  private ConcurrentMap<String, ServiceRegistration> registrations = null;
  /** Quiesce this Manager */
  private boolean quiesce = false;

  /** Logger */
  private static final Logger _logger = LoggerFactory.getLogger("org.apache.aries.jpa.container");
  
  /**
   * Create an {@link EntityManagerFactoryManager} for
   * the supplied persistence bundle.
   * 
   * This constructor should only be used by a 
   * {@link PersistenceBundleManager} that is synchronized
   * on itself, and the resulting manager should be immediately
   * stored in the bundleToManager Map
   * 
   * @param b
   * @param infos 
   * @param ref 
   * @param parsedUnits 
   */
  public EntityManagerFactoryManager(BundleContext containerCtx, Bundle b, Collection<ParsedPersistenceUnit> parsedUnits, ServiceReference ref, Collection<ManagedPersistenceUnitInfo> infos) {
    containerContext = containerCtx;
    bundle = b;
    provider = ref;
    persistenceUnits = infos;
    parsedData = parsedUnits;
  }

  /**
   * Notify the {@link EntityManagerFactoryManager} that a provider is being
   * removed from the service registry.
   * 
   * If the provider is used by this {@link EntityManagerFactoryManager} then
   * the manager should destroy the dependent persistence units.
   * 
   * <b>This method should only be called when not holding any locks</b>
   * 
   * @param ref  The provider service reference
   * @return true if the the provider is being used by this manager
   */
  public synchronized boolean providerRemoved(ServiceReference ref) {
    
    boolean toReturn = provider.equals(ref);
    
    if(toReturn)
      destroy();
    
    return toReturn;
  }

  /**
   * Notify the {@link EntityManagerFactoryManager} that the bundle it is
   * managing has changed state
   * 
   * <b>This method should only be called when not holding any locks</b>
   * 
   * @throws InvalidPersistenceUnitException if the manager is no longer valid and
   *                                         should be destroyed
   */
  public synchronized void bundleStateChange() throws InvalidPersistenceUnitException {
    
    switch(bundle.getState()) {
      case Bundle.RESOLVED :
        //If we are Resolved as a result of having stopped
        //and missed the STOPPING event we need to unregister
        unregisterEntityManagerFactories();
        //Create the EMF objects if necessary
        createEntityManagerFactories();
        break;
        //Starting and active both require EMFs to be registered
      case Bundle.STARTING :
      case Bundle.ACTIVE :
        registerEntityManagerFactories();
        break;
        //Stopping means the EMFs should
      case Bundle.STOPPING :
        unregisterEntityManagerFactories();
        break;
      case Bundle.INSTALLED :
        //Destroy everything
        destroyEntityManagerFactories();
    }
  }

  /**
   * Unregister all {@link EntityManagerFactory} services
   */
  private void unregisterEntityManagerFactories() {
    //If we have registrations then unregister them
    if(registrations != null) {
      for(ServiceRegistration reg : registrations.values()) {
        try {
          reg.unregister();
        } catch (Exception e) {
          _logger.error("There was an error unregistering the EntityManagerFactory services for bundle " 
              + bundle.getSymbolicName() + "_" + bundle.getVersion() , e);
        }
      }
      // remember to set registrations to be null
      registrations = null;
    }
  }

  /**
   * Register {@link EntityManagerFactory} services
   * 
   * @throws InvalidPersistenceUnitException if this {@link EntityManagerFactory} is no longer
   *  valid and should be destroyed
   */
  private void registerEntityManagerFactories() throws InvalidPersistenceUnitException {
    //Only register if there is a provider and we are not
    //already registered
    if(provider != null && registrations == null && !quiesce) {
      //Make sure the EntityManagerFactories are instantiated
      createEntityManagerFactories();
      
      registrations = new ConcurrentHashMap<String, ServiceRegistration>();
      String providerName = (String) provider.getProperty("javax.persistence.provider");
      if(providerName == null) {
        _logger.warn("The PersistenceProvider for bundle {} did not specify a provider name in the \"javax.persistence.provider\" service property. " +
        		"As a result EntityManagerFactory objects will not be registered with the " 
            + PersistenceUnitConstants.OSGI_UNIT_PROVIDER + " property. " 
            + "The Peristence Provider service was {}",
            new Object[] {bundle.getSymbolicName() + "_" + bundle.getVersion(), provider});
      }
      //Register each EMF
      for(Entry<String, ? extends EntityManagerFactory> entry : emfs.entrySet())
      {
        Properties props = new Properties();
        String unitName = entry.getKey();
          
        props.put(PersistenceUnitConstants.OSGI_UNIT_NAME, unitName);
        if(providerName != null)
          props.put(PersistenceUnitConstants.OSGI_UNIT_PROVIDER, providerName);
        props.put(PersistenceUnitConstants.OSGI_UNIT_VERSION, provider.getBundle().getVersion());
        props.put(PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT, Boolean.TRUE);
        props.put(PersistenceUnitConstants.EMPTY_PERSISTENCE_UNIT_NAME, "".equals(unitName));
        try {
          registrations.put(unitName, bundle.getBundleContext().registerService(EntityManagerFactory.class.getCanonicalName(), entry.getValue(), props));
        } catch (Exception e) {
          _logger.error("There was an error registering the persistence unit " 
              + unitName + " defined by the bundle " + bundle.getSymbolicName() + "_" + bundle.getVersion(), e);
          throw new InvalidPersistenceUnitException(e);
        }
      }
    }
  }

  /**
   * Create {@link EntityManagerFactory} services for this peristence unit
   * throws InvalidPersistenceUnitException if this {@link EntityManagerFactory} is no longer
   *  valid and should be destroyed
   */
  private void createEntityManagerFactories() throws InvalidPersistenceUnitException {
    //Only try if we have a provider and EMFs
    if(provider != null) {
      if(emfs == null && !quiesce) {
        try {
          emfs = new HashMap<String, CountingEntityManagerFactory>();
        
          //Get hold of the provider
          PersistenceProvider providerService = (PersistenceProvider) containerContext.getService(provider);

          if(providerService == null) {
            _logger.warn("The PersistenceProvider service hosting persistence units in bundle " 
                + bundle.getSymbolicName() + "_" + bundle.getVersion() + " is no longer available. " +
                		"Persistence units defined by the bundle will not be available until the bundle is refreshed");
            throw new InvalidPersistenceUnitException();
          }
      
          for(ManagedPersistenceUnitInfo info : persistenceUnits){
            PersistenceUnitInfo pUnitInfo = info.getPersistenceUnitInfo();
        
            emfs.put(pUnitInfo.getPersistenceUnitName(), new CountingEntityManagerFactory(
                providerService.createContainerEntityManagerFactory(
                    pUnitInfo, info.getContainerProperties()), pUnitInfo.getPersistenceUnitName()));
          }
        } finally {
          //Remember to unget the provider
          containerContext.ungetService(provider);
        }
      }
    }
  }

  /**
   * Manage the EntityManagerFactories for the following
   * provider and {@link PersistenceUnitInfo}s
   * 
   * This method should only be called when not holding any locks
   * 
   * @param ref The {@link PersistenceProvider} {@link ServiceReference}
   * @param infos The {@link PersistenceUnitInfo}s defined by our bundle
   */
  public synchronized void manage(ServiceReference ref,
      Collection<ManagedPersistenceUnitInfo> infos)  throws IllegalStateException{
    provider = ref;
    persistenceUnits = infos;
  }
  
  /**
   * Manage the EntityManagerFactories for the following
   * provider, updated persistence xmls and {@link PersistenceUnitInfo}s
   * 
   * This method should only be called when not holding any locks
   * 
   * @param parsedUnits The updated {@link ParsedPersistenceUnit}s for this bundle 
   * @param ref The {@link PersistenceProvider} {@link ServiceReference}
   * @param infos The {@link PersistenceUnitInfo}s defined by our bundle
   */
  public synchronized void manage(Collection<ParsedPersistenceUnit> parsedUnits, ServiceReference ref,
      Collection<ManagedPersistenceUnitInfo> infos)  throws IllegalStateException{
    parsedData = parsedUnits;
    provider = ref;
    persistenceUnits = infos;
  }

  /**
   * Stop managing any {@link EntityManagerFactory}s 
   * 
   * This method should only be called when not holding any locks
   */
  public synchronized void destroy() {
    destroyEntityManagerFactories();
    
    provider = null;
    persistenceUnits = null;
  }

  /**
   * S
   */
  private void destroyEntityManagerFactories() {
    if(registrations != null)
      unregisterEntityManagerFactories();
    if(emfs != null) {
      for(Entry<String, ? extends EntityManagerFactory> entry : emfs.entrySet()) {
        try {
          entry.getValue().close();
        } catch (Exception e) {
          _logger.error("There was an exception when closing the EntityManagerFactory for persistence unit "
              + entry.getKey() + " in bundle " + bundle.getSymbolicName() + "_" + bundle.getVersion(), e);
        }
      }
    }
    emfs = null;
  }

  public Bundle getBundle() {
    return bundle;
  }

  public Collection<ParsedPersistenceUnit> getParsedPersistenceUnits()
  {
    return parsedData;
  }
  /** Quiesce this Manager */
  public void quiesce(DestroyCallback countdown) {
    
    //Find the EMFs to quiesce, and their Service registrations
    Map<CountingEntityManagerFactory, ServiceRegistration> entries = new HashMap<CountingEntityManagerFactory, ServiceRegistration>();
    Collection<String> names = new ArrayList<String>();
    synchronized(this) {
      quiesce = true;
      if(emfs != null) {
        for(String key : emfs.keySet()) {
          entries.put(emfs.get(key), registrations != null ? registrations.get(key) : null);
          names.add(key);
        }
      }
    }
    //Quiesce as necessary
    if(entries.isEmpty())
      countdown.callback();
    else {
    NamedCallback callback = new NamedCallback(names, countdown);
      for(Entry<CountingEntityManagerFactory, ServiceRegistration> entry : entries.entrySet()) {
        CountingEntityManagerFactory emf = entry.getKey();
        emf.quiesce(callback, entry.getValue());
      }
    }
  }

}
