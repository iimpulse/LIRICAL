package org.monarchinitiative.lirical.model;

import org.monarchinitiative.exomiser.core.model.TranscriptAnnotation;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class VariantMetadataDefault implements VariantMetadata {

    private static final VariantMetadataDefault EMPTY = new VariantMetadataDefault(Float.NaN, Float.NaN, false, List.of());

    static VariantMetadataDefault empty() {
        return EMPTY;
    }

    private final float frequency;
    private final float pathogenicity;
    private final boolean isClinvarPathogenic;
    private final List<TranscriptAnnotation> annotations;

    VariantMetadataDefault(float frequency,
                           float pathogenicity,
                           boolean isClinvarPathogenic,
                           List<TranscriptAnnotation> annotations) {
        this.frequency = frequency;
        this.pathogenicity = pathogenicity;
        this.isClinvarPathogenic = isClinvarPathogenic;
        this.annotations = annotations;
    }

    @Override
    public Optional<Float> frequency() {
        return Float.isNaN(frequency)
                ? Optional.empty()
                : Optional.of(frequency);
    }

    @Override
    public float pathogenicityScore() {
        return pathogenicity;
    }

    @Override
    public boolean isClinVarPathogenic() {
        return isClinvarPathogenic;
    }

    @Override
    public List<TranscriptAnnotation> annotations() {
        return annotations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VariantMetadataDefault that = (VariantMetadataDefault) o;
        return Float.compare(that.frequency, frequency) == 0 && Float.compare(that.pathogenicity, pathogenicity) == 0 && isClinvarPathogenic == that.isClinvarPathogenic && Objects.equals(annotations, that.annotations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(frequency, pathogenicity, isClinvarPathogenic, annotations);
    }

    @Override
    public String toString() {
        return "VariantMetadataDefault{" +
                "frequency=" + frequency +
                ", pathogenicity=" + pathogenicity +
                ", isClinvarPathogenic=" + isClinvarPathogenic +
                ", annotations=" + annotations +
                '}';
    }
}
