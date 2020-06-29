package io.cat.ai.zio.ssh

import java.util.Properties
import java.util.concurrent.TimeUnit

import com.jcraft.jsch.ChannelSftp

import zio.{ExitCode, URIO, ZIO}
import zio.console._
import zio.duration.Duration

object Sftp extends zio.App {

  import io.cat.ai.zio._
  import io.cat.ai.zio.ssh.session._

  val localFile = "path/to/local_file.txt"
  val remoteDir = "/path/to/remote/dir"
  val fileName = "file_name_after_moving.txt"

  val config = new Properties
  config.put(STRICT_HOSTKEY_CHECKIN_KEY, STRICT_HOSTKEY_CHECKIN_NO_VALUE)

  private val transferZIO: ZIO[Console, Throwable, Unit] =
    for {
      session        <- ssh.session.createWithPropsAndKnownHosts(usr = "your_username", host = "host.to.connect", password = "your_password", props = config, knownHosts = ".ssh/known_hosts")
      _              <- session.establishConnection()

      _              <- putStrLn("Session connection established")

      jcraftChannel  <- ssh.channel.open(session, SFTP_CHANNEL_TYPE)
      sftpChannel    <- ssh.channel.connect(jcraftChannel)

      _              =  jcraftChannel.underlying.asInstanceOf[ChannelSftp].put(localFile, s"$remoteDir/$fileName")
      _              <- putStrLn(s"Moved from $localFile to $remoteDir$fileName")
      _              =  sftpChannel.disconnect()

    } yield ()

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    (transferZIO *> ZIO.sleep(Duration(3000, TimeUnit.MILLISECONDS))) as ExitCode.success catchAllCause(cause => putStrLn(s"${cause.prettyPrint}") as ExitCode.failure)
}