package io.cat.ai.zio.ssh.inout

import java.io.IOException

import com.jcraft.jsch.Channel

import zio._

trait Reader[-A, +B] {

  def read(a: A): ZIO[zio.ZEnv, Throwable, B]
}

object ChannelInputReader extends Reader[Channel, String] {

  type ChannelReader = Has[ChannelInputReader.ReaderService]

  trait ReaderService extends Serializable {
    def read(channel: Channel): IO[IOException, String]
  }

  object ReaderService {
    val default: ReaderService = (channel: Channel) => IO.effect(JSshChannelReader.read(channel)).refineToOrDie[IOException]
  }

  override def read(channel: Channel): IO[IOException, String] = IO.effect(JSshChannelReader.read(channel)).refineToOrDie[IOException]

  val default: Layer[Nothing, ChannelReader] = ZLayer.succeed(ReaderService.default)

  val any: ZLayer[ChannelReader, Nothing, ChannelReader] = ZLayer.requires[ChannelReader]

  def readAccessM(channel: Channel): ZIO[ChannelReader, IOException, String] = ZIO.accessM(_.get read channel)
}
