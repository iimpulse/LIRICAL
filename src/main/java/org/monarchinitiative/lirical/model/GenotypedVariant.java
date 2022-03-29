package org.monarchinitiative.lirical.model;

import org.monarchinitiative.svart.GenomicVariant;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface GenotypedVariant {

    static GenotypedVariant of(GenomicVariant variant, Map<String, AlleleCount> genotypes) {
        return new GenotypedVariantDefault(variant, genotypes);
    }

    GenomicVariant variant();

    Set<String> sampleNames();

    Optional<AlleleCount> alleleCount(String sample);

}
