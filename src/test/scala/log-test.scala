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

import com.github.nscala_time.time.Imports._

class LogSuite extends FunSuite {

  test("generated sql should be valid") {
    val sql = ParameterEmbeddedStyleSqlLogger.createMessage(
      "select * from message where id = ? and user_name = ? and created_at < ?",
      1, "abc", new DateTime(2013, 8, 16, 23, 9, 0, 0)
    )

    assert(sql === "select * from message where id = 1 and user_name = 'abc' and created_at < '2013-08-16 23:09:00'")
  }
}

