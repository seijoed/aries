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
package org.apache.aries.application.management.spi.repository;

import java.net.URI;
import java.util.Collection;

/**
 * This interface allows one to find out information about configured bundle repositories
 *
 */
public interface PlatformRepository {

  
  /**
   * Obtain a set of URIs to bundle repositories representing the local platform's capabilities. 
   * These repositories do not represent any bundles but only platform capabilities.   
   * @return URLs to bundle repositories representing the local platform 
   */
  Collection<URI> getPlatformRepositoryURLs();
}
