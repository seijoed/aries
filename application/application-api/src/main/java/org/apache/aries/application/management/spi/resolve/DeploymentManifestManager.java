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
package org.apache.aries.application.management.spi.resolve;

import java.util.Collection;
import java.util.jar.Manifest;

import org.apache.aries.application.Content;
import org.apache.aries.application.ServiceDeclaration;
import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.ResolveConstraint;
import org.apache.aries.application.management.ResolverException;
import org.apache.aries.application.modelling.DeployedBundles;
import org.apache.aries.application.modelling.ModelledResource;

public interface DeploymentManifestManager
{
  /**
   * Generate the deployment manifest map for the application. The method is designed to be used when installing an application.
   * @param app The Aries application
   * @param constraints the constraints, used to narrow down the deployment content
   * @return the deployment manifest 
   * @throws ResolverException
   */
  Manifest generateDeploymentManifest( AriesApplication app, ResolveConstraint... constraints ) throws ResolverException;
  
  
  /**
   * Generate the deployment manifest map. The method can be used for some advanced scenarios.
   * @param appMeta The Aries application metadata
   * @param byValueBundles By value bundles
   * @param useBundleSet Use Bundle set
   * @param otherBundles Other bundles to be used to narrow the resolved bundles
   * @param appImportServices the Application-ImportService header
   * @return DeployedBundles model of the deployed application
   * @throws ResolverException
   */
  DeployedBundles generateDeployedBundles( 
      String appName, 
      String appVersion, 
      Collection<Content> appContent, 
      Collection<ModelledResource> byValueBundles, 
      Collection<Content> useBundleSet, 
      Collection<Content> otherBundles, 
      Collection<ServiceDeclaration> appImportServices) throws ResolverException;

  /**
   * Generate a Manifest representation of a DEPLOYMENT.MF, 
   * suitable for example to writing to disk
   * @param appSymbolicName
   * @param appVersion
   * @param deployedBundles Such as obtained from generateDeployedBundles()
   * @return
   * @throws ResolverException
   */
  Manifest generateDeploymentManifest (
      String appSymbolicName, 
      String appVersion, 
      DeployedBundles deployedBundles) throws ResolverException; 
       
}
