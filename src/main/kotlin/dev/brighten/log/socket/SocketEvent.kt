package dev.brighten.log.socket

import lombok.Getter
import java.net.InetAddress
import java.net.Socket

@Getter
class SocketEvent(val socket: Socket, val address: InetAddress, val request: InRequest)