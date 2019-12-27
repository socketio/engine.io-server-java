===
API
===

.. contents:: Table of Contents
   :local:

EngineIoServer
==============

The ``EngineIoServer`` class contains the logic for accepting and handling
connections from the client.

Methods
-------

handleRequest
^^^^^^^^^^^^^

Call this method to handle an incoming HTTP request.

handleWebSocket
^^^^^^^^^^^^^^^

Call this method to handle an incoming WebSocket request.

Events
------

``EngineIoServer`` emits the following events:

connection
^^^^^^^^^^

This event is emitted when a new client successfully connects to the server.

**Arguments**

0. ``EngineIoSocket`` object

EngineIoSocket
==============

The ``EngineIoSocket`` class represents one connection to a remote client.

Methods
-------

send
^^^^

Call this method on a connected ``EngineIoSocket`` instance to queue a packet for sending to
remote client.
This method is thread safe.

close
^^^^^

Call this method to close the connection with the remote socket.

Events
------

``EngineIoSocket`` emits the following events:

open
^^^^

This event is emitted when a connection is established.

**Note** This event cannot be trapped as it occurs before the "connection" event of ``EngineIoServer``.

close
^^^^^

This event is emitted when the socket is closed either by the server or the client.

**Arguments**

0. ``String`` indicating reason for close
1. ``String`` indicating description of reason or ``null``

packet
^^^^^^

This event is emitted when a packet is received from the remote client.

**Arguments**

0. ``Packet`` object

heartbeat
^^^^^^^^^

This event is emitted when a *ping* packet is received from the remote client.

message
^^^^^^^

This event is emitted when a *message* packet is received from the remote client.

**Arguments**

0. ``String`` or ``byte[]`` object sent by the remote client

data
^^^^

Same as ``message`` event.

flush
^^^^^

This event is raised just before sending packets to the remote client.

**Arguments**

0. ``Collection<Packet<?>>`` being sent to the client

drain
^^^^^

This event is raised after queued packets have been sent to the remote client.

Sending Data
------------

Use the ``send`` method on ``EngineIoSocket`` to send packets to the remote client.
Queuing of packets in case of polling transport are handled internally by ``EngineIoSocket``.

For example::

    EngineIoSocket socket;  // connected socket
    socket.send(new Packet<>(Packet.MESSAGE, "foo"));

Receiving Data
--------------

Listen on either the *message* or *data* event to receive data from the remote client.