package dev.jurczyk.renoplan.ui.roomdetails

import androidx.compose.runtime.Composable
import dev.jurczyk.renoplan.domain.model.Opening
import dev.jurczyk.renoplan.domain.model.OpeningSize
import dev.jurczyk.renoplan.domain.model.OpeningType
import dev.jurczyk.renoplan.ui.format.formatDecimal
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import renoplan.shared.generated.resources.Res
import renoplan.shared.generated.resources.opening_at_floor
import renoplan.shared.generated.resources.opening_dimensions
import renoplan.shared.generated.resources.opening_summary
import renoplan.shared.generated.resources.opening_type_door
import renoplan.shared.generated.resources.opening_type_niche
import renoplan.shared.generated.resources.opening_type_passage
import renoplan.shared.generated.resources.opening_type_window

val OpeningType.label: StringResource
    get() =
        when (this) {
            OpeningType.WINDOW -> Res.string.opening_type_window
            OpeningType.DOOR -> Res.string.opening_type_door
            OpeningType.PASSAGE -> Res.string.opening_type_passage
            OpeningType.NICHE -> Res.string.opening_type_niche
        }

/** E.g. "Drzwi · 0,80 × 2,00 m · od podłogi", or "Okno" for an opening given by area. */
@Composable
fun openingDescription(opening: Opening): String {
    var text = stringResource(opening.type.label)
    val size = opening.size
    if (size is OpeningSize.Dimensions) {
        val dimensions =
            stringResource(
                Res.string.opening_dimensions,
                formatDecimal(size.width.metres),
                formatDecimal(size.height.metres),
            )
        text = stringResource(Res.string.opening_summary, text, dimensions)
    }
    if (opening.startsAtFloor) {
        text =
            stringResource(Res.string.opening_summary, text, stringResource(Res.string.opening_at_floor))
    }
    return text
}
