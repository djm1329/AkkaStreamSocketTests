import akka.actor._
import akka.util.ByteString

import scala.concurrent.duration._


object ServerReceiver {
  def props(serverSender: ActorRef): Props =
    Props(new ServerReceiver(serverSender))

  case object StreamConnect
  case object StreamDisconnect
  case object Ack
  case object Report
  case object ReportKey
  final case class StreamFailure(ex: Throwable)
}

class ServerReceiver(serverSender: ActorRef) extends Actor with ActorLogging with Timers {
  import ServerReceiver._

  implicit val actorSystem = context.system

  var rxCount = 0
  
  def receive: Receive = {

    case StreamConnect =>
      log.info("server receiver got new connection")
      rxCount = 0
      serverSender ! ServerSender.Report(rxCount)
      timers.startTimerWithFixedDelay(ReportKey, Report, 10.seconds)

    case Report => 
      serverSender ! ServerSender.Report(rxCount)

    case bs: ByteString => 
      rxCount += 1
      // log.info("received {}", s)
      serverSender ! bs.utf8String
      sender() ! Ack

    case StreamDisconnect =>
      log.info("server receiver got disconnect")
      timers.cancel(ReportKey)

  }
  
}
