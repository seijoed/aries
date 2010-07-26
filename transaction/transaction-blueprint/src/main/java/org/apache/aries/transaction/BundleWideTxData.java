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
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class BundleWideTxData
{
  private String value;
  private List<Pattern> method = new ArrayList<Pattern>();
  private List<Pattern> bean = new ArrayList<Pattern>();
  
  public BundleWideTxData(String value,
          String method, String bean) {
      this.value = value;
      setupPatterns(method, bean);  
  }
 
  private void setupPatterns(String method, String bean) {
      String[] names = method.split("[, \t]");
      
      for (int i = 0; i < names.length; i++) {
          Pattern p = Pattern.compile(names[i].replaceAll("\\*", ".*"));
          this.method.add(p);
      }
      
      names = bean.split("[, \t]");
      
      for (int i = 0; i < names.length; i++) {
          Pattern p = Pattern.compile(names[i].replaceAll("\\*", ".*"));
          this.bean.add(p);
      }
  }
  public String getValue() {
      return this.value;
  }
  
  public List<Pattern> getMethod() {
      return this.method;
  }
  
  public List<Pattern> getBean() {
      return this.bean;
  }
}
