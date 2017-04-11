/*
 * Copyright 2013 Akira Ueda
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.physalis.shirahae

import java.sql.{ Connection, Driver, DriverManager }
import javax.sql.DataSource
import com.typesafe.scalalogging.LazyLogging


abstract class ConnectionFactory {
  def newConnection: Connection
}

class JdbcDriverConnectionFactory (
  driverClassName: String,
  url: String,
  username: String,
  password: String
) extends ConnectionFactory with LazyLogging {

  Class.forName(driverClassName)

  def newConnection: Connection = {
    val conn = DriverManager.getConnection(url)
    conn.setAutoCommit(false)
    conn
  }
}

class DataSourceConnectionFactory(ds: DataSource) extends ConnectionFactory {
  def newConnection: Connection = ds.getConnection
}
