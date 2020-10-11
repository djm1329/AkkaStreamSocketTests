import akka.actor._
import akka.stream.alpakka.unixdomainsocket.scaladsl.UnixDomainSocket
import akka.stream.scaladsl._
import akka.stream._
import akka.util.ByteString
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source

import scala.concurrent.Future
import scala.concurrent.duration._

import java.nio.file.Paths
import java.nio.{ByteBuffer, ByteOrder}
import akka.util.Timeout
import scala.concurrent.duration._


object SocketServer {
  def apply(system: ActorSystem, serverSender: ActorRef, serverReceiver: ActorRef) = new SocketServer(system, serverSender, serverReceiver)

}

class SocketServer(system: ActorSystem, serverSender: ActorRef, serverReceiver: ActorRef) {
  implicit val actorSystem = system
  implicit val ec = system.dispatcher
  implicit val askTimeout = Timeout(1.second)

  system.log.info("**** Starting domain socket test *****")

  val path = Paths.get(s"test.sock")
  val file = path.toFile
  file.delete()
  file.deleteOnExit()
   
  val sink = Flow[ByteString]
    .via(Framing.lengthField(fieldLength = 4, maximumFrameLength = 1048576, byteOrder = ByteOrder.BIG_ENDIAN))
    // .log("server after incoming framing")
    .map(_.drop(4).utf8String)
    .recover{
      case ex => 
        system.log.error(ex, "server receive failure")
        "Error"
    }
    .ask[ServerReceiver.Ack.type](serverReceiver)
    .log("server after ask")
    .to(Sink.onComplete{_ => serverReceiver ! ServerReceiver.Disconnect})

  val source = Source.actorRefWithBackpressure[String](
      ServerSender.Ack,
      {case "Stop" => CompletionStrategy.immediately}: PartialFunction[Any, CompletionStrategy],
      PartialFunction.empty
    )
   
  val outgoingFraming = Flow[String]
    .map {s =>
      val bytes = s.getBytes
      val len = bytes.length
      ByteString(ByteBuffer.allocate(4 + len).putInt(len).put(bytes).array)
  }

  // val server: Source[UnixDomainSocket.IncomingConnection, Future[UnixDomainSocket.ServerBinding]] = UnixDomainSocket().bind(path, halfClose = true)
  val server: Source[Tcp.IncomingConnection, Future[Tcp.ServerBinding]] = Tcp().bind("localhost", 8080)

  val binding = server
    .to(Sink.foreach { connection =>
        serverReceiver ! ServerReceiver.NewConnection
        val (clientSender, src) = source.preMaterialize()
        serverSender ! ServerSender.SenderConfig(clientSender)
        val process = Flow.fromSinkAndSourceCoupled(sink, src.via(outgoingFraming))
        val _ = connection.handleWith(process)
      }
    )
    .run

  binding.map{_ =>
    system.log.info("**** Binding successful *****")
  }

}
