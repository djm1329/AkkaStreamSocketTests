import akka.actor.{ActorSystem}

object DomainSocketServerMain extends App {

   val system: ActorSystem = ActorSystem("UDSTest")

   system.actorOf(DomainSocketServerActor.props())

}

object DomainSocketClientMain extends App {

   val system: ActorSystem = ActorSystem("UDSTest")

   system.actorOf(DomainSocketClientActor.props())

}
