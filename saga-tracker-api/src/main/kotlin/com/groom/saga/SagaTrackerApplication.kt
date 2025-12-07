package com.groom.saga

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SagaTrackerApplication

fun main(args: Array<String>) {
    runApplication<SagaTrackerApplication>(*args)
}
