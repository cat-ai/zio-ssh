package io.cat.ai.zio.ssh

import java.util.Properties
import java.util.concurrent.TimeUnit

import com.jcraft.jsch.ChannelExec

import io.cat.ai.zio.ssh.inout.ChannelInputReader.ChannelReader
import io.cat.ai.zio.ssh.jcraft.SshChannel.Channel
import io.cat.ai.zio.ssh.jcraft.SshSession.Session

import zio.{ExitCode, URIO, ZIO}
import zio.console._
import zio.duration.Duration

object Ssh extends zio.App {

  import io.cat.ai.zio._
  import io.cat.ai.zio.ssh.session._

  val config = new Properties
  config.put(STRICT_HOSTKEY_CHECKIN_KEY, STRICT_HOSTKEY_CHECKIN_NO_VALUE)

  val sshProgram: ZIO[Console with Session with Channel with ChannelReader, Throwable, String] =
    for {
      session          <- ssh.session.createWithProps(usr = "your_username", host = "host.to.connect", password = "your_password", props = config)
      _                <- session.establishConnection()

      _                <- putStrLn("Session connection established")

      jcraftChannel    <- ssh.channel.open(session, EXEC_CHANNEL_TYPE)
      _                <- putStrLn(s"Opened session $jcraftChannel")
      _                =  jcraftChannel.underlying.asInstanceOf[ChannelExec].setCommand("ls -ltr")
      _                =  jcraftChannel.underlying.asInstanceOf[ChannelExec].setErrStream(System.err)

      sshChannel       <- ssh.channel.connect(jcraftChannel)

      _                <- putStrLn(s"Connected to channel $sshChannel")

      result           <- ssh.sshIO.read(sshChannel)

      _                =  sshChannel.disconnect()
      _                =  session.disconnect()

      _                <- putStrLn(result)
    } yield result

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    (sshProgram *> ZIO.sleep(Duration(5000, TimeUnit.MILLISECONDS))) as ExitCode.success catchAllCause (cause => putStrLn(s"${cause.prettyPrint}") as ExitCode.failure)

    ZIO.succeed(0) as ExitCode.success
  }
}