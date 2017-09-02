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

import scala.util.Try
import org.scalatest.FunSuite
import org.mockito.Mockito._
import Imports._

import com.github.nscala_time.time.Imports._

class LogSuite extends FunSuite {

  test("test types") {
    val now = new DateTime(2013, 8, 16, 23, 9, 0, 0)
    val sql = EmbeddedParameterStyleSqlLogger.createMessage(
      """|select * from test
         |where c_int = ?
         |  and c_long = ?
         |  and c_float = ?
         |  and c_double = ?
         |  and c_string = ?
         |  and c_boolean = ?
         |  and c_datetime = ?
         |  and c_null = ?
         |  and c_none = ?
         |  and c_some = ?""".stripMargin,
      List(1, 2, 3.0, 4.0, "abc", true, now, null, None, Some(3))
    )

    assert(sql ===
      """select * from test"""
        + """ where c_int = 1 and c_long = 2 and c_float = 3.0 and c_double = 4.0"""
        + """ and c_string = 'abc' and c_boolean = true and c_datetime = '2013-08-16 23:09:00'"""
        + """ and c_null = null and c_none = null and c_some = 3""".stripMargin)
  }

  test("generated sql should be valid") {
    val sql = EmbeddedParameterStyleSqlLogger.createMessage(
      "select * from message where id = ? and user_name = ? and created_at < ?",
      List(1, "abc", new DateTime(2013, 8, 16, 23, 9, 0, 0))
    )

    assert(sql === "select * from message where id = 1 and user_name = 'abc' and created_at < '2013-08-16 23:09:00'")
  }

  test("log works without error") {
    EmbeddedParameterStyleSqlLogger.log(
      "select * from message where id = ? and user_name = ? and created_at < ?",
      1, "abc", new DateTime(2013, 8, 16, 23, 9, 0, 0)
    )
  }

  test("log works with single argument without error") {
    EmbeddedParameterStyleSqlLogger.log("select * from message where id = ?", 1)
  }
}

