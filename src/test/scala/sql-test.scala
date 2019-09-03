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

import java.time.{Instant, LocalDateTime}

import scala.util.Try
import com.github.nscala_time.time.{Imports => NST}
import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfter
import Imports._

class SqlSuite extends FunSuite with BeforeAndAfter {

  val db = Database.forDriver("org.hsqldb.jdbc.JDBCDriver", "jdbc:hsqldb:mem:hsqldb:test")


  def prepareEmp {
    Try { db.ddl("drop table emp") }
    db.ddl("create table emp (id integer primary key, name varchar(30))")
    db.withTransaction { s =>
      s.update("insert into emp (id, name) values (?, ?)", 1, "name1")
      s.update("insert into emp (id, name) values (?, ?)", 2, "name2")
    }
  }

  def prepareTestTable {
    Try { db.ddl("drop table test") }
    db.ddl(
      """create table test (
        |  c_integer integer,
        |  c_long bigint,
        |  c_float float,
        |  c_double double,
        |  c_boolean boolean,
        |  c_varchar varchar(10),
        |  c_timestamp1 timestamp,
        |  c_timestamp2 timestamp,
        |  c_timestamp_tz timestamp with time zone,
        |)""".stripMargin)
  }

  test("select should return existing records") {
    prepareEmp
    val result = db.withTransaction { _.select("select id, name from emp")(_.map(_.int(1)).toList) }
    assert(result.size === 2)
    assert(result(0) === 1)
    assert(result(1) === 2)
  }

  test("fold example") {
    prepareEmp
    val max = db.withTransaction {
      _.select("select id from emp") { rows =>
        // find max id
        rows.foldLeft(0) { (max, row) =>
          math.max(max, row.int(1))
        }
      }
    }
    assert(max === 2)
  }

  test("selectOne should return None if not found") {
    prepareEmp
    val result = db.withTransaction { _.selectOne("select * from emp where id = -1") { _ => }}
    assert(result === None)
  }

  test("selectOne should return Some only if one record found") {
    prepareEmp
    val result = db.withTransaction { _.selectOne("select id from emp where id = 1") { _.int(1) }}
    assert(result === Some(1))
  }

  test("selectOne should throw TooManyRowsException if there are more") {
    prepareEmp
    intercept[TooManyRowsException] {
      db.withTransaction { _.selectOne("select id from emp") { _.int(1) }}
    }
  }

  test("update can insert") {
    prepareEmp
    db.withTransaction { _.update("insert into emp (id, name) values (?, ?)", 100, "name100") }
    val name = db.withTransaction { _.selectOne("select name from emp where id = ?", 100) { _.string(1) }}.get
    assert(name === "name100")
  }

  test("update can update") {
    prepareEmp
    db.withTransaction { _.update("update emp set name = ? where id = 1", "name999") }
    val name = db.withTransaction { _.selectOne("select name from emp where id = ?", 1) { _.string(1) }}.get
    assert(name === "name999")
  }

  test("update can delete") {
    prepareEmp
    db.withTransaction { _.update("delete from emp where id = 1") }
    val name = db.withTransaction { _.selectOne("select name from emp where id = ?", 1) { _.string(1) }}
    assert(name === None)
  }

  test("updateWithGeneratedKey can insert") {
    db.ddl("drop table emp")
    db.ddl("create table emp (id identity primary key, name varchar(30))")
    val id = db.withTransaction { _.updateWithGeneratedKey("insert into emp (name) values (?)", "name") }
    val name = db.withTransaction { _.selectOne("select name from emp where id = ?", id) { _.string(1) }}.get
    assert(name === "name")
  }

  test("can insert null") {
    prepareTestTable
    db.withTransaction { session =>
      session.update("insert into test values (?, ?, ?, ?, ?, ?, ?, ?, ?)", null, null, null, null, null, null, null, null, null)
    }

    val result = db.withTransaction { _.selectOne("select * from test") {
      row => (row.intOpt(1), row.longOpt(2), row.floatOpt(3), row.doubleOpt(4), row.booleanOpt(5), row.stringOpt(6), row.dateTimeOpt(7), row.localDateTimeOpt(8), row.instantOpt(9))
    }}

    assert(result.isDefined)
    assert(result.get._1 === None)
    assert(result.get._2 === None)
    assert(result.get._3 === None)
    assert(result.get._4 === None)
    assert(result.get._5 === None)
    assert(result.get._6 === None)
    assert(result.get._7 === None)
    assert(result.get._8 === None)
    assert(result.get._9 === None)
  }

  test("can insert some") {
    prepareTestTable
    val nstNow = NST.DateTime.now
    val jtNow = LocalDateTime.now
    val instantNow = Instant.now
    db.withTransaction { session =>
      session.update("insert into test values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
        Some(1), Some(1L), Some(1.0), Some(1.0), Some(true), Some("abc"), Some(nstNow), Some(jtNow), Some(instantNow))
    }

    val result = db.withTransaction { _.selectOne("select * from test") {
      row => (row.int(1), row.long(2), row.float(3), row.double(4), row.boolean(5), row.stringOpt(6), row.dateTimeOpt(7), row.localDateTimeOpt(8), row.instantOpt(9))
    }}

    assert(result.isDefined)
    assert(result.get._1 === 1)
    assert(result.get._2 === 1L)
    assert(result.get._3 === 1.0)
    assert(result.get._4 === 1.0)
    assert(result.get._5 === true)
    assert(result.get._6 === Some("abc"))
    assert(result.get._7 === Some(nstNow))
    assert(result.get._8 === Some(jtNow))
    assert(result.get._9 === Some(instantNow))
  }

  test("can insert none") {
    prepareTestTable
    // StackOverFlowError in compilation without explicit type declaration (Scala 2.11.2)
    db.withTransaction { session =>
      session.update("insert into test values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
        None: Option[Int], None: Option[Long], None: Option[Float], None: Option[Double], None: Option[Boolean], None: Option[String], None: Option[NST.DateTime], None: Option[LocalDateTime], None: Option[Instant])
    }

    val result = db.withTransaction { _.selectOne("select * from test") {
      row => (row.int(1), row.long(2), row.float(3), row.double(4), row.boolean(5), row.stringOpt(6), row.dateTimeOpt(7), row.localDateTimeOpt(8), row.instantOpt(9))
    }}

    assert(result.isDefined)
    assert(result.get._1 === 0)
    assert(result.get._2 === 0L)
    assert(result.get._3 === 0.0)
    assert(result.get._4 === 0.0)
    assert(result.get._5 === false)
    assert(result.get._6 === None)
    assert(result.get._7 === None)
    assert(result.get._8 === None)
    assert(result.get._9 === None)
  }

  test("can get values") {
    prepareTestTable
    val nstNow = NST.DateTime.now
    val jtNow = LocalDateTime.now
    val instantNow = Instant.now
    db.withTransaction { session =>
      session.update("insert into test values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
        1, 1L, 1.0, 1.0, true, "abc", nstNow, jtNow, instantNow)
    }

    val result = db.withTransaction { _.selectOne("select * from test") {
      row => (row.int(1), row.long(2), row.float(3), row.double(4), row.boolean(5), row.string(6), row.dateTime(7), row.any(6), row.localDateTime(8), row.instant(9))
    }}

    assert(result.isDefined)
    assert(result.get._1 === 1)
    assert(result.get._2 === 1L)
    assert(result.get._3 === 1.0)
    assert(result.get._4 === 1.0)
    assert(result.get._5 === true)
    assert(result.get._6 === "abc")
    assert(result.get._7 === nstNow)
    assert(result.get._8 === "abc")  // row.any(6)
    assert(result.get._9 === jtNow)
    assert(result.get._10 === instantNow)
  }

  test("can get values by column name") {
    prepareTestTable
    val nstNow = NST.DateTime.now
    val jtNow = LocalDateTime.now
    val instantNow = Instant.now
    db.withTransaction { session =>
      session.update("insert into test values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
        1, 1L, 1.0, 1.0, true, "abc", nstNow, jtNow, instantNow)
    }

    val result = db.withTransaction { _.selectOne("select * from test") {
      row => (
        row.int("c_integer"),
        row.long("c_long"),
        row.float("c_float"),
        row.double("c_double"),
        row.boolean("c_boolean"),
        row.string("c_varchar"),
        row.dateTime("c_timestamp1"),
        row.any("c_varchar"),
        row.localDateTime("c_timestamp2"),
        row.instant("c_timestamp_tz")
      )
    }}

    assert(result.isDefined)
    assert(result.get._1 === 1)
    assert(result.get._2 === 1L)
    assert(result.get._3 === 1.0)
    assert(result.get._4 === 1.0)
    assert(result.get._5 === true)
    assert(result.get._6 === "abc")
    assert(result.get._7 === nstNow)
    assert(result.get._8 === "abc")  // row.any("c_varchar")
    assert(result.get._9 === jtNow)
    assert(result.get._10 === instantNow)
  }

  test("can get opt values") {
    prepareTestTable
    val now = NST.DateTime.now
    db.withTransaction { session =>
      session.update("insert into test (c_varchar, c_timestamp1) values (?, ?)", "abc", now)
    }

    val result = db.withTransaction { _.selectOne("select c_varchar, c_timestamp1 from test") {
      row => (row.stringOpt(1), row.dateTimeOpt(2), row.anyOpt(1))
    }}

    assert(result.isDefined)
    assert(result.get._1 === Some("abc"))
    assert(result.get._2 === Some(now))
    assert(result.get._3 === Some("abc"))
  }
}
