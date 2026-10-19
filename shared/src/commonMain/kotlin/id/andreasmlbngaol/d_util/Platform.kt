package id.andreasmlbngaol.d_util

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform