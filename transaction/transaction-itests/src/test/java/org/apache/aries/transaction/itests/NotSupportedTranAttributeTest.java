/*  Licensed to the Apache Software Foundation (ASF) under one or more
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
package org.apache.aries.transaction.itests;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;

import javax.transaction.UserTransaction;

import org.apache.aries.transaction.test.TestBean;
import org.junit.Test;

public class NotSupportedTranAttributeTest extends AbstractIntegrationTest {
  
  @Test
  public void testNotSupported() throws Exception {
      TestBean nsBean = getOsgiService(TestBean.class, "(tranAttribute=NotSupported)", DEFAULT_TIMEOUT);
      TestBean rBean = getOsgiService(TestBean.class, "(tranAttribute=Required)", DEFAULT_TIMEOUT);
      UserTransaction tran = getOsgiService(UserTransaction.class);
      
      //Test with client transaction - the insert fails because the bean delegates to another
      //bean with a transaction strategy of Mandatory, and no transaction is available, i.e.
      //the user transaction is not propagated, and there is no container transaction.
      int initialRows = nsBean.countRows();
      
      tran.begin();
      
      try {
          nsBean.insertRow("testWithClientTran", 1, true);
          fail("IllegalStateException not thrown");
      } catch (IllegalStateException e) {
          e.printStackTrace();
      }
      
      tran.commit();
      
      int finalRows = nsBean.countRows();
      assertTrue("Initial rows: " + initialRows + ", Final rows: " + finalRows, finalRows - initialRows == 0);
      
      //Test with client transaction and with application exception - the user transaction is not
      //marked for rollback and can still be committed
      initialRows = nsBean.countRows();
      
      tran.begin();
      rBean.insertRow("testWithClientTranAndWithAppException", 1);
      
      try {
          nsBean.throwApplicationException();
      } catch (SQLException e) {
          e.printStackTrace();
      }
      
      tran.commit();
      
      finalRows = nsBean.countRows();
      assertTrue("Initial rows: " + initialRows + ", Final rows: " + finalRows, finalRows - initialRows == 1);
      
      //Test with client transaction and with runtime exception - the user transaction is not
      //marked for rollback and can still be committed
      initialRows = nsBean.countRows();
      
      tran.begin();
      rBean.insertRow("testWithClientTranAndWithRuntimeException", 1);
      
      try {
          nsBean.throwRuntimeException();
      } catch (RuntimeException e) {
          e.printStackTrace();
      }
      
      tran.commit();
      
      finalRows = nsBean.countRows();
      assertTrue("Initial rows: " + initialRows + ", Final rows: " + finalRows, finalRows - initialRows == 1);
      
      //Test without client transaction - the insert fails because the bean delegates to another
      //bean with a transaction strategy of Mandatory, and no transaction is available
      initialRows = nsBean.countRows();
      
      try {
          nsBean.insertRow("testWithoutClientTran", 1, true);
          fail("IllegalStateException not thrown");
      } catch (IllegalStateException e) {
          e.printStackTrace();
      }
      
      finalRows = nsBean.countRows();
      assertTrue("Initial rows: " + initialRows + ", Final rows: " + finalRows, finalRows - initialRows == 0);
  }
}
