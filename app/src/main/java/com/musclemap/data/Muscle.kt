package com.musclemap.data

enum class Muscle(val id: String) {
    Abs("abs"),
    Biceps("biceps"),
    Calves("calves"),
    Chest("chest"),
    Deltoids("deltoids"),
    Feet("feet"),
    Forearm("forearm"),
    Gluteal("gluteal"),
    Hamstring("hamstring"),
    Hands("hands"),
    Head("head"),
    Knees("knees"),
    LowerBack("lower-back"),
    Obliques("obliques"),
    Quadriceps("quadriceps"),
    Tibialis("tibialis"),
    Trapezius("trapezius"),
    Triceps("triceps"),
    UpperBack("upper-back"),

    // New muscle groups
    RotatorCuff("rotator-cuff"),
    Serratus("serratus"),
    Rhomboids("rhomboids"),

    // Sub-groups
    Ankles("ankles"),
    Adductors("adductors"),
    Neck("neck"),
    HipFlexors("hip-flexors"),
    UpperChest("upper-chest"),
    LowerChest("lower-chest"),
    InnerQuad("inner-quad"),
    OuterQuad("outer-quad"),
    UpperAbs("upper-abs"),
    LowerAbs("lower-abs"),
    FrontDeltoid("front-deltoid"),
    RearDeltoid("rear-deltoid"),
    UpperTrapezius("upper-trapezius"),
    LowerTrapezius("lower-trapezius");

    val displayName: String
        get() = id // Localization mapping would go here in Android

    val isCosmeticPart: Boolean
        get() = this == Head

    val subGroups: List<Muscle>
        get() = when (this) {
            Chest -> listOf(UpperChest, LowerChest)
            Quadriceps -> listOf(InnerQuad, OuterQuad, HipFlexors)
            Abs -> listOf(UpperAbs, LowerAbs)
            Deltoids -> listOf(FrontDeltoid, RearDeltoid)
            Trapezius -> listOf(UpperTrapezius, LowerTrapezius)
            Obliques -> listOf(Serratus)
            Feet -> listOf(Ankles)
            Hamstring -> listOf(Adductors)
            Head -> listOf(Neck)
            else -> emptyList()
        }

    val parentGroup: Muscle?
        get() = when (this) {
            UpperChest, LowerChest -> Chest
            InnerQuad, OuterQuad, HipFlexors -> Quadriceps
            UpperAbs, LowerAbs -> Abs
            FrontDeltoid, RearDeltoid -> Deltoids
            UpperTrapezius, LowerTrapezius -> Trapezius
            Serratus -> Obliques
            Ankles -> Feet
            Adductors -> Hamstring
            Neck -> Head
            else -> null
        }

    val isSubGroup: Boolean
        get() = parentGroup != null

    val isAlwaysVisibleSubGroup: Boolean
        get() = when (this) {
            Ankles, Adductors, Neck -> true
            else -> false
        }
}

enum class BodySlug(val value: String) {
    Abs("abs"),
    Biceps("biceps"),
    Calves("calves"),
    Chest("chest"),
    Deltoids("deltoids"),
    Feet("feet"),
    Forearm("forearm"),
    Gluteal("gluteal"),
    Hamstring("hamstring"),
    Hands("hands"),
    Hair("hair"),
    Head("head"),
    Knees("knees"),
    LowerBack("lower-back"),
    Obliques("obliques"),
    Quadriceps("quadriceps"),
    Tibialis("tibialis"),
    Trapezius("trapezius"),
    Triceps("triceps"),
    UpperBack("upper-back"),

    // New muscle groups
    RotatorCuff("rotator-cuff"),
    Serratus("serratus"),
    Rhomboids("rhomboids"),

    // Sub-groups
    Ankles("ankles"),
    Adductors("adductors"),
    Neck("neck"),
    HipFlexors("hip-flexors"),
    UpperChest("upper-chest"),
    LowerChest("lower-chest"),
    InnerQuad("inner-quad"),
    OuterQuad("outer-quad"),
    UpperAbs("upper-abs"),
    LowerAbs("lower-abs"),
    FrontDeltoid("front-deltoid"),
    RearDeltoid("rear-deltoid"),
    UpperTrapezius("upper-trapezius"),
    LowerTrapezius("lower-trapezius");

    val muscle: Muscle?
        get() = if (this == Hair) null else Muscle.values().find { it.id == value }
}
