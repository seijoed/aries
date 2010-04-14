/*
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
package org.apache.aries.subsystem.core.internal;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.aries.subsystem.SubsystemAdmin;
import org.apache.aries.subsystem.SubsystemConstants;
import org.apache.aries.subsystem.spi.ResourceProcessor;
import org.apache.aries.subsystem.spi.ResourceResolver;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The bundle activator for the this bundle.
 * When the bundle is starting, this activator will create
 * and register the SubsystemAdmin service.
 */
public class Activator implements BundleActivator {
    private static final Logger LOGGER = LoggerFactory.getLogger(Activator.class);
    
    private BundleContext context;
    private List<ServiceRegistration> registrations = new ArrayList<ServiceRegistration>();
    private static SubsystemEventDispatcher eventDispatcher;

    public void start(BundleContext context) throws Exception {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("subsystem activator starting");
        }
        this.context = context;
        eventDispatcher = new SubsystemEventDispatcher(context);
        
        register(SubsystemAdmin.class, new SubsystemAdminFactory(), null);
        register(ResourceResolver.class,
                 new NoOpResolver(),
                 DictionaryBuilder.build(Constants.SERVICE_RANKING, Integer.MIN_VALUE));
        register(ResourceResolver.class,
                new ResourceResolverImpl(this.context),
                DictionaryBuilder.build(Constants.SERVICE_RANKING, 0));
        register(ResourceProcessor.class,
                new BundleResourceProcessor(),
                DictionaryBuilder.build(SubsystemConstants.SERVICE_RESOURCE_TYPE, SubsystemConstants.RESOURCE_TYPE_BUNDLE));
        register(ResourceProcessor.class,
                new SubsystemResourceProcessor(),
                DictionaryBuilder.build(SubsystemConstants.SERVICE_RESOURCE_TYPE, SubsystemConstants.RESOURCE_TYPE_SUBSYSTEM));
    }

    protected <T> void register(Class<T> clazz, T service, Dictionary props) {
         registrations.add(context.registerService(clazz.getName(), service, props));
    }

    protected <T> void register(Class<T> clazz, ServiceFactory factory, Dictionary props) {
         registrations.add(context.registerService(clazz.getName(), factory, props));
    }

    public void stop(BundleContext context) throws Exception {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("subsystem activator stopping");
        }
        for (ServiceRegistration r : registrations) {
            try {
                r.unregister();
            } catch (Exception e) {
                LOGGER.warn("Subsystem Activator shut down", e);
            }
        }
        eventDispatcher.destroy();
    }


    public static class SubsystemAdminFactory implements ServiceFactory {

        private final Map<BundleContext, SubsystemAdminImpl> admins = new HashMap<BundleContext, SubsystemAdminImpl>();
        private final Map<SubsystemAdminImpl, Long> references = new HashMap<SubsystemAdminImpl, Long>();

        public synchronized Object getService(Bundle bundle, ServiceRegistration registration) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Get SubsystemAdmin service from bundle symbolic name {} version {}", bundle.getSymbolicName(), bundle.getVersion());
            }
            BundleContext systemBundleContext = bundle.getBundleContext().getBundle(0).getBundleContext();
            SubsystemAdminImpl admin = admins.get(systemBundleContext);
            long ref = 0;
            if (admin == null) {
                admin = new SubsystemAdminImpl(systemBundleContext, eventDispatcher);
                admins.put(systemBundleContext, admin);
            } else {
                ref = references.get(admin);
            }
            references.put(admin, ref + 1);
            return admin;
        }

        public synchronized void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unget SubsystemAdmin service {} from bundle symbolic name {} version {}", new Object[] {service, bundle.getSymbolicName(), bundle.getVersion()});
            }
            SubsystemAdminImpl admin = (SubsystemAdminImpl) service;
            long ref = references.get(admin) - 1;
            if (ref == 0) {
                admin.dispose();
                admins.remove(admin.context);
                references.remove(admin);
            } else {
                references.put(admin, ref);
            }
        }

    }

}
