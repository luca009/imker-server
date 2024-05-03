package com.luca009.imker.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableAsync
@EnableScheduling
class ImkerServerApplication

fun main(args: Array<String>) {
    runApplication<ImkerServerApplication>(*args)
}
