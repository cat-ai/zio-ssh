package io.cat.ai.zio.ssh.inout

import zio.ZIO

trait Writer[-A, +B] {

  def write(a: A): ZIO[zio.ZEnv, Throwable, B]
}