package com.melihcolpan.musclemap.data

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

data class BodyPartPathData(
    val slug: BodySlug,
    val common: List<String> = emptyList(),
    val left: List<String> = emptyList(),
    val right: List<String> = emptyList()
) {
    val allPaths: List<String>
        get() = common + left + right
}

data class BodyViewBox(
    val origin: Offset,
    val size: Size
) {
    val rect: Rect
        get() = Rect(origin, size)

    companion object {
        val maleFront = BodyViewBox(
            origin = Offset(0f, 150f),
            size = Size(727f, 1180f)
        )

        val maleBack = BodyViewBox(
            origin = Offset(796.5f, 150f),
            size = Size(727f, 1180f)
        )

        val femaleFront = BodyViewBox(
            origin = Offset(0f, 50f),
            size = Size(650f, 1350f)
        )

        val femaleBack = BodyViewBox(
            origin = Offset(755f, 50f),
            size = Size(650f, 1350f)
        )
    }
}

object BodyPathProvider {
    fun paths(gender: BodyGender, side: BodySide): List<BodyPartPathData> {
        return when (Pair(gender, side)) {
            Pair(BodyGender.Male, BodySide.Front) -> MaleFrontPaths.paths
            Pair(BodyGender.Male, BodySide.Back) -> MaleBackPaths.paths
            Pair(BodyGender.Female, BodySide.Front) -> FemaleFrontPaths.paths
            Pair(BodyGender.Female, BodySide.Back) -> FemaleBackPaths.paths
            else -> emptyList()
        }
    }

    fun viewBox(gender: BodyGender, side: BodySide): BodyViewBox {
        return when (Pair(gender, side)) {
            Pair(BodyGender.Male, BodySide.Front) -> BodyViewBox.maleFront
            Pair(BodyGender.Male, BodySide.Back) -> BodyViewBox.maleBack
            Pair(BodyGender.Female, BodySide.Front) -> BodyViewBox.femaleFront
            Pair(BodyGender.Female, BodySide.Back) -> BodyViewBox.femaleBack
            else -> BodyViewBox.maleFront
        }
    }
}
