/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.aries.application.resolver.obr.impl;

import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;

import org.apache.aries.application.Content;
import org.apache.aries.application.management.BundleInfo;
import org.osgi.framework.Version;

public class OBRBundleInfo implements BundleInfo
{

  private final String symbolicName;
  private final Version version;
  private final String location;
  private final Set<Content> importPackage;
  private final Set<Content> exportPackage;
  private final Set<Content> importService;
  private final Set<Content> exportService;
  private final Map<String, String> headers;
  private final Set<Content> requireBundle;
  private final Map<String, String> attributes;
  private final Map<String, String> directives;

  public OBRBundleInfo(String symbolicName, Version version, String location,
                       Set<Content> importPackage, Set<Content> exportPackage,
                       Set<Content> importService, Set<Content> exportService,
                       Set<Content> requireBundle, Map<String, String> attributes,
                       Map<String, String> directives, Map<String, String> headers)
  {
    this.symbolicName = symbolicName;
    this.version = version;
    this.location = location;
    this.importPackage = importPackage;
    this.exportPackage = exportPackage;
    this.importService = importService;
    this.exportService = exportService;
    this.headers = headers;
    this.requireBundle = requireBundle;
    this.attributes = attributes;
    this.directives = directives;
  }

  public String getSymbolicName()
  {
    return symbolicName;
  }

  public Version getVersion()
  {
    return version;
  }

  public String getLocation()
  {
    return location;
  }

  public Set<Content> getImportPackage()
  {
    return importPackage;
  }

  public Set<Content> getExportPackage()
  {
    return exportPackage;
  }

  public Set<Content> getImportService()
  {
    return importService;
  }

  public Set<Content> getExportService()
  {
    return exportService;
  }

  public Map<String, String> getHeaders()
  {
    return headers;
  }

  public Map<String, String> getBundleAttributes()
  {
    return attributes;
  }

  public Map<String, String> getBundleDirectives()
  {
    return directives;
  }

  public Set<Content> getRequireBundle()
  {
    return requireBundle;
  }
    
  public String toString()
  {
    return symbolicName + "_" + version;
  }

  public int hashCode() 
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + symbolicName.hashCode();
    result = prime * result + version.hashCode();
    return result;
  }

  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) {
        return false;
    }
    OBRBundleInfo other = (OBRBundleInfo) obj;
    return (symbolicName.equals(other.symbolicName)
            && version.equals(other.version));
  }
  
  public Attributes getRawAttributes()
  {

    Attributes _attributes = new Attributes();
    _attributes.putAll(attributes);
    _attributes.putAll(directives);
    return _attributes;
  }
}
