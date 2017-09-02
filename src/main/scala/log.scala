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

import com.typesafe.scalalogging.LazyLogging
import com.github.nscala_time.time.Imports._
import net.physalis.shirahae.EmbeddedParameterStyleSqlLogger.replace

trait SqlLogger {
  def log(sql: String, params: Parameter[_]*)
}

object SimpleSqlLogger extends SqlLogger with LazyLogging {
  def log(sql: String, params: Parameter[_]*) {
    val s = (sql :: params.zipWithIndex.map { x => s"${x._2 + 1}: ${x._1}" }.toList).mkString("\n")
    logger.debug(s)
  }
}

object EmbeddedParameterStyleSqlLogger extends SqlLogger with LazyLogging {
  val R = """\?""".r
  val lineBreakR = """\n""".r
  val spaceR = """\s+""".r

  def log(sql: String, params: Parameter[_]*) {
    logger.debug(createMessage(sql, params.toList))
  }

  protected [shirahae] def createMessage(sql: String, params: List[Parameter[_]]): String = {
    val s1 = lineBreakR.replaceAllIn(sql, " ")
    val s2 = spaceR.replaceAllIn(s1, " ")

    replace(s2, params)
  }

  @annotation.tailrec
  private def replace(s: String, params: List[Parameter[_]]): String = {
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
      case p: Parameter[_] => expr(p.value)
      case None => "null"
      case Some(p) => expr(p)
      case p: String => "'" + p + "'"
      case p: DateTime => "'" + p.toString("yyyy-MM-dd HH:mm:ss") + "'"
      case p => p.toString
    }
  }

}
