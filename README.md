# Akka Streams Domain Socket Tests

This project tests a bidirectional socket-based communication link for a peer-peer connection. The connection it isn't request-response and the two directions of message exchange are independent. The “server” side exists only to accept connections from the “client” side. After connection either side sends whenever it has a message to be delivered to the other side.

The primary purpose is to test an Akka Streams based domain socket connection, however it can also test using a TCP socket instead.

## SBT - local environment settings

1. Open two terminal windows in the project's top-level directory.
2. Start the "server" in one window:

```bash
sbt "runMain DomainSocketServerMain"
```

3. Start the "client" in the other window:

```bash
sbt "runMain DomainSocketClientMain"
```
