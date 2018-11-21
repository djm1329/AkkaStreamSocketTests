# Akka Streams Domain Socket Tests

This project tests a bidirectional socket-based communication link for a peer-peer connection. The connection isn't request-response, so the two directions of message exchange are independent. The “server” side exists only to accept connections from the “client” side. After connection either side sends whenever it has a message to be delivered to the other side.

The primary purpose is to test an Akka Streams based domain socket connection, however it can also test using a TCP socket instead.

The "server" uses an Akka actor as an interface to the socket channel. ByteStringS received from the socket stream are delivered to the actor, while Strings sent to the actor are emitted to the outgoing socket. The socket uses RecordIO framing.  

## Running using TCP socket

A TCP socket is used out-of-the-box.

1. Open two terminal windows in the project's top-level directory.
2. Start the "server" in one window:

```bash
sbt "runMain DomainSocketServerMain"
```

3. Start the "client" in the other window:

```bash
sbt "runMain DomainSocketClientMain"
```

## Running using unix domain socket

1. In DomainSocketServerActor.scala, comment out the use of TCP:

```
// val binding =
//   Tcp().bindAndHandle(process, "localhost", 1329, halfClose = true)
```

and uncomment the domain socket instead:

```
val binding: Future[UnixDomainSocket.ServerBinding] =
   UnixDomainSocket().bindAndHandle(process, file, halfClose = true)
```

2. Do the similar thing for DomainSocketClientActor. Comment out TCP:

```
// val dsFlow = Tcp().outgoingConnection("localhost", 1329)
```

and uncomment the domain socket:

```
val dsFlow = UnixDomainSocket().outgoingConnection(file)
```
