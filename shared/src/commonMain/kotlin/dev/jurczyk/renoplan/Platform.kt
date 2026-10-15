package dev.jurczyk.renoplan

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform