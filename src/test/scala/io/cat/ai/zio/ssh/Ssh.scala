package io.cat.ai.zio.ssh

import java.util.Properties
import java.util.concurrent.TimeUnit

import com.jcraft.jsch.ChannelExec

import zio.{ExitCode, URIO, ZIO}
import zio.console._
import zio.duration.Duration

object Ssh extends zio.App {

  import io.cat.ai.zio._
  import io.cat.ai.zio.ssh.session._

  val config = new Properties
  config.put("StrictHostKeyChecking", "no")

  val sshProgram: ZIO[Console, Throwable, String] =
    for {
      session          <- ssh.session.createWithProps(usr = "your_username", host = "host.to.connect", password = "your_password", props = config)
      _                <- session.establishConnection()

      _                <- putStrLn("Session connection established")

      jcraftChannel    <- ssh.channel.open(session, EXEC_CHANNEL_TYPE)
      _                =  jcraftChannel.underlying.asInstanceOf[ChannelExec].setCommand("ls -ltr")
      _                =  jcraftChannel.underlying.asInstanceOf[ChannelExec].setErrStream(System.err)

      channel          <- ssh.channel.connect(jcraftChannel)

      _                <- putStrLn(s"Connected to channel $channel")

      result           <- ssh.sshIO.read(channel)

      _                <- putStrLn(result)
    } yield result

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    (sshProgram *> ZIO.sleep(Duration(2000, TimeUnit.MILLISECONDS))) as ExitCode.success catchAllCause(cause => putStrLn(s"${cause.prettyPrint}") as ExitCode.failure)
}
