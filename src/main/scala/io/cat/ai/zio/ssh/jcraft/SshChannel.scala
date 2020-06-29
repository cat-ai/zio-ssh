package io.cat.ai.zio.ssh.jcraft

import java.io.IOException

import com.jcraft.jsch.{JSchException, Channel => JSchChannel, Session => JSchSession}

import io.cat.ai.zio.ssh

import zio._

import scala.util.Try

object SshChannel {

  type Channel = Has[SshChannel.ChannelService]

  sealed trait ChannelType
  case object exec    extends ChannelType
  case object session extends ChannelType
  case object shell   extends ChannelType
  case object sftp    extends ChannelType
  case object Empty   extends ChannelType

  case class JCraftChannelWrapper(underlying: JSchChannel, channelType: ChannelType)

  trait ChannelService extends Serializable {

    def open(session: JSchSession, channelType: String): IO[IOException, JCraftChannelWrapper]

    def connect(jCraftChannel: JCraftChannelWrapper, timeout: Int): IO[IOException, JSchChannel]
  }

  object ChannelService {

    import internal._

    val default: ChannelService = new ChannelService {

      def open(session: JSchSession, channelType: String): IO[IOException, JCraftChannelWrapper] =
        IO.effect { createJCraftChannel(session, channelType) }.refineToOrDie[IOException]

      def connect(jCraftChannel: JCraftChannelWrapper, timeout: Int): IO[IOException, JSchChannel] =
        IO.effect { jCraftChannel.establishConnection(timeout) }.refineToOrDie[IOException]
    }
  }

  object internal {

    def createJCraftChannel(jcraftSession: JSchSession, `type`: String): JCraftChannelWrapper = {

      def jcraftChannel(channel: JSchChannel, channelType: ChannelType): JCraftChannelWrapper = JCraftChannelWrapper(channel, channelType)

      try
        `type` match {
          case sessionType @ ssh.SESSION_CHANNEL_TYPE => jcraftChannel(jcraftSession.openChannel(sessionType), session)
          case shellType   @ ssh.SHELL_CHANNEL_TYPE   => jcraftChannel(jcraftSession.openChannel(shellType), shell)
          case execType    @ ssh.EXEC_CHANNEL_TYPE    => jcraftChannel(jcraftSession.openChannel(execType), exec)
          case sftpType    @ ssh.SFTP_CHANNEL_TYPE    => jcraftChannel(jcraftSession.openChannel(sftpType), sftp)
        }
      catch {
        case ex: JSchException => throw new IOException(ex)
      }
    }

    implicit class JCraftChannelConnection(jcraftChannel: JCraftChannelWrapper) {
      def establishConnection(timeout: Int): JSchChannel = {
        try {
          jcraftChannel.underlying.connect(timeout)
          jcraftChannel.underlying
        } catch {
          case ex: JSchException => throw new IOException(ex)
        }
      }
    }

  }

  val any: ZLayer[Channel, Nothing, Channel] = ZLayer.requires[Channel]

  val default: Layer[Nothing, Channel] = ZLayer.succeed(ChannelService.default)

  import internal._

  // experimental
  def openAccessM(session: JSchSession, channelType: String): ZIO[Channel, Throwable, JCraftChannelWrapper] = ZIO.accessM(_.get open(session, channelType))

  def connectAccessM(jCraftChannel: JCraftChannelWrapper, timeout: Int): ZIO[Channel, Throwable, JSchChannel] = ZIO.accessM(_.get connect(jCraftChannel, timeout) )
  // experimental

  def open(session: JSchSession, channelType: String): Task[JCraftChannelWrapper] = ZIO.fromTry { Try(createJCraftChannel(session, channelType)) }

  def connect(jCraftChannel: JCraftChannelWrapper, timeout: Int): Task[JSchChannel] = ZIO.fromTry { Try(jCraftChannel.establishConnection(timeout)) }
}
