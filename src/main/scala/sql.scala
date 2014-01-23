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

class TooManyRowsException(message: String = null, cause: Throwable = null) extends Exception

class Session(conn: Connection)(implicit sqlLogger: SqlLogger) extends Using with Logging {
  def select[A](sql: String, params: Any*)(f: Iterator[Row] => A): A = {
    using(conn.prepareStatement(sql)) { stmt =>
      updateParams(stmt, params: _*)
      sqlLogger.log(sql, params: _*)
      using(stmt.executeQuery) { rs => f(new RowIterator(this, rs)) }
    }
  }

  /** get a row as Option
   * @throws TooManyRowsException in case of more than one rows found
   */
  def selectOne[A](sql: String, params: Any*)(f: Row => A): Option[A] = {
    select(sql, params: _*) { rows =>
      if (rows.hasNext) {
        val nextRow = f(rows.next)
        if (rows.hasNext) throw new TooManyRowsException
        Some(nextRow)
      } else {
        None
      }
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
    for ((param, position) <- params.zip(Stream.iterate(1)(_ + 1))) {
      updateParam(stmt, param, position)
    }
  }

  @annotation.tailrec
  private def updateParam(stmt: PreparedStatement, param: Any, position: Int) {
    def setNull(stmt: PreparedStatement, position: Int) {
      stmt.setNull(position, stmt.getParameterMetaData.getParameterType(position))
    }
    param match {
      case Some(x) => updateParam(stmt, x, position)
      case None => setNull(stmt, position)
      case null => setNull(stmt, position)
      case p: String => stmt.setString(position, p)
      case p: Int => stmt.setInt(position, p)
      case p: Long => stmt.setLong(position, p)
      case p: Float => stmt.setFloat(position, p)
      case p: Double => stmt.setDouble(position, p)
      case p: DateTime => stmt.setTimestamp(position, new java.sql.Timestamp(p.getMillis))
      case p: Boolean => stmt.setBoolean(position, p)
      case x =>
        val className = x.getClass.getName
        throw new IllegalArgumentException(s"unsupported type: ${className} ${x}")
    }
  }
}

class Row(session: Session, rs: ResultSet) {
  def int(n: Int): Int = rs.getInt(n)
  def long(n: Int): Long = rs.getLong(n)
  def float(n: Int): Float = rs.getFloat(n)
  def double(n: Int): Double = rs.getDouble(n)
  def string(n: Int): String = rs.getString(n)
  def stringOpt(n: Int): Option[String] = opt(n)(string)
  def boolean(n: Int): Boolean = rs.getBoolean(n)
  def dateTime(n: Int): DateTime = new DateTime(rs.getTimestamp(n))
  def dateTimeOpt(n: Int): Option[DateTime] = opt(n)(dateTime)
  def any(n: Int): Any = rs.getObject(n)
  def anyOpt(n: Int): Option[Any] = opt(n)(any)

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

