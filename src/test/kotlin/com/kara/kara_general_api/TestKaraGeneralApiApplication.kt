package com.kara.kara_general_api

import org.springframework.boot.fromApplication
import org.springframework.boot.with

fun main(args: Array<String>) {
    fromApplication<KaraGeneralApiApplication>().with(TestcontainersConfiguration::class).run(*args)
}
