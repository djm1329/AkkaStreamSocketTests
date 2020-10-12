# Akka Streams Domain Socket Tests

This project tests a bidirectional socket-based communication link for a peer-peer connection. The connection isn't request-response, so the two directions of message exchange are independent. The “server” side exists only to accept connections from the “client” side. After connection either side sends whenever it has a message to be delivered to the other side.

The primary purpose is to test an Akka Streams based domain socket connection, however it can also test using a TCP socket instead.

**WARNING at the time of this writing, the domain socket connection does not work.** TCP however works well.  

The server uses two Akka actors, one as the send interface and one as the receive interface, to the socket channel. The socket channel (either unix domain socket or TCP) is established in `SocketServer.scala`. The `ServerReceiver` and `ServerSender` actors are created beforehand and passed into the SocketServer at instantiation. SocketServer handles stream framing (4 byte length field (in binary) followed by the indicated number of bytes) and exchanges framed ByteStringS with SocketServer and ServerReceiver actors. 

## Running using unix domain socket

A unix domain socket is used out-of-the-box.

1. Open two terminal windows in the project's top-level directory.
2. build the executables

```bash
sbt stage
```

2. Start the "server" in one window:

```bash
./target/universal/stage/bin/server-main
```

3. Start the "client" in the other window:

```bash
./target/universal/stage/bin/client-main
```

After connecting to the server, the app repeatedly passes strings from the client through the socket to the server, which then echoes what has been received. Both server and client emit send and receive counts every 10 seconds.

## Running using TCP socket instead of domain socket

1. In SocketServer.scala, uncomment out the use of TCP:

```
val server: Source[Tcp.IncomingConnection, Future[Tcp.ServerBinding]] = Tcp().bind("localhost", 8080)
```

and comment out the domain socket:

```
// val server: Source[UnixDomainSocket.IncomingConnection, Future[UnixDomainSocket.ServerBinding]] = UnixDomainSocket().bind(path)
```

2. Do the similar thing for SocketClient.scala Uncomment TCP:

```
val socketFlow = Tcp().outgoingConnection("localhost", 8080)
```

and comment out the domain socket:

```
// val socketFlow = UnixDomainSocket().outgoingConnection(path)
```

3. Start the server and then the client, as above.
