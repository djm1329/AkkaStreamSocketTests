import akka.actor._
import akka.stream.ActorMaterializer
import akka.stream.alpakka.unixdomainsocket.scaladsl.UnixDomainSocket
import akka.stream.scaladsl._
import akka.stream.alpakka.recordio.scaladsl.RecordIOFraming
import akka.stream._
import akka.util.ByteString

import scala.concurrent.duration._

import java.nio.file.Paths

object DomainSocketClientActor {
   def props(): Props =
     Props(new DomainSocketClientActor())
}

class DomainSocketClientActor() extends Actor with ActorLogging {

   implicit val actorSystem = context.system
   implicit val materializer = ActorMaterializer()

   log.info("**** Starting domain socket client test *****")

   var count = 0

   val file = Paths.get(s"test.sock").toFile

   def receive = Actor.emptyBehavior

   val dsFlow = UnixDomainSocket().outgoingConnection(file)
   // val dsFlow = Tcp().outgoingConnection("localhost", 1329)

   Source
      .repeat("hello world")
      // .single("hello world")
      .throttle(1, 10.milliseconds)
      .concat(Source.maybe)   // ensure stream doesn't complete so messages from server are still received
      .map{s => count +=1; s"$s $count"}
      .map(s => ByteString(s.length + "\n" + s)) // RecordIO framing
      .via(dsFlow)
      .via(RecordIOFraming.scanner())
      .map(bytes => bytes.utf8String)
      .log("incoming")
      .runWith(Sink.ignore)

}
