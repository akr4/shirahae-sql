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

import javax.sql.DataSource


class Database(tm: TransactionManager, cf: ConnectionFactory,
  implicit private val sqlLogger: SqlLogger = SimpleSqlLogger) extends Using {

  def ddl(sql: String): Unit = {
    withSession(_.update(sql))
  }

  def withSession[A](f: Session => A): A = {
    using(cf.newConnection) { conn =>
      f(new Session(conn))
    }
  }

  def withTransaction[A](f: Session => A): A = tm.withTransaction(cf, f)
}

/** convenient factory methods */
object Database {
  def forDataSource(ds: DataSource) =
    new Database(LocalTransactionManager, new DataSourceConnectionFactory(ds))

  def forDriver(driverClassName: String, url: String) =
    new Database(LocalTransactionManager, new JdbcDriverConnectionFactory(driverClassName, url))
}
