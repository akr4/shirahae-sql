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

import com.github.nscala_time.time.Imports._
import java.sql.{ Connection, Statement, PreparedStatement, ResultSet, SQLException }
import com.typesafe.scalalogging.slf4j.Logging


class Session(conn: Connection)(implicit sqlLogger: SqlLogger) extends Using with Logging {
  def select[A](sql: String, params: Any*)(f: Iterator[Row] => A): A = {
    using(conn.prepareStatement(sql)) { stmt =>
      updateParams(stmt, params: _*)
      sqlLogger.log(sql, params: _*)
      using(stmt.executeQuery) { rs => f(new RowIterator(this, rs)) }
    }
  }

  def selectOne[A](sql: String, params: Any*)(f: Row => A): Option[A] = {
    select(sql, params: _*) { rows =>
      if (rows.hasNext) Some(f(rows.next))
      else None
    }
  }

  /** TODO: support arbitary number of generated keys */
  def updateWithGeneratedKey(sql: String, params: Any*): Long = {
    using(conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) { stmt =>
      updateParams(stmt, params: _*)
      sqlLogger.log(sql, params: _*)

      stmt.executeUpdate()
      using(stmt.getGeneratedKeys) { rs =>
        rs.next()
        val id = rs.getLong(1)
        logger.debug(s"generated ID: ${id}")
        id
      }
    }
  }

  def update(sql: String, params: Any*) {
    using(conn.prepareStatement(sql)) { stmt =>
      updateParams(stmt, params: _*)
      sqlLogger.log(sql, params: _*)

      stmt.executeUpdate()
    }
  }

  private def updateParams(stmt: PreparedStatement, params: Any*) {
    for (pair <- params.zip(Stream.iterate(1)(_ + 1))) {
      pair match {
        case (None, n) =>
          val sqlType = stmt.getParameterMetaData.getParameterType(n)
          stmt.setNull(n, sqlType)
        case (null, n) =>
          val sqlType = stmt.getParameterMetaData.getParameterType(n)
          stmt.setNull(n, sqlType)
        case (p: String, n) => stmt.setString(n, p)
        case (p: Int, n) => stmt.setInt(n, p)
        case (p: Long, n) => stmt.setLong(n, p)
        case (p: DateTime, n) => stmt.setTimestamp(n, new java.sql.Timestamp(p.getMillis))
        case (p: Boolean, n) => stmt.setBoolean(n, p)
        case x =>
          val className = x._1.getClass.getName
          throw new IllegalArgumentException(s"unsupported type: ${className} ${x}")
      }
    }
  }
}

class Row(session: Session, rs: ResultSet) {
  def int(n: Int): Int = rs.getInt(n)
  def long(n: Int): Long = rs.getLong(n)
  def string(n: Int): String = rs.getString(n)
  def stringOpt(n: Int): Option[String] = opt(n)(string)
  def boolean(n: Int): Boolean = rs.getBoolean(n)
  def dateTime(n: Int): DateTime = new DateTime(rs.getTimestamp(n))
  def dateTimeOpt(n: Int): Option[DateTime] = opt(n)(dateTime)

  private def opt[A](n: Int)(f: Int => A): Option[A] = {
    val v = f(n)
    if (rs.wasNull) None
    else Some(v)
  }
}

class RowIterator(session: Session, rs: ResultSet) extends Iterator[Row] {
  private var _hasNext: Option[Boolean] = None
  def hasNext: Boolean = {
    _hasNext match {
      case Some(x) => x
      case None  =>
        val x = rs.next
        _hasNext = Some(x)
        x
    }
  }
  def next(): Row = {
    _hasNext = None
    new Row(session, rs)
  }
}

