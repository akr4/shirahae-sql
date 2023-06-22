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

import scala.util.control.Exception._
import com.typesafe.scalalogging.LazyLogging

trait TransactionManager {
  def withTransaction[A](cf: ConnectionFactory, f: Session => A)(implicit sqlLogger: SqlLogger): A
}

/** TransactionManager which throws Exception
 * useful when you use outside transaction such as JTA
 */
object ErrorTransactionManager extends TransactionManager {
  def withTransaction[A](cf: ConnectionFactory, f: Session => A)(implicit sqlLogger: SqlLogger): A = {
    throw new UnsupportedOperationException
  }
}

/** TransactionManager which use Connection.commit/rollback */
object LocalTransactionManager extends TransactionManager with LazyLogging {
  private val underlyingSession: ThreadLocal[Option[Session]] = new ThreadLocal[Option[Session]] {
    override def initialValue: Option[Session] = None
  }

  def withTransaction[A](cf: ConnectionFactory, f: Session => A)(implicit sqlLogger: SqlLogger): A = {
    underlyingSession.get match {
      case Some(session) => f(session)
      case None =>
        logger.debug("getting new connection")
        val conn = cf.newConnection
        val session = new Session(conn)
        underlyingSession.set(Some(session))
        try {
          val result = f(session)
          logger.debug("committing transaction")
          conn.commit()
          result
        } catch {
          case e: Throwable =>
            logger.debug("rolling back transaction", e)
            allCatch { conn.rollback() }
          throw e
        } finally {
          logger.debug("closing connection")
          allCatch { conn.close() }
          underlyingSession.remove()
        }
    }
  }
}
