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

import java.sql.ResultSet

class RowIteratorSuite extends AnyFunSuite {

  test("iterate through resultset") {
    val session = mock(classOf[Session])

    val rs = mock(classOf[ResultSet])
    when(rs.next).
      thenReturn(true).
      thenReturn(true).
      thenReturn(false)
    when(rs.getInt(1)).
      thenReturn(1).
      thenReturn(2)

    val it = new RowIterator(session, rs)

    assert(it.hasNext === true)
    assert(it.next.int(1) === 1)
    assert(it.hasNext === true)
    assert(it.next.int(1) === 2)
    assert(it.hasNext === false)
  }

  test("hasNext calls multiple times") {
    val session = mock(classOf[Session])

    val rs = mock(classOf[ResultSet])
    when(rs.next).
      thenReturn(true).
      thenReturn(true).
      thenReturn(false)
    when(rs.getInt(1)).
      thenReturn(1).
      thenReturn(2)

    val it = new RowIterator(session, rs)

    assert(it.hasNext === true)
    assert(it.hasNext === true)
    assert(it.next.int(1) === 1)
    assert(it.hasNext === true)
    assert(it.hasNext === true)
    assert(it.next.int(1) === 2)
    assert(it.hasNext === false)
    assert(it.hasNext === false)
  }
}


