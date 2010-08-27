/**
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
package org.apache.aries.blueprint.compendium.cm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Since persistence id can only be associated with one ManagedService in a bundle
 * this class ensures only one ManagedService is registered per persistence id.
 */
public class ManagedObjectManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagedObjectManager.class);

    private HashMap<String, ConfigurationWatcher> map = new HashMap<String, ConfigurationWatcher>();
               
    public synchronized void register(ManagedObject cm, Properties props) {
        String key = cm.getPersistentId();
        ConfigurationWatcher reg = map.get(key);
        if (reg == null) {
            reg = new ConfigurationWatcher(); 
            ServiceRegistration registration = cm.getBundle().getBundleContext().registerService(ManagedService.class.getName(), reg, props);
            reg.setRegistration(registration);            
            map.put(key, reg);
        }
        reg.add(cm);
    }

    public synchronized void unregister(ManagedObject cm) {
        String key = cm.getPersistentId();
        ConfigurationWatcher reg = map.get(key);
        if (reg != null) {
            reg.remove(cm);
            if (reg.isEmpty()) {
                map.remove(key);
                reg.getRegistration().unregister();
            }
        }
    }
            
    private static class ConfigurationWatcher implements ManagedService {

        private ServiceRegistration registration;
        private List<ManagedObject> list = Collections.synchronizedList(new ArrayList<ManagedObject>());
        
        public ConfigurationWatcher() {
        }
        
        public void updated(Dictionary props) throws ConfigurationException {
            synchronized (list) {
                for (ManagedObject cm : list) {
                    cm.updated(props);
                }
            }
        }
        
        private void setRegistration(ServiceRegistration registration) {
            this.registration = registration;
        }
        
        private ServiceRegistration getRegistration() {
            return registration;
        }
        
        private void add(ManagedObject cm) {
            list.add(cm);
        }
        
        private void remove(ManagedObject cm) {
            list.remove(cm);
        }
        
        private boolean isEmpty() {
            return list.isEmpty();
        }
    }
        
}
