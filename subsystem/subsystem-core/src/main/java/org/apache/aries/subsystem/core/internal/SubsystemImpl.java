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
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.aries.subsystem.Subsystem;
import org.apache.aries.subsystem.SubsystemException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;
import org.osgi.service.composite.CompositeBundle;

public class SubsystemImpl implements Subsystem {

    final long id;
    final SubsystemAdminImpl admin;
    final CompositeBundle composite;

    public SubsystemImpl(SubsystemAdminImpl admin, CompositeBundle composite) {
        this.admin = admin;
        this.composite = composite;
        this.id = composite.getBundleId();
    }

    public State getState() {
        switch (composite.getState())
        {
            case Bundle.UNINSTALLED:
                return State.UNINSTALLED;
            case Bundle.INSTALLED:
                return State.INSTALLED;
            case Bundle.RESOLVED:
                return State.RESOLVED;
            case Bundle.STARTING:
                return State.STARTING;
            case Bundle.ACTIVE:
                return State.ACTIVE;
            case Bundle.STOPPING:
                return State.STOPPING;
        }
        throw new SubsystemException("Unable to retrieve subsystem state");
    }

    public void start() throws SubsystemException {
        try {
            composite.start();
        } catch (BundleException e) {
            throw new SubsystemException("Unable to start subsystem", e);
        }
    }

    public void stop() throws SubsystemException {
        try {
            composite.stop();
        } catch (BundleException e) {
            throw new SubsystemException("Unable to stop subsystem", e);
        }
    }

    public long getSubsystemId() {
        return composite.getBundleId();
    }

    public String getLocation() {
        return composite.getLocation();
    }

    public String getSymbolicName() {
        return composite.getSymbolicName();
    }

    public Version getVersion() {
        return composite.getVersion();
    }

    public String getScope() {
        return getSymbolicName() + "_" + getVersion().toString();
    }

    public Map<String, String> getHeaders() {
        return getHeaders(null);
    }

    public Map<String, String> getHeaders(String locale) {
        // TODO: retrieve headers
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Collection<Bundle> getConstituents() {
        List<Bundle> list = new ArrayList<Bundle>();
        Bundle[] bundles = composite.getSystemBundleContext().getBundles();
        for (Bundle bundle : bundles) {
            if (bundle.getBundleId() != 0) {
                list.add(bundle);
            }
        }
        return list;
    }
}