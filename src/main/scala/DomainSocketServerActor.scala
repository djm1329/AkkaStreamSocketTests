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


object DomainSocketServerActor {
   def props(): Props =
     Props(new DomainSocketServerActor())

   case object Ack
   case object StreamInitialized
   case object StreamCompleted
   final case class StreamFailure(ex: Throwable)
}

class DomainSocketServerActor() extends Actor with ActorLogging with Stash {
   import DomainSocketServerActor._

   implicit val actorSystem = context.system
   implicit val materializer = ActorMaterializer()
   import context.dispatcher

   log.info("**** Starting domain socket test *****")

   var count = 0

   val file = Paths.get(s"test.sock").toFile
   file.delete()
   file.deleteOnExit()

   val actorRefSink = Sink.actorRefWithAck[ByteString](
      self,
      onInitMessage = StreamInitialized,
      ackMessage = Ack,
      onCompleteMessage = StreamCompleted,
      onFailureMessage = (ex: Throwable) => StreamFailure(ex)
   )

   val sink = RecordIOFraming.scanner()
      // .log("domain socket incoming")
      .to(actorRefSink)

   val source = Source
      .queue[String](5, OverflowStrategy.backpressure)
      //.log("domain socket outgoing")
      .map(s => ByteString(s.length + "\n" + s))

   val (queue, src) = source.preMaterialize()

   val process = Flow.fromSinkAndSource(sink, src)

   // val binding: Future[UnixDomainSocket.ServerBinding] =
   //    UnixDomainSocket().bindAndHandle(process, file, halfClose = true)

   val binding =
      Tcp().bindAndHandle(process, "localhost", 1329, halfClose = true)

   binding.map{_ =>
      log.info("**** Binding successful *****")
   }

   def idle: Receive = {
      case StreamInitialized =>
         println("Got stream initialized")
         context.system.scheduler.schedule(10.millisecond, 10.millisecond, self, "HELLO WORLD")
         sender ! Ack
         context.become(ready)
   }

   def ready: Receive = {
      case s: String =>
         count += 1
         val f = queue.offer(s"$s $count")
         f pipeTo self
         context.become(waitingForQueue(s))

      case bytes: ByteString =>
         val str = bytes.utf8String
         println(str)
         sender ! Ack

      case StreamCompleted =>
         println("Got stream completed")
         context.become(idle)

      case StreamFailure(ex) =>
         println("*** error: " + ex)
         context.become(idle)
   }

   def waitingForQueue(s: String): Receive = {
      case QueueOfferResult.Enqueued =>
         // println(s"enqueued $s")
         unstashAll
         context.become(ready)

      case QueueOfferResult.Dropped     =>
         println(s"dropped $s")
         context.become(idle)

      case QueueOfferResult.Failure(ex) =>
         println(s"offer failed ${ex.getMessage}")
         context.become(idle)

      case QueueOfferResult.QueueClosed =>
         println("source Queue closed")
         context.become(idle)

      case bytes: ByteString =>
         val str = bytes.utf8String
         println(str)
         sender ! Ack

      case s: String => stash

      case StreamCompleted =>
         println("Got stream completed")
         context.become(idle)

      case StreamFailure(ex) =>
         println("*** error: " + ex)
         context.become(idle)
   }

   // def receive = Actor.emptyBehavior
   def receive = idle

}
