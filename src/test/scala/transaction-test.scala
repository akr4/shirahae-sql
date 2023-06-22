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

import org.scalatest.funsuite.AnyFunSuite
import org.mockito.Mockito._

import java.sql.Connection

class TransactionSuite extends AnyFunSuite {

  //implicit val sqlLogger = ParameterEmbeddedStyleSqlLogger
  implicit val sqlLogger = SimpleSqlLogger

  test("nested transaction should do nothing") {
    val conn = mock(classOf[Connection])
    val cf = mock(classOf[ConnectionFactory])

    when(cf.newConnection).thenReturn(conn)

    val tm = LocalTransactionManager

    tm.withTransaction(cf, { _ =>
      tm.withTransaction(cf, { _ => })
      tm.withTransaction(cf, { _ =>
        tm.withTransaction(cf, { _ => })
      })
    })

    verify(cf, times(1)).newConnection
    verify(conn, times(1)).commit
  }
}


