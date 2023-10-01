package com.luca009.imker.imkerserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ImkerServerApplication

fun main(args: Array<String>) {
    runApplication<ImkerServerApplication>(*args)
}
