package com.melihcolpan.musclemap.data

enum class MuscleSide(val value: String) {
    Left("left"),
    Right("right"),
    Both("both");

    val displayName: String
        get() = value // In a real Android app, this would use Context to read from strings.xml
}

enum class BodySide(val value: String) {
    Front("front"),
    Back("back");

    val displayName: String
        get() = value
}

enum class BodyGender(val value: String) {
    Male("male"),
    Female("female");

    val displayName: String
        get() = value
}
