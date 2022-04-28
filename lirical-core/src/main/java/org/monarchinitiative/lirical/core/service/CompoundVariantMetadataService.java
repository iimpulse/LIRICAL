package org.monarchinitiative.lirical.core.service;

import org.monarchinitiative.lirical.core.model.ClinvarClnSig;
import org.monarchinitiative.lirical.core.model.TranscriptAnnotation;
import org.monarchinitiative.lirical.core.model.VariantMetadata;
import org.monarchinitiative.svart.GenomicVariant;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class CompoundVariantMetadataService implements VariantMetadataService {

    private final FunctionalVariantAnnotator variantAnnotator;
    private final VariantFrequencyService frequencyService;
    private final VariantPathogenicityService pathogenicityService;

    public static CompoundVariantMetadataService of(FunctionalVariantAnnotator variantAnnotator,
                                                    VariantFrequencyService frequencyService,
                                                    VariantPathogenicityService pathogenicityService) {
        return new CompoundVariantMetadataService(variantAnnotator, frequencyService, pathogenicityService);
    }

    private CompoundVariantMetadataService(FunctionalVariantAnnotator variantAnnotator,
                                          VariantFrequencyService frequencyService,
                                          VariantPathogenicityService pathogenicityService) {
        this.variantAnnotator = Objects.requireNonNull(variantAnnotator);
        this.frequencyService = Objects.requireNonNull(frequencyService);
        this.pathogenicityService = Objects.requireNonNull(pathogenicityService);
    }


    @Override
    public VariantMetadata metadata(GenomicVariant variant) {
        // First, the frequency data
        float frequency = frequencyService.getFrequency(variant).orElse(0f);

        // Next, the pathogenicity data
        Optional<VariantPathogenicity> pathogenicityOptional = pathogenicityService.getPathogenicity(variant);
        float pathogenicity = pathogenicityOptional.map(VariantPathogenicity::pathogenicity)
                .orElse(0f);
        ClinvarClnSig clinicalSignificance = pathogenicityOptional.flatMap(VariantPathogenicity::clinvarClnSig)
                .orElse(ClinvarClnSig.NOT_PROVIDED);

        // Last, get variant annotations
        List<TranscriptAnnotation> annotations = variantAnnotator.annotate(variant);

        return VariantMetadata.of(frequency, pathogenicity, clinicalSignificance, annotations);
    }

}
