# Akka Streams Domain Socket Tests

This project tests a bidirectional socket-based communication link for a peer-peer connection. The connection isn't request-response, so the two directions of message exchange are independent. The “server” side exists only to accept connections from the “client” side. After connection either side sends whenever it has a message to be delivered to the other side.

The primary purpose is to test an Akka Streams based domain socket connection, however it can also test using a TCP socket instead.

The "server" uses an Akka actor as an interface to the socket channel. ByteStringS received from the socket stream are delivered to the actor, while Strings sent to the actor are emitted to the outgoing socket. The socket uses RecordIO framing.  

## Running using TCP socket

A unix domain socket is used out-of-the-box.

1. Open two terminal windows in the project's top-level directory.
2. Start the "server" in one window:

```bash
sbt "runMain DomainSocketServerMain"
```

3. Start the "client" in the other window:

```bash
sbt "runMain DomainSocketClientMain"
```

`hello world <n>`, with n incremented for each message, should be sent from client to server every 10ms and printed to the server console. `HELLO WORLD <n>` should be sent from server to client at the same rate, and printed in the client console.

## Running using TCP socket

1. In DomainSocketServerActor.scala, uncomment out the use of TCP:

```
val binding =
   Tcp().bindAndHandle(process, "localhost", 1329, halfClose = true)
```

and comment out the domain socket:

```
// val binding: Future[UnixDomainSocket.ServerBinding] =
//   UnixDomainSocket().bindAndHandle(process, file, halfClose = true)
```

2. Do the similar thing for DomainSocketClientActor. Uncomment TCP:

```
val dsFlow = Tcp().outgoingConnection("localhost", 1329)
```

and comment out the domain socket:

```
// val dsFlow = UnixDomainSocket().outgoingConnection(file)
```

3. Start the server and then the client, as above.
