import akka.actor._


object ServerSender {
   def props(): Props =
     Props(new ServerSender())

   case object Ack
   case class SenderConfig(sendTo: ActorRef)
   case object SendStringKey
   case class Report(rxCount: Int)
}

class ServerSender() extends Actor with ActorLogging with Stash with Timers {
  import ServerSender._

  implicit val actorSystem = context.system
  // import context.dispatcher

  var count = 0

  def receive = idle

  def idle: Receive = {
    case SenderConfig(sendTo) => 
      log.info(s"server sender (idle) received actor config")
      count = 0
      unstashAll()
      context.watch(sendTo)
      context.become(ready(sendTo))

    case Report(rxCount) => 
      log.info("server sender (idle) received {} sent {}", rxCount, count)
    // case Ack => // dangling Ack should not be stashed  
    case _: String => stash()
  }

  def ready(sendTo: ActorRef): Receive = {

    case s: String =>
      //log.info("sending {}", s)
      sendTo ! s
      count += 1
      context.become(waitingForAck(sendTo))

    case Report(rxCount) => 
      log.info("server sender (ready) received {} sent {}", rxCount, count)

    // case SenderConfig(newSendTo) => 
    //   log.info(s"server sender received actor config in ready state!")
    //   context.unwatch(sendTo)
    //   context.watch(newSendTo)
    //   context.become(ready(newSendTo))

    case Terminated(senderRef) =>
      val activeSenderTerminated = senderRef == sendTo
      log.info(s"server sender (ready) detected termination of ${if(activeSenderTerminated) "active" else "inactive"} domain socket sender actor")
      if(activeSenderTerminated) {
        context.become(idle)
      }
  }

  def waitingForAck(sendTo: ActorRef): Receive = {
 
    case Ack => 
      // log.info(s"server sender received Ack")
      unstashAll()
      context.become(ready(sendTo))

    // case SenderConfig(newSendTo) => 
    //   log.info(s"server sender received actor config in waitingForAck state!")
    //   context.unwatch(sendTo)
    //   context.watch(newSendTo)
    //   context.become(ready(newSendTo))

    case Terminated(senderRef) =>
      val activeSenderTerminated = senderRef == sendTo
      log.info(s"server sender detected termination of ${if(activeSenderTerminated) "active" else "inactive"} domain socket sender actor")
      if(activeSenderTerminated) {
        unstashAll()
        context.become(idle)
      }

    case Report(rxCount) => 
      log.info("server (waitingForAck) received {} sent {}", rxCount, count)

    case _: String => stash()
  }
}
