package io.cat.ai.zio.ssh.jcraft

import java.io.IOException
import java.util.Properties

import com.jcraft.jsch.{JSch, JSchException, Session => JSchSession}

import zio._

import scala.util.Try

object SshSession {

  type Session = Has[SshSession.SessionService]

  trait SessionService extends Serializable {

    def create(usr: String, host: String, port: Int): IO[Throwable, JSchSession]

    def createWithProperties(usr: String, host: String, port: Int, password: String, props: Properties): IO[Throwable, JSchSession]

    def createWithKnowHosts(usr: String, host: String, port: Int, password: String, knownHosts: String): IO[Throwable, JSchSession]

    def createWithPropsAndKnowHosts(usr: String, host: String, port: Int, password: String, props: Properties, knownHosts: String): IO[Throwable, JSchSession]

    def createConnection(session: JSchSession, timeout: Int): IO[Throwable, JSchSession]
  }

  object SessionService {

    import internal._

    val default: SessionService = new SessionService {

      def create(usr: String, host: String, port: Int): IO[IOException, JSchSession] =
        IO.effect { internal.newJCraftSession(usr, host, port) }.refineToOrDie[IOException]

      def createWithProperties(usr: String, host: String, port: Int, password: String, props: Properties): IO[IOException, JSchSession] =
        IO.effect { internal.newJCraftSession(usr, host, port, password, props) }.refineToOrDie[IOException]

      def createWithKnowHosts(usr: String, host: String, port: Int, password: String, knownHosts: String): IO[IOException, JSchSession] =
        IO.effect { internal.newJCraftSessionWithKnowHosts(usr, host, port, password, knownHosts) }.refineToOrDie[IOException]

      def createWithPropsAndKnowHosts(usr: String, host: String, port: Int, password: String, props: Properties, knownHosts: String): IO[IOException, JSchSession] =
        IO.effect { internal.newJCraftSessionWithPropsAndKnowHosts(usr, host, port, password, props, knownHosts) }.refineToOrDie[IOException]

      def createConnection(session: JSchSession, timeout: Int): IO[IOException, JSchSession] = IO.effect { session.establishConnection(timeout) }.refineToOrDie[IOException]
    }
  }

  object internal {

    def handleJschActionAndThrowIoExc[A](action: => A): A = try action catch { case ex: JSchException => throw new IOException(ex) }

    implicit class JCraftSessionConnection(session: JSchSession) {
      def establishConnection(timeout: Int): JSchSession =
        handleJschActionAndThrowIoExc {
          session.connect(timeout)
          session
        }
    }

    def sessionConnection(session: JSchSession, timeout: Int): Try[JSchSession] =
      Try {
        session.connect(timeout)
        session
      }

    def newJCraftSession(usr: String, host: String, port: Int): JSchSession =
      try new JSch().getSession(usr, host, port) catch { case ex: JSchException => throw new IOException(ex) }

    def newJCraftSession(usr: String, host: String, port: Int, password: String, props: Properties): JSchSession =
      handleJschActionAndThrowIoExc {
        val jsch = new JSch
        val session = jsch.getSession(usr, host, port)
        session.setPassword(password)
        session.setConfig(props)

        session
      }

    def newJCraftSessionWithKnowHosts(usr: String, host: String, port: Int,  password: String, knownHosts: String): JSchSession =
      handleJschActionAndThrowIoExc {
        val jsch = new JSch
        jsch.setKnownHosts(knownHosts)
        val session = jsch.getSession(usr, host, port)
        session.setPassword(password)

        session
      }

    def newJCraftSessionWithPropsAndKnowHosts(usr: String, host: String, port: Int, password: String, props: Properties, knownHosts: String): JSchSession =
      handleJschActionAndThrowIoExc {
        val jsch = new JSch
        jsch.setKnownHosts(knownHosts)
        val session = jsch.getSession(usr, host, port)
        session.setPassword(password)
        session.setConfig(props)

        session
      }
  }

  val default: Layer[Nothing, Session] = ZLayer.succeed(SessionService.default)

  val any: ZLayer[Session, Nothing, Session] = ZLayer.requires[Session]

  // experimental
  def createAccessM(usr: String, host: String, port: Int): ZIO[Session, Throwable, JSchSession] =
    ZIO.accessM { _.get create (usr, host, port) }

  def createWithPropertiesAccessM(usr: String, host: String, port: Int, password: String, props: Properties): ZIO[Session, Throwable, JSchSession] =
    ZIO.accessM { _.get createWithProperties (usr, host, port, password, props) }

  def createWithKnowHostsAccessM(usr: String, host: String, port: Int, password: String, knownHosts: String): ZIO[Session, Throwable, JSchSession] =
    ZIO.accessM { _.get createWithKnowHosts (usr, host, port, password, knownHosts) }

  def createWithPropsAndKnowHostsAccessM(usr: String, host: String, port: Int, password: String, props: Properties, knownHosts: String): ZIO[Session, Throwable, JSchSession] =
    ZIO.accessM { _.get createWithPropsAndKnowHosts (usr, host, port, password, props, knownHosts) }

  def createConnectionAccessM(session: JSchSession, timeout: Int): ZIO[Session, Throwable, JSchSession] =
    ZIO.accessM { _.get createConnection(session, timeout) }
  // experimental

  import internal._

  def create(usr: String, host: String, port: Int): IO[JSchException, JSchSession] = ZIO.effect { internal.newJCraftSession(usr, host, port) }.refineToOrDie[JSchException]

  def createWithProperties(usr: String, host: String, port: Int, password: String, props: Properties): IO[JSchException, JSchSession] =
    ZIO.effect { internal.newJCraftSession(usr, host, port, password, props) }.refineToOrDie[JSchException]

  def createWithKnowHosts(usr: String, host: String, port: Int, password: String, knownHosts: String): IO[JSchException, JSchSession] =
    ZIO.effect { internal.newJCraftSessionWithKnowHosts(usr, host, port, password, knownHosts) }.refineToOrDie[JSchException]

  def createWithPropsAndKnowHosts(usr: String, host: String, port: Int, password: String, props: Properties, knownHosts: String): IO[JSchException, JSchSession] =
    ZIO.effect { internal.newJCraftSessionWithPropsAndKnowHosts(usr, host, port, password, props, knownHosts)}.refineToOrDie[JSchException]

  def createConnection(session: JSchSession, timeout: Int): Task[JSchSession] = ZIO.effect { session.establishConnection(timeout) }.refineToOrDie[JSchException]
}