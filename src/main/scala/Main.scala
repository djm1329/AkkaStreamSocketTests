import akka.actor.{ActorSystem}
import scala.io.StdIn

object ServerMain extends App {

   val system: ActorSystem = ActorSystem("TestServer")

   val serverSender = system.actorOf(ServerSender.props, "sender")
   val serverReceiver = system.actorOf(ServerReceiver.props(serverSender), "receiver")
   
   val server = SocketServer(system, serverSender, serverReceiver)

   println(s"Server online at test.sock RETURN to stop...")
   StdIn.readLine 
   system.terminate()
   
}

object ClientMain extends App {

   val system: ActorSystem = ActorSystem("TestClient")

   val client = SocketClient(system)

   println(s"client starting press RETURN to stop...")
   StdIn.readLine 
   system.terminate()

}
