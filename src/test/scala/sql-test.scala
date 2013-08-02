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
import com.github.nscala_time.time.Imports._
import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfter

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
        |  c_varchar varchar(10),
        |  c_timestamp timestamp
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

  test("selectOne should return first record even if there are more") {
    prepareEmp
    val result = db.withTransaction { _.selectOne("select id from emp") { _.int(1) }}
    assert(result === Some(1))
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
      session.update("insert into test values (?, ?, ?)", null, null, null)
    }

    val result = db.withTransaction { _.selectOne("select * from test") {
      row => (row.intOpt(1), row.stringOpt(2), row.dateTimeOpt(3))
    }}

    assert(result.isDefined)
    assert(result.get._1 === None)
    assert(result.get._2 === None)
    assert(result.get._3 === None)
  }

  test("can insert none") {
    prepareTestTable
    db.withTransaction { session =>
      session.update("insert into test values (?, ?, ?)", None, None, None)
    }

    val result = db.withTransaction { _.selectOne("select * from test") {
      row => (row.intOpt(1), row.stringOpt(2), row.dateTimeOpt(3))
    }}

    assert(result.isDefined)
    assert(result.get._1 === None)
    assert(result.get._2 === None)
    assert(result.get._3 === None)
  }

  test("can get values") {
    prepareTestTable
    val now = DateTime.now
    db.withTransaction { session =>
      session.update("insert into test values (?, ?, ?)", 1, "abc", now)
    }

    val result = db.withTransaction { _.selectOne("select * from test") {
      row => (row.int(1), row.string(2), row.dateTime(3))
    }}

    assert(result.isDefined)
    assert(result.get._1 === 1)
    assert(result.get._2 === "abc")
    assert(result.get._3 === now)
  }

  test("can get opt values") {
    prepareTestTable
    val now = DateTime.now
    db.withTransaction { session =>
      session.update("insert into test values (?, ?, ?)", 1, "abc", now)
    }

    val result = db.withTransaction { _.selectOne("select * from test") {
      row => (row.intOpt(1), row.stringOpt(2), row.dateTimeOpt(3))
    }}

    assert(result.isDefined)
    assert(result.get._1 === Some(1))
    assert(result.get._2 === Some("abc"))
    assert(result.get._3 === Some(now))
  }
}

