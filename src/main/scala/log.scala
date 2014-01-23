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

import com.typesafe.scalalogging.slf4j.Logging
import com.github.nscala_time.time.Imports._

trait SqlLogger {
  def log(sql: String, params: Any*)
}

object SimpleSqlLogger extends SqlLogger with Logging {
  def log(sql: String, params: Any*) {
    val s = (sql :: params.zipWithIndex.map { x => s"${x._2 + 1}: ${x._1}" }.toList).mkString("\n")
    logger.debug(s)
  }
}

object EmbeddedParameterStyleSqlLogger extends SqlLogger with Logging {
  val R = """\?""".r

  def log(sql: String, params: Any*) {
    logger.debug(createMessage(sql, params.toList))
  }

  def createMessage(sql: String, params: List[Any]): String = {
    replace(sql, params)
  }

  @annotation.tailrec
  private def replace(s: String, params: List[Any]): String = {
    params match {
      case p :: ps => {
        val replaced = R.replaceFirstIn(s, expr(p))
        replace(replaced, ps)
      }
      case Nil => s
    }
  }

  @annotation.tailrec
  private def expr(param: Any): String = {
    param match {
      case null => "null"
      case None => "null"
      case Some(p) => expr(p)
      case p: String => "'" + p + "'"
      case p: DateTime => "'" + p.toString("yyyy-MM-dd HH:mm:ss") + "'"
      case p => p.toString
    }
  }

}
