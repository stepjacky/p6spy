/*
 * #%L
 * P6Spy
 * %%
 * Copyright (C) 2013 P6Spy
 * %%
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
 * #L%
 */
package com.p6spy.engine.spy;

import com.p6spy.engine.test.P6TestFramework;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class P6TestCallableStatement extends P6TestFramework {
  private static final Logger log = Logger.getLogger(P6TestCallableStatement.class);

  @Parameterized.Parameters(name = "{index}: {0}")
  public static Collection<Object[]> dbs() {
    Collection<Object[]> result;
    String dbList = (System.getProperty("DB") == null ? "HSQLDB" : System.getProperty("DB"));

    Object[] dbs = dbList.split(",");
    List<Object[]> dbsToTest = new ArrayList<Object[]>();
    for (int i = 0; i < dbs.length; i++) {
      //  Check against list of databases with stored procs
      // As procs become available for other databases, enable them here.
      if( Arrays.asList("Oracle","MySQL","HSQLDB").contains(dbs[i])) {
        dbsToTest.add(new Object[]{dbs[i]});
      } else {
        log.info("Skipping "+dbs[i]+" because stored procedures have not been created for testing");
      }
    }
    result = dbsToTest;

    return result;
  }


  public P6TestCallableStatement(String db) throws SQLException, IOException {
    super(db);
  }
  
  @Test
  public void testStoredProcedureNoResultSet() throws SQLException {
    this.clearLogEnties();

    // execute the statement
    String query = "{call test_proc(?,?,?)}";
    CallableStatement call = connection.prepareCall(query);
    call.registerOutParameter(3, Types.INTEGER);
    call.setInt(1, 1);
    call.setString(2, "hi");
    call.execute();
    int retVal = call.getInt(3);
    assertEquals(2, retVal);
    call.close();

    // the last log message should have the original query
    assertTrue(getLastLogEntry().contains(query));

    // verify that the bind parameters are resolved in the log message
    assertTrue(getLastLogEntry().contains("1,'hi'"));
  }

  @Test
  public void testNamedParameters() throws SQLException {
    this.clearLogEnties();
    
    String param1Name = "param1";
    String param2Name = "param2";
    String resultParamName = "result_param";
    
    if( "HSQLDB".equals(db) ) {
      // HSQLDB uses @p1, @p2, etc...  as the "names" of the parameters
      param1Name = "@p1";
      param2Name = "@p2";
      resultParamName = "@p3";
    }

    // execute the statement
    String query = "{call test_proc(?,?,?)}";
    CallableStatement call = connection.prepareCall(query);
    call.setInt(param1Name, 1);
    call.setString(param2Name, "hi");
    call.registerOutParameter(resultParamName, Types.INTEGER);
    call.execute();
    call.close();

    // the last log message should have the original query
    assertTrue(getLastLogEntry().contains(query));

    // for now, named parameters are not logged!
    //assertTrue(getLastLogEntry().contains("1,'hi'"));
  }

  @Test
  public void testStoredProcedureWithNullInputParameter() throws SQLException {
    this.clearLogEnties();

    // execute the statement
    String query = "{call test_proc(?,?,?)}";
    CallableStatement stmt = connection.prepareCall(query);
    stmt.registerOutParameter(3, Types.INTEGER);
    stmt.setInt(1, 1);
    stmt.setNull(2, Types.VARCHAR);
    stmt.execute();
    int retVal = stmt.getInt(3);
    assertEquals(2, retVal);
    stmt.close();

    // verify that the third parameter is NULL
    assertTrue(getLastLogEntry().contains("1,NULL"));
  }

}


