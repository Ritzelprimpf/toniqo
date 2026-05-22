package de.ritzelprimpf.toniqo.metronome.domain

import de.ritzelprimpf.toniqo.metronome.domain.model.TempoDescriptor
import de.ritzelprimpf.toniqo.metronome.domain.model.tempoDescriptorFor
import org.junit.Assert.assertEquals
import org.junit.Test

class TempoDescriptorTest {

    // -------------------------------------------------------------------------
    // Exact BPM boundary values (from Phase6-Metronome-Decisions.md Item 3)
    // -------------------------------------------------------------------------

    @Test fun `BPM 1 maps to ADAGIO`() = assertEquals(TempoDescriptor.ADAGIO, tempoDescriptorFor(1))
    @Test fun `BPM 75 maps to ADAGIO`() = assertEquals(TempoDescriptor.ADAGIO, tempoDescriptorFor(75))

    @Test fun `BPM 76 maps to ANDANTE`() = assertEquals(TempoDescriptor.ANDANTE, tempoDescriptorFor(76))
    @Test fun `BPM 107 maps to ANDANTE`() = assertEquals(TempoDescriptor.ANDANTE, tempoDescriptorFor(107))

    @Test fun `BPM 108 maps to MODERATO`() = assertEquals(TempoDescriptor.MODERATO, tempoDescriptorFor(108))
    @Test fun `BPM 119 maps to MODERATO`() = assertEquals(TempoDescriptor.MODERATO, tempoDescriptorFor(119))

    @Test fun `BPM 120 maps to ALLEGRO`() = assertEquals(TempoDescriptor.ALLEGRO, tempoDescriptorFor(120))
    @Test fun `BPM 167 maps to ALLEGRO`() = assertEquals(TempoDescriptor.ALLEGRO, tempoDescriptorFor(167))

    @Test fun `BPM 168 maps to PRESTO`() = assertEquals(TempoDescriptor.PRESTO, tempoDescriptorFor(168))
    @Test fun `BPM 300 maps to PRESTO`() = assertEquals(TempoDescriptor.PRESTO, tempoDescriptorFor(300))

    // -------------------------------------------------------------------------
    // Round-trip: every enum value is reachable
    // -------------------------------------------------------------------------

    @Test
    fun `all five TempoDescriptor values are produced by some BPM in the valid range`() {
        val produced = (1..300).map { tempoDescriptorFor(it) }.toSet()
        assertEquals(TempoDescriptor.entries.toSet(), produced)
    }

    // -------------------------------------------------------------------------
    // Midpoint sanity checks
    // -------------------------------------------------------------------------

    @Test fun `BPM 40 maps to ADAGIO`() = assertEquals(TempoDescriptor.ADAGIO, tempoDescriptorFor(40))
    @Test fun `BPM 90 maps to ANDANTE`() = assertEquals(TempoDescriptor.ANDANTE, tempoDescriptorFor(90))
    @Test fun `BPM 112 maps to MODERATO`() = assertEquals(TempoDescriptor.MODERATO, tempoDescriptorFor(112))
    @Test fun `BPM 140 maps to ALLEGRO`() = assertEquals(TempoDescriptor.ALLEGRO, tempoDescriptorFor(140))
    @Test fun `BPM 200 maps to PRESTO`() = assertEquals(TempoDescriptor.PRESTO, tempoDescriptorFor(200))

    // -------------------------------------------------------------------------
    // Out-of-range inputs resolve without throwing (function is total)
    // -------------------------------------------------------------------------

    @Test fun `BPM 0 resolves without throwing`() {
        assertEquals(TempoDescriptor.ADAGIO, tempoDescriptorFor(0))
    }

    @Test fun `BPM negative resolves without throwing`() {
        assertEquals(TempoDescriptor.ADAGIO, tempoDescriptorFor(-10))
    }

    @Test fun `BPM above 300 resolves without throwing`() {
        assertEquals(TempoDescriptor.PRESTO, tempoDescriptorFor(500))
    }
}
