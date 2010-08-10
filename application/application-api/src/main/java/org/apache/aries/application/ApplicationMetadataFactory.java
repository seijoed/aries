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
package org.apache.aries.application;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Manifest;

/**
 * Provides various means of generating {@link org.apache.aries.application.ApplicationMetadata  
 * ApplicationMetadata} instances.  
 */
public interface ApplicationMetadataFactory
{

  /**
   * Parse from the input stream the application manifest. This method is more
   * lenient than the normal manifest parsing routine and does not limit the
   * manifest to 76 bytes as a line length.
   * 
   * @param in the inputstream of the manifest to parse.
   * @return   the parsed application metadata.
   * 
   */
  public ApplicationMetadata parseApplicationMetadata(InputStream in) throws IOException;
    
  /**
   * Create the application metadata from the provided Manifest. This is provided
   * so application metadata can be created from within the JVM. When reading
   * from a stream the parseApplication method should be used.
   * 
   * @param man the manifest to read from
   * @return    the parsed application metadata.
   */
  public ApplicationMetadata createApplicationMetadata(Manifest man);

}