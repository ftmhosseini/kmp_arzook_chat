package ca.arzook.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ArzookWebSocketServerApplication

fun main(args: Array<String>) {
    runApplication<ArzookWebSocketServerApplication>(*args)
}
