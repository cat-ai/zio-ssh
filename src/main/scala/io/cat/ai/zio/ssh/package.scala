package io.cat.ai.zio

import java.io.IOException
import java.util.Properties

import com.jcraft.jsch.{Channel => JSchChannel, JSchException, Session => JSchSession}

import io.cat.ai.zio.ssh.inout.ChannelInputReader
import io.cat.ai.zio.ssh.jcraft.{SshChannel, SshSession}
import io.cat.ai.zio.ssh.jcraft.SshChannel.JCraftChannelWrapper

import zio.{IO, Task}

package object ssh {

  final val STRICT_HOSTKEY_CHECKIN_KEY   = "StrictHostKeyChecking"
  final val STRICT_HOSTKEY_CHECKIN_NO_VALUE = "no"

  final val SESSION_CHANNEL_TYPE = "session"
  final val SHELL_CHANNEL_TYPE   = "shell"
  final val EXEC_CHANNEL_TYPE    = "exec"
  final val SFTP_CHANNEL_TYPE    = "sftp"

  object session {

    def create(usr: String, host: String, port: Int = 22): IO[JSchException, JSchSession] = SshSession.create(usr, host, port)

    def createWithProps(usr: String, host: String, port: Int = 22, password: String, props: Properties): IO[JSchException, JSchSession] =
      SshSession.createWithProperties(usr, host, port, password, props)

    def createWithPropsAndKnownHosts(usr: String, host: String, port: Int = 22, password: String, props: Properties, knownHosts: String): IO[JSchException, JSchSession] =
      SshSession.createWithPropsAndKnowHosts(usr, host, port, password, props, knownHosts)

    def createWithKnownHosts(usr: String, host: String, port: Int = 22, password: String, knownHosts: String): IO[JSchException, JSchSession] =
      SshSession.createWithKnowHosts(usr, host, port, password, knownHosts)

    def connectTo(session: JSchSession, timeout: Int = 0): Task[JSchSession] = SshSession.createConnection(session, timeout)

    implicit class JCraftSessionConnectionCreator(session: JSchSession) {
      def establishConnection(timeout: Int = 0): Task[JSchSession] = connectTo(session, timeout)
    }
  }

  object channel {

    def open(session: JSchSession, channelType: String): Task[JCraftChannelWrapper] = SshChannel.open(session, channelType)

    def connect(jCraftChannel: JCraftChannelWrapper, timeout: Int = 0): Task[JSchChannel] = SshChannel.connect(jCraftChannel, timeout)
  }

  object sshIO {
    def read(channel: JSchChannel): IO[IOException, String] = ChannelInputReader.read(channel)
  }
}
