import akka.actor._

import scala.concurrent.duration._


object ServerReceiver {
  def props(serverSender: ActorRef): Props =
    Props(new ServerReceiver(serverSender))

  case object NewConnection
  case object Disconnect
  case object Report
  case object Ack
  case object ReportKey
  final case class StreamFailure(ex: Throwable)
}

class ServerReceiver(serverSender: ActorRef) extends Actor with ActorLogging with Timers {
  import ServerReceiver._

  implicit val actorSystem = context.system

  var rxCount = 0
  
  def receive: Receive = {

    case NewConnection =>
      log.info("server receiver got new connection")
      rxCount = 0
      serverSender ! ServerSender.Report(rxCount)
      timers.startTimerWithFixedDelay(ReportKey, Report, 10.seconds)

    case Report => 
      serverSender ! ServerSender.Report(rxCount)

    case "Error" => 
      log.info("Got stream failure")

    case s: String => 
      rxCount += 1
      // log.info("received {}", s)
      serverSender ! s
      sender() ! Ack

    case Disconnect =>
      log.info("server receiver got disconnect")
      timers.cancel(ReportKey)

  }
  
}
