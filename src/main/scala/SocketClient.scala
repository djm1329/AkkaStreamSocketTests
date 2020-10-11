import akka.actor._
import akka.stream.alpakka.unixdomainsocket.scaladsl.UnixDomainSocket
import akka.stream.scaladsl._
import akka.util.ByteString

import scala.concurrent.duration._

import java.nio.file.Paths
import java.nio.{ByteBuffer, ByteOrder}

object SocketClient {
   def apply(system: ActorSystem) = new SocketClient(system)
}

class SocketClient(system: ActorSystem) {

   implicit val actorSystem = system

   system.log.info("**** Starting socket client test *****")

   var sendCount = 0
   var rxCount = 0

   val path = Paths.get(s"test.sock")

   // val socketFlow = UnixDomainSocket().outgoingConnection(path)
   val socketFlow = Tcp().outgoingConnection("localhost", 8080)
   
   val out = Source
      .tick(10.millis, 1.seconds, List.fill(1000)("hello world"))
      .mapConcat(identity)
      .map{s => s"$s ${sendCount +=1; sendCount}"}
      .map {s =>
         val bytes = s.getBytes
         val len = bytes.length
         ByteString(ByteBuffer.allocate(4 + len).putInt(len).put(bytes).array)
      }
      // .log("client outgoing")
      .via(socketFlow)
      .async
      // .log("client incoming")
      .via(Framing.lengthField(fieldLength = 4, maximumFrameLength = 1048576, byteOrder = ByteOrder.BIG_ENDIAN))
      .map(_.drop(4).utf8String)
      .log("incoming after convert to string")
      .map{s => rxCount += 1; s}
      .run

   val reporter = Source
      .tick(50.millis, 10.seconds, "report")
      .map{s => system.log.info("client sent {} received {}", sendCount, rxCount); s}
      .run


}
