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

import java.sql.{Connection, PreparedStatement, ResultSet, Statement}
import java.time.{Instant, LocalDate, LocalDateTime}
import com.github.nscala_time.time.{Imports => NST}
import com.typesafe.scalalogging.LazyLogging

import scala.language.implicitConversions

class TooManyRowsException(message: String = null, cause: Throwable = null) extends Exception

case class Parameter[A](value: A)

object Imports {
  type ConnectionFactory = net.physalis.shirahae.ConnectionFactory
  val LocalTransactionManager: net.physalis.shirahae.LocalTransactionManager.type = net.physalis.shirahae.LocalTransactionManager
  val ErrorTransactionManager: net.physalis.shirahae.ErrorTransactionManager.type = net.physalis.shirahae.ErrorTransactionManager
  val SimpleSqlLogger: net.physalis.shirahae.SimpleSqlLogger.type = net.physalis.shirahae.SimpleSqlLogger
  val EmbeddedParameterStyleSqlLogger: net.physalis.shirahae.EmbeddedParameterStyleSqlLogger.type = net.physalis.shirahae.EmbeddedParameterStyleSqlLogger
  type Database = net.physalis.shirahae.Database
  type Row = net.physalis.shirahae.Row

  implicit def convert(x: String): Parameter[String] = Parameter(x)
  implicit def convert(x: Int): Parameter[Int] = Parameter(x)
  implicit def convert(x: Long): Parameter[Long] = Parameter(x)
  implicit def convert(x: Float): Parameter[Float] = Parameter(x)
  implicit def convert(x: Double): Parameter[Double] = Parameter(x)
  implicit def convert(x: NST.DateTime): Parameter[NST.DateTime] = Parameter(x)
  implicit def convert(x: LocalDate): Parameter[LocalDate] = Parameter(x)
  implicit def convert(x: LocalDateTime): Parameter[LocalDateTime] = Parameter(x)
  implicit def convert(x: Instant): Parameter[Instant] = Parameter(x)
  implicit def convert(x: Boolean): Parameter[Boolean] = Parameter(x)
  implicit def convert[A](x: Option[A]): Parameter[Option[A]] = x match {
    case Some(y) => Parameter(Some(y))
    case None => Parameter(None)
  }
}

class Session(conn: Connection)(implicit sqlLogger: SqlLogger) extends Using with LazyLogging {
  def select[A](sql: String, params: Parameter[_]*)(f: Iterator[Row] => A): A = {
    using(conn.prepareStatement(sql)) { stmt =>
      updateParams(stmt, params: _*)
      sqlLogger.log(sql, params: _*)
      using(stmt.executeQuery) { rs => f(new RowIterator(this, rs)) }
    }
  }

  /** get a row as Option
   * @throws TooManyRowsException in case of more than one rows found
   */
  def selectOne[A](sql: String, params: Parameter[_]*)(f: Row => A): Option[A] = {
    select(sql, params: _*) { rows =>
      if (rows.hasNext) {
        val nextRow = f(rows.next())
        if (rows.hasNext) throw new TooManyRowsException
        Some(nextRow)
      } else {
        None
      }
    }
  }

  /** TODO: support arbitrary number of generated keys */
  def updateWithGeneratedKey(sql: String, params: Parameter[_]*): Long = {
    using(conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) { stmt =>
      updateParams(stmt, params: _*)
      sqlLogger.log(sql, params: _*)

      stmt.executeUpdate()
      using(stmt.getGeneratedKeys) { rs =>
        rs.next()
        val id = rs.getLong(1)
        logger.debug(s"generated ID: $id")
        id
      }
    }
  }

  def updateWithMaybeGeneratedKey(sql: String, params: Parameter[_]*): Option[Long] = {
    using(conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) { stmt =>
      updateParams(stmt, params: _*)
      sqlLogger.log(sql, params: _*)

      stmt.executeUpdate()
      using(stmt.getGeneratedKeys) { rs =>
        if (rs.next()) {
          val id = rs.getLong(1)
          logger.debug(s"generated ID: $id")
          Some(id)
        } else {
          None
        }
      }
    }
  }

  def update(sql: String, params: Parameter[_]*): Int = {
    using(conn.prepareStatement(sql)) { stmt =>
      updateParams(stmt, params: _*)
      sqlLogger.log(sql, params: _*)

      stmt.executeUpdate()
    }
  }

  private def updateParams(stmt: PreparedStatement, params: Parameter[_]*): Unit = {
    for ((param, position) <- params.zip(LazyList.iterate(1)(_ + 1))) {
      updateParam(stmt, param, position)
    }
  }

  @annotation.tailrec
  private def updateParam(stmt: PreparedStatement, param: Parameter[_], position: Int): Unit = {
    def setNull(stmt: PreparedStatement, position: Int): Unit = {
      stmt.setNull(position, stmt.getParameterMetaData.getParameterType(position))
    }

    if (param == null) {
      setNull(stmt, position)
    } else {
      param match {
        case Parameter(Some(x)) => updateParam(stmt, Parameter(x), position)
        case Parameter(None) => setNull(stmt, position)
        case Parameter(p: String) => stmt.setString(position, p)
        case Parameter(p: Int) => stmt.setInt(position, p)
        case Parameter(p: Long) => stmt.setLong(position, p)
        case Parameter(p: Float) => stmt.setFloat(position, p)
        case Parameter(p: Double) => stmt.setDouble(position, p)
        case Parameter(p: NST.DateTime) => stmt.setTimestamp(position, new java.sql.Timestamp(p.getMillis))
        case Parameter(p: LocalDate) => stmt.setTimestamp(position, java.sql.Timestamp.valueOf(p.atStartOfDay()))
        case Parameter(p: LocalDateTime) => stmt.setTimestamp(position, java.sql.Timestamp.valueOf(p))
        case Parameter(p: Instant) => stmt.setTimestamp(position, java.sql.Timestamp.from(p))
        case Parameter(p: Boolean) => stmt.setBoolean(position, p)
        case Parameter(x) =>
          val className = x.getClass.getName
          throw new IllegalArgumentException(s"unsupported type: $className $x")
      }
    }
  }
}

class Row(session: Session, rs: ResultSet) {
  def int(n: Int): Int = rs.getInt(n)
  def int(c: String): Int = rs.getInt(c)
  def intOpt(n: Int): Option[Int] = opt(n)(int)
  def intOpt(c: String): Option[Int] = opt(c)(int)
  def long(n: Int): Long = rs.getLong(n)
  def long(c: String): Long = rs.getLong(c)
  def longOpt(n: Int): Option[Long] = opt(n)(long)
  def longOpt(c: String): Option[Long] = opt(c)(long)
  def float(n: Int): Float = rs.getFloat(n)
  def float(c: String): Float = rs.getFloat(c)
  def floatOpt(n: Int): Option[Float] = opt(n)(float)
  def floatOpt(c: String): Option[Float] = opt(c)(float)
  def double(n: Int): Double = rs.getDouble(n)
  def double(c: String): Double = rs.getDouble(c)
  def doubleOpt(n: Int): Option[Double] = opt(n)(double)
  def doubleOpt(c: String): Option[Double] = opt(c)(double)
  def string(n: Int): String = rs.getString(n)
  def string(c: String): String = rs.getString(c)
  def stringOpt(n: Int): Option[String] = opt(n)(string)
  def stringOpt(c: String): Option[String] = opt(c)(string)
  def boolean(n: Int): Boolean = rs.getBoolean(n)
  def boolean(c: String): Boolean = rs.getBoolean(c)
  def booleanOpt(n: Int): Option[Boolean] = opt(n)(boolean)
  def booleanOpt(c: String): Option[Boolean] = opt(c)(boolean)
  def dateTime(n: Int): NST.DateTime = new NST.DateTime(rs.getTimestamp(n))
  def dateTime(c: String): NST.DateTime = new NST.DateTime(rs.getTimestamp(c))
  def dateTimeOpt(n: Int): Option[NST.DateTime] = opt(n)(dateTime)
  def dateTimeOpt(c: String): Option[NST.DateTime] = opt(c)(dateTime)
  def localDate(n: Int): LocalDate = Option(rs.getTimestamp(n)).map(_.toLocalDateTime.toLocalDate).orNull
  def localDate(c: String): LocalDate = Option(rs.getTimestamp(c)).map(_.toLocalDateTime.toLocalDate).orNull
  def localDateOpt(n: Int): Option[LocalDate] = opt(n)(localDate)
  def localDateOpt(c: String): Option[LocalDate] = opt(c)(localDate)
  def localDateTime(n: Int): LocalDateTime = Option(rs.getTimestamp(n)).map(_.toLocalDateTime).orNull
  def localDateTime(c: String): LocalDateTime = Option(rs.getTimestamp(c)).map(_.toLocalDateTime).orNull
  def localDateTimeOpt(n: Int): Option[LocalDateTime] = opt(n)(localDateTime)
  def localDateTimeOpt(c: String): Option[LocalDateTime] = opt(c)(localDateTime)
  def instant(n: Int): Instant = Option(rs.getTimestamp(n)).map(_.toInstant).orNull
  def instant(c: String): Instant = Option(rs.getTimestamp(c)).map(_.toInstant).orNull
  def instantOpt(n: Int): Option[Instant] = opt(n)(instant)
  def instantOpt(c: String): Option[Instant] = opt(c)(instant)
  def any(n: Int): Any = rs.getObject(n)
  def any(c: String): Any = rs.getObject(c)
  def anyOpt(n: Int): Option[Any] = opt(n)(any)
  def anyOpt(c: String): Option[Any] = opt(c)(any)

  private def opt[A](n: Int)(f: Int => A): Option[A] = {
    val v = f(n)
    if (rs.wasNull) None
    else Some(v)
  }

  private def opt[A](c: String)(f: String => A): Option[A] = {
    val v = f(c)
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
