# Gateway for Opencraft

A layer 7 gateway protocol for redirecting players to different server instances. The server should be started prior to the gateway.

Implemented with mcprotollib, the gateway is tested on Opencraft. Theoretically, it works on any Minecraft server.


## Implemented Policy

- Simple Policy: Split the terrain based on the negative / positive distances to the spwan location. The gateway redirect the players to different servers based on their current locations.



## Configuration File

- gateway-config.json