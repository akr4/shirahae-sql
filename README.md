shirahae-sql
=====================

Very simple SQL client library for Scala


Usage
=====

```scala
import net.physalis.shirahae.Database

val db = Database.forDriver("org.postgresql.Driver", "jdbc:postgresql://localhost:5432/neko")

case class Employee(id: Int, name: String)

val employeeList = db.withTransaction { session =>
  session.select("select id, name from emp where dept_id = ?", 1) { rows =>
    rows.map { row =>
      Employee(row.int(1), row.string(2))
    }
  }.toList
}

val count = db.withTransaction { session =>
  session.selectOne("select count(*) from emp") { row =>
    row.int(1)
  }.get
}

db.withTransaction { session =>
  val id = session.updateWithGeneratedKey(
    "insert into emp (name, dept_id) values (?, ?)", "akira", 1)
  session.update("update emp set dept_id = ? where id = ?", 2, id)
  session.update("delete from emp where id = ?", id)
}
```

shirahae?
=========
shirahae = 白南風
