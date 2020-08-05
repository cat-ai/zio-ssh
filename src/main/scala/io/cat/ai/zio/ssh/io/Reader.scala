package io.cat.ai.zio.ssh.io

import java.io.IOException

import com.jcraft.jsch.Channel

import zio.{IO, _}

trait Reader[-A, +B] {

  def read(a: A): ZIO[zio.ZEnv, Throwable, B]
}

object ChannelInputReader extends Reader[Channel, String] {

  type ChannelReader = Has[ChannelInputReader.ReaderService]

  trait ReaderService extends Serializable {
    def read(channel: Channel): IO[IOException, String]
  }

  object ReaderService {
    val default: ReaderService = (channel: Channel) => IO.effect(read0(channel)).refineToOrDie[IOException]
  }

  private def read0(channel: Channel): Task[String] = {
    val OFFSET: Int = 0
    val LENGTH: Int = 1 << 10
    val bytes = new Array[Byte](LENGTH)
    val in = channel.getInputStream

    def loopTaskString(acc: String): Task[String] =
      IO { (in.available, in.read(bytes, OFFSET, LENGTH), acc) } >>= {
        case (available, i, _acc) if available > 0 && i < 0              => IO(_acc)
        case (available, i, _acc) if available > 0                       => IO("") *> loopTaskString(_acc ++ new String(bytes, OFFSET, i))
        case (_, _, _acc) if _acc contains "object"                      => IO(_acc)
        case (_, _, _acc) if channel.isClosed                            => IO(_acc)
        case (_, _, _acc) if channel.isClosed && in.available() > OFFSET =>
          val i = in.read(bytes, OFFSET, LENGTH)
          IO(_acc ++ new String(bytes, OFFSET, i))
      }


    loopTaskString("")
  }

  override def read(channel: Channel): IO[IOException, String] = read0(channel).refineToOrDie[IOException]

  val default: Layer[Nothing, ChannelReader] = ZLayer.succeed(ReaderService.default)

  val any: ZLayer[ChannelReader, Nothing, ChannelReader] = ZLayer.requires[ChannelReader]

  def readAccessM(channel: Channel): ZIO[ChannelReader, IOException, String] = ZIO.accessM(_.get read channel)
}