package de.ritzelprimpf.toniqo.chordfinder.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import de.ritzelprimpf.toniqo.R
import de.ritzelprimpf.toniqo.chordfinder.domain.model.DegreeChord
import de.ritzelprimpf.toniqo.common.model.ChordQuality
import de.ritzelprimpf.toniqo.ui.theme.JetBrainsMonoFamily
import de.ritzelprimpf.toniqo.ui.theme.Tq

// §8.4: "Roman numeral in h2 mono size" — H2 metrics (17sp SemiBold −0.018em) with mono family
private val RomanNumeralStyle = TextStyle(
    fontFamily    = JetBrainsMonoFamily,
    fontSize      = 17.sp,
    lineHeight    = 22.sp,
    fontWeight    = FontWeight.SemiBold,
    letterSpacing = (-0.018).em,
)

private val RowShape = RoundedCornerShape(Tq.Radius.Md)
private val LEFT_BLOCK_WIDTH = 40.dp  // fixed column width for the Roman numeral area

/**
 * A single diatonic chord row for the Chord Finder list screen.
 *
 * Layout (§8.4): coloured Roman numeral + quality abbreviation on the left, vertical divider,
 * chord symbol + note pills on the right, trailing chevron. The entire row is clickable.
 */
@Composable
internal fun ChordDegreeRow(
    degreeChord: DegreeChord,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val qualityColor = DegreeColor.of(degreeChord.triadQuality)
    val qualityAbbrev = when (degreeChord.triadQuality) {
        ChordQuality.MAJOR      -> stringResource(R.string.cf_quality_maj)
        ChordQuality.MINOR      -> stringResource(R.string.cf_quality_min)
        ChordQuality.DIMINISHED -> stringResource(R.string.cf_quality_dim)
        ChordQuality.AUGMENTED  -> stringResource(R.string.cf_quality_aug)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RowShape)
            .background(Tq.Color.BgElev1)
            .border(1.dp, Tq.Color.LineFaint, RowShape)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(Tq.Sp.s3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // ── Left block: Roman numeral + quality abbreviation ──
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(LEFT_BLOCK_WIDTH),
            ) {
                Text(
                    text = degreeChord.romanNumeral,
                    style = RomanNumeralStyle,
                    color = qualityColor,
                )
                Text(
                    text = qualityAbbrev,
                    style = Tq.Type.KickerS,
                    color = Tq.Color.FgTertiary,
                )
            }

            Spacer(Modifier.width(Tq.Sp.s2))

            // ── Vertical divider ──
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(Tq.Color.LineFaint),
            )

            Spacer(Modifier.width(Tq.Sp.s2))

            // ── Right block: chord symbol + note pills ──
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = degreeChord.symbol,
                    style = Tq.Type.H2,
                    color = Tq.Color.FgPrimary,
                )
                Spacer(Modifier.height(Tq.Sp.s1))
                Row(horizontalArrangement = Arrangement.spacedBy(Tq.Sp.s1)) {
                    degreeChord.noteNames.forEach { name ->
                        NotePill(name)
                    }
                }
            }

            Spacer(Modifier.width(Tq.Sp.s2))

            // ── Trailing chevron ──
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = Tq.Color.FgTertiary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun NotePill(name: String) {
    val pillShape = RoundedCornerShape(Tq.Radius.Xs)
    Box(
        modifier = Modifier
            .background(Tq.Color.BgElev2, pillShape)
            .border(1.dp, Tq.Color.LineFaint, pillShape)
            .padding(horizontal = Tq.Sp.s1, vertical = 2.dp),
    ) {
        Text(
            text = name,
            style = Tq.Type.Kicker,
            color = Tq.Color.FgSecondary,
        )
    }
}
