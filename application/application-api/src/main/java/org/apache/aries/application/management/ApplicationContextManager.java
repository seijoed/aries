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

package org.apache.aries.application.management;

import java.util.Set;

/* We'll use this interface as a plug-point to the application-runtime */
public interface ApplicationContextManager {

  // Moved from AriesApplicationManager
  public ApplicationContext getApplicationContext(AriesApplication app);
  
  // Not sure who needs this or for what, so don't yet know if it'll need to 
  // be an immutable copy, or a live reference. 
  public Set<ApplicationContext> getApplicationContexts();
}