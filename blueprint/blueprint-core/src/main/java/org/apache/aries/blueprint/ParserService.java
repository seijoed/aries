/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.aries.blueprint;

import java.io.InputStream;
import java.net.URL;
import java.util.List;

import org.osgi.framework.Bundle;

public interface ParserService {
  
  /**
   * Parse a single InputStream containing blueprint xml. No validation will be performed. The caller
   * is responsible for closing the InputStream afterwards.  
   * @param is           InputStream containing blueprint xml. 
   * @param clientBundle The client's bundle 
   * @return             ComponentDefinitionRegistry containing metadata generated by the parser. 
   * @throws Exception
   */
  ComponentDefinitionRegistry parse (InputStream is, Bundle clientBundle) throws Exception;
  
  /**
   * Parse a single InputStream containing blueprint xml. The caller is responsible for 
   * closing the InputStream afterwards.  
   * @param is           Input stream containing blueprint xml
   * @param clientBundle The client's bundle 
   * @param validate     Indicates whether or not to validate the blueprint xml
   * @return             ComponentDefinitionRegistry containing metadata generated by the parser. 
   * @throws Exception
   */
  ComponentDefinitionRegistry parse (InputStream is, Bundle clientBundle, boolean validate) throws Exception;
  
  /**
   * Parse blueprint xml referred to by a single URL. No validation will be performed. 
   * @param url          URL reference to the blueprint xml to parse
   * @param clientBundle The client's bundle
   * @return             ComponentDefinitionRegistry containing metadata generated by the parser.
   * @throws Exception
   */
  ComponentDefinitionRegistry parse (URL url, Bundle clientBundle) throws Exception;
  
  /**
   * Parse blueprint xml referred to by a single URL.
   * @param url          URL reference to the blueprint xml to parse
   * @param clientBundle The client's bundle
   * @param validate     Indicates whether or not to validate the blueprint xml
   * @return             ComponentDefinitionRegistry containing metadata generated by the parser.
   * @throws Exception
   */
  ComponentDefinitionRegistry parse (URL url, Bundle clientBundle, boolean validate) throws Exception;
  
  /**
   * Parse blueprint xml referred to by a list of URLs. No validation will be performed. 
   * @param urls         URL reference to the blueprint xml to parse
   * @param clientBundle The client's bundle
   * @return             ComponentDefinitionRegistry containing metadata generated by the parser.
   * @throws Exception
   */
	ComponentDefinitionRegistry parse (List<URL> urls, Bundle clientBundle) throws Exception;
	
  /**
   * Parse blueprint xml referred to by a list of URLs.
   * @param urls         URL reference to the blueprint xml to parse
   * @param clientBundle The client's bundle
   * @param validate     Indicates whether or not to validate the blueprint xml
   * @return             ComponentDefinitionRegistry containing metadata generated by the parser.
   * @throws Exception
   */
	ComponentDefinitionRegistry parse (List<URL> urls, Bundle clientBundle, boolean validate) throws Exception;
	
}
