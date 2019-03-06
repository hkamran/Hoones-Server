# Hoones-Server

This project is the multiplayer component for the Hoones project.

## High-Level Design

The emulator before every frame will wait for all player key states given to it, and then performs a render. After
which will re-perform this logic. This in no way "hides" delays between players and the server, since there is a mandatory
requirement to wait for all key states before rendering. Most of the delay is caused from sending and waiting for key states (plus TCP overhead). One benefit for this design is that it greatly simplifies the synchronization logic, since we ensure at every frame all players are rendering the same input, and our only concern is with the initial synchronization.

## Server Sequence

A player must first send a REST request to create a room. The player(s) then join the room via WebSocket and follows the 
state flow given from the WebSocket.

## Components

* Restful Server 
* WebSocket Server

## Requirements

* Java 8+

