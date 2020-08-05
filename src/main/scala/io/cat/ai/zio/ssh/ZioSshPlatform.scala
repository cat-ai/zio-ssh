package io.cat.ai.zio.ssh

import io.cat.ai.zio.ssh.jcraft.{SshChannel, SshSession}
import io.cat.ai.zio.ssh.io.ChannelInputReader

import io.cat.ai.zio.ssh.io.ChannelInputReader.ChannelReader
import io.cat.ai.zio.ssh.jcraft.SshSession.Session
import io.cat.ai.zio.ssh.jcraft.SshChannel.Channel

import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.Console
import zio.random.Random
import zio.system.System
import zio.{Has, Layer, ZLayer}

trait ZioSshPlatform {
  type ZioSshEnv = Clock with Console with System with Random with Blocking with Session with Channel with ChannelReader

  object ZioSshEnv {

    private object SshServices {
      val default: ZioSshEnv =
        Has.allOf[Clock.Service, Console.Service, System.Service, Random.Service, Blocking.Service, SshSession.SessionService, SshChannel.ChannelService, ChannelInputReader.ReaderService](
          Clock.Service.live,
          Console.Service.live,
          System.Service.live,
          Random.Service.live,
          Blocking.Service.live,
          SshSession.SessionService.default,
          SshChannel.ChannelService.default,
          ChannelInputReader.ReaderService.default
        )
    }

    val any: ZLayer[ZioSshEnv, Nothing, ZioSshEnv] = ZLayer.requires[ZioSshEnv]

    val default: Layer[Nothing, ZioSshEnv] =
      Clock.live ++ Console.live ++ System.live ++ Random.live ++ Blocking.live ++ SshSession.default ++ SshChannel.default ++ ChannelInputReader.default

  }
}