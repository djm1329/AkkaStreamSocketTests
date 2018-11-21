import java.io.{File, IOException}
import java.nio.file.{Files, Paths}
import java.nio.file.attribute._

import akka.Done
import akka.actor._
import akka.pattern.{ pipe }
import akka.stream.ActorMaterializer
import akka.stream.alpakka.unixdomainsocket.scaladsl.UnixDomainSocket
import akka.stream.alpakka.unixdomainsocket.scaladsl.UnixDomainSocket._
import akka.stream.scaladsl._
import akka.stream.alpakka.recordio.scaladsl.RecordIOFraming
import akka.stream._
import akka.util.ByteString
import jnr.unixsocket.UnixSocketAddress

import scala.concurrent.{Future, Promise}
import scala.concurrent.duration._
import scala.util.{Success, Try}


object DomainSocketClientActor {
   def props(): Props =
     Props(new DomainSocketClientActor())

}

class DomainSocketClientActor() extends Actor with ActorLogging {
   import DomainSocketClientActor._

   implicit val actorSystem = context.system
   implicit val materializer = ActorMaterializer()
   import context.dispatcher

   log.info("**** Starting domain socket client test *****")

   var count = 0

   val file = Paths.get(s"test.sock").toFile

   def receive = Actor.emptyBehavior

   // val dsFlow = UnixDomainSocket().outgoingConnection(file)
   val dsFlow = Tcp().outgoingConnection("localhost", 1329)

   Source
      .repeat("hello world")
      //.log("outgoing")
      .throttle(1, 1.millisecond)
      .map{s => count +=1; s"$s $count"}
      .map(s => ByteString(s.length + "\n" + s))
      .via(dsFlow)
      .via(RecordIOFraming.scanner())
      .map(bytes => bytes.utf8String)
      .log("incoming")
      .runWith(Sink.ignore)

}
