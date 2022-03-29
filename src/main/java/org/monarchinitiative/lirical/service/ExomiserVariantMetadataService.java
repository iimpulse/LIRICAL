package org.monarchinitiative.lirical.service;

import de.charite.compbio.jannovar.annotation.VariantEffect;
import de.charite.compbio.jannovar.data.JannovarData;
import de.charite.compbio.jannovar.data.JannovarDataSerializer;
import de.charite.compbio.jannovar.data.SerializationException;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.monarchinitiative.exomiser.core.genome.GenomeAssembly;
import org.monarchinitiative.exomiser.core.genome.JannovarVariantAnnotator;
import org.monarchinitiative.exomiser.core.genome.dao.serialisers.MvStoreUtil;
import org.monarchinitiative.exomiser.core.genome.jannovar.InvalidFileFormatException;
import org.monarchinitiative.exomiser.core.genome.jannovar.JannovarDataProtoSerialiser;
import org.monarchinitiative.exomiser.core.model.AlleleProtoAdaptor;
import org.monarchinitiative.exomiser.core.model.ChromosomalRegionIndex;
import org.monarchinitiative.exomiser.core.model.RegulatoryFeature;
import org.monarchinitiative.exomiser.core.model.VariantAnnotation;
import org.monarchinitiative.exomiser.core.model.frequency.FrequencyData;
import org.monarchinitiative.exomiser.core.model.pathogenicity.ClinVarData;
import org.monarchinitiative.exomiser.core.model.pathogenicity.PathogenicityData;
import org.monarchinitiative.exomiser.core.model.pathogenicity.VariantEffectPathogenicityScore;
import org.monarchinitiative.exomiser.core.proto.AlleleProto;
import org.monarchinitiative.lirical.io.LiricalDataException;
import org.monarchinitiative.lirical.model.VariantMetadata;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.GenomicVariant;
import org.monarchinitiative.svart.Strand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public class ExomiserVariantMetadataService implements VariantMetadataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExomiserVariantMetadataService.class);
    // The following effects are considered pathogenic
    private static final Set<ClinVarData.ClinSig> CLINVAR_PATHOGENIC = Set.of(ClinVarData.ClinSig.PATHOGENIC,
            ClinVarData.ClinSig.PATHOGENIC_OR_LIKELY_PATHOGENIC,
            ClinVarData.ClinSig.LIKELY_PATHOGENIC);

    /**
     * A map with data from the Exomiser database.
     */
    private final MVMap<AlleleProto.AlleleKey, AlleleProto.AlleleProperties> alleleMap;
    private final JannovarVariantAnnotator variantAnnotator;
    private final Options options;

    public static ExomiserVariantMetadataService of(Path mvStore,
                                                    Path jannovarCache,
                                                    GenomeAssembly genomeAssembly,
                                                    Options options) throws LiricalDataException {
        MVStore store = new MVStore.Builder()
                .fileName(mvStore.toAbsolutePath().toString())
                .readOnly()
                .open();

        JannovarData jannovarData = deserializeJannovar(jannovarCache);
        ChromosomalRegionIndex<RegulatoryFeature> emptyRegionIndex = ChromosomalRegionIndex.of(List.of());
        JannovarVariantAnnotator jannovarVariantAnnotator = new JannovarVariantAnnotator(genomeAssembly, jannovarData, emptyRegionIndex);

        return of(store, jannovarVariantAnnotator, options);
    }

    public static ExomiserVariantMetadataService of(MVStore mvStore,
                                                    JannovarVariantAnnotator variantAnnotator,
                                                    Options options) {
        return new ExomiserVariantMetadataService(mvStore, variantAnnotator, options);
    }

    private static JannovarData deserializeJannovar(Path jannovarCache) throws LiricalDataException {
        try {
            return JannovarDataProtoSerialiser.load(jannovarCache);
        } catch (InvalidFileFormatException e) {
            LOGGER.info("Could not deserialize Jannovar file with Protobuf deserializer, trying legacy deserializer...");
        }
        try {
            return new JannovarDataSerializer(jannovarCache.toAbsolutePath().toString()).load();
        } catch (SerializationException e) {
            LOGGER.error("Could not deserialize Jannovar file with legacy deserializer...");
            throw new LiricalDataException(String.format("Could not load Jannovar data from %s", jannovarCache.toAbsolutePath()), e);
        }
    }

    public ExomiserVariantMetadataService(MVStore mvStore,
                                          JannovarVariantAnnotator variantAnnotator,
                                          Options options) {
        this.alleleMap = MvStoreUtil.openAlleleMVMap(mvStore);
        this.variantAnnotator = variantAnnotator;
        this.options = options;
    }

    @Override
    public VariantMetadata metadata(GenomicVariant variant) {
        int pos = variant.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.oneBased());

        VariantAnnotation annotation = variantAnnotator.annotate(variant.contigName(), pos, variant.ref(), variant.alt());

        AlleleProto.AlleleKey alleleKey = createAlleleKey(variant);
        AlleleProto.AlleleProperties alleleProp = alleleMap.get(alleleKey);

        float frequency;
        float pathogenicity;
        boolean isClinvarPathogenic;
        float variantEffectPathogenicity = VariantEffectPathogenicityScore.getPathogenicityScoreOf(annotation.getVariantEffect());
        if (alleleProp == null) {
            frequency = options.defaultFrequency();
            pathogenicity = variantEffectPathogenicity;
            isClinvarPathogenic = false;
        } else {
            FrequencyData frequencyData = AlleleProtoAdaptor.toFrequencyData(alleleProp);
            frequency = frequencyData.getMaxFreq();

            PathogenicityData pathogenicityData = AlleleProtoAdaptor.toPathogenicityData(alleleProp);
            pathogenicity = calculatePathogenicity(annotation.getVariantEffect(), pathogenicityData, variantEffectPathogenicity);
            ClinVarData cVarData = pathogenicityData.getClinVarData();
            // Only use ClinVar data if it is backed up by assertions.
            if (cVarData.getReviewStatus().startsWith("no_assertion")) {
                isClinvarPathogenic = false;
            } else {
                isClinvarPathogenic = CLINVAR_PATHOGENIC.contains(cVarData.getPrimaryInterpretation());
            }
        }


        return VariantMetadata.of(frequency, pathogenicity, isClinvarPathogenic, annotation.getTranscriptAnnotations());
    }

    private static AlleleProto.AlleleKey createAlleleKey(GenomicVariant variant) {
        return AlleleProto.AlleleKey.newBuilder()
                .setChr(variant.contigId())
                .setPosition(variant.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.oneBased()))
                .setRef(variant.ref())
                .setAlt(variant.alt())
                .build();
    }

    /**
     * Calculate a pathogenicity score for the current variant in the same way that the Exomiser does.
     *
     * @param variantEffect     class of variant such as Missense, Nonsense, Synonymous, etc.
     * @param pathogenicityData Object representing the predicted pathogenicity of the data.
     * @param variantEffectPathogenicityScore pathogenicity score based on {@code variantEffect}
     * @return the predicted pathogenicity score.
     */
    private static float calculatePathogenicity(VariantEffect variantEffect,
                                                PathogenicityData pathogenicityData,
                                                float variantEffectPathogenicityScore) {
        if (pathogenicityData.isEmpty())
            return variantEffectPathogenicityScore;
        float predictedScore = pathogenicityData.getScore();
        return switch (variantEffect) {
            case MISSENSE_VARIANT -> pathogenicityData.hasPredictedScore()
                    ? predictedScore
                    : variantEffectPathogenicityScore;
            // there are cases where synonymous variants have been assigned a high MutationTaster score.
            // These looked to have been wrongly mapped and are therefore probably wrong. So we'll use the default score for these.
            case SYNONYMOUS_VARIANT -> variantEffectPathogenicityScore;
            default -> Math.max(predictedScore, variantEffectPathogenicityScore);
        };
    }

}
