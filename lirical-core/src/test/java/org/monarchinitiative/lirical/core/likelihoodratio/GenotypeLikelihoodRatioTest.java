package org.monarchinitiative.lirical.core.likelihoodratio;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.monarchinitiative.lirical.core.model.Gene2Genotype;
import org.monarchinitiative.lirical.core.service.BackgroundVariantFrequencyService;
import org.monarchinitiative.phenol.annotations.formats.GeneIdentifier;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.monarchinitiative.phenol.constants.hpo.HpoModeOfInheritanceTermIds.AUTOSOMAL_DOMINANT;
import static org.monarchinitiative.phenol.constants.hpo.HpoModeOfInheritanceTermIds.AUTOSOMAL_RECESSIVE;

public class GenotypeLikelihoodRatioTest {

    private static final double EPSILON=0.0001;

    private static final String SAMPLE_ID = "JIM";
    private static final float PATHOGENICITY_THRESHOLD = .8f;
    private static final GeneIdentifier MADE_UP_GENE = GeneIdentifier.of(TermId.of("Fake:123"), "FAKE_SYMBOL");
    private static final GenotypeLikelihoodRatio.Options OPTIONS = new GenotypeLikelihoodRatio.Options(PATHOGENICITY_THRESHOLD, false);


    private static Gene2Genotype setupGeneToGenotype(int variantCount, int pathogenicClinvarCount, double sumOfPathBinScores) {
        Gene2Genotype g2g = mock(Gene2Genotype.class);
        when(g2g.geneId()).thenReturn(MADE_UP_GENE);
        when(g2g.variantCount()).thenReturn(variantCount);
        when(g2g.pathogenicClinVarCount(SAMPLE_ID)).thenReturn(pathogenicClinvarCount);
        when(g2g.hasVariants()).thenReturn(variantCount != 0);
        when(g2g.getSumOfPathBinScores(SAMPLE_ID, PATHOGENICITY_THRESHOLD)).thenReturn(sumOfPathBinScores); // mock that we find no pathogenic variant
        return g2g;
    }

    /**
     * If we find one variant that is listed as pathogenic in ClinVar, then we return the genotype
     * likelihood ratio of 1000 to 1.
     */
    @Test
    public void testOneClinVarVariant() {
        Gene2Genotype g2g = setupGeneToGenotype(1, 1, 0.8);
        GenotypeLikelihoodRatio genoLRmap = new GenotypeLikelihoodRatio(BackgroundVariantFrequencyService.of(Map.of(), 0.1), OPTIONS);
        double result = genoLRmap.evaluateGenotype(SAMPLE_ID, g2g, List.of(AUTOSOMAL_DOMINANT)).lr();
        double expected = 1000;
        Assertions.assertEquals(expected,result,EPSILON);
    }


    /**
     * If we find two variants listed as pathogenic in ClinVar, then we return the genotype
     * likelihood ratio of 1000*1000 to 1.
     */
    @Test
    public void testTwoClinVarVariants() {
        Gene2Genotype g2g = setupGeneToGenotype(2, 2, 1.6);
        GenotypeLikelihoodRatio genoLRmap = new GenotypeLikelihoodRatio(BackgroundVariantFrequencyService.of(Map.of(), 0.1), OPTIONS);
        double result = genoLRmap.evaluateGenotype(SAMPLE_ID, g2g, List.of(AUTOSOMAL_RECESSIVE)).lr();
        double expected = (double)1000*1000;
        Assertions.assertEquals(expected,result,EPSILON);
    }


    /**
     * We want to test what happens with a gene that has lots of variants but a pathogenic variant count sum of zero,
     * a lambda-disease of 1, and a lambda-background of 8.7. This numbers are taken from the HLA-B gene.
     */
    @Test
    public void testHLA_Bsituation() {
        // create a background map with just one gene for testing
        Map <TermId,Double> g2background = new HashMap<>();
        TermId HLAB = TermId.of("NCBIGene:3106");
        g2background.put(HLAB,8.7418); // very high lambda-background for HLAB
        GenotypeLikelihoodRatio glr = new GenotypeLikelihoodRatio(BackgroundVariantFrequencyService.of(Map.of(), 0.1), OPTIONS);
        Gene2Genotype g2g = setupGeneToGenotype(0, 0, 0.);
        double score = glr.evaluateGenotype(SAMPLE_ID, g2g, List.of(AUTOSOMAL_DOMINANT)).lr();
        double expected = 0.05; // heuristic score
        Assertions.assertEquals(expected,score,EPSILON);
    }

    /**
     * We want to test what happens with a gene that has lots of variants but a pathogenic variant count sum of zero,
     * a lambda-disease of 2, and a lambda-background of 8.7. This numbers are taken from a made-up  gene.
     */
    @Test
    public void testRecessiveManyCalledPathVariants() {
        // create a background map with just one gene for testing
        Map <TermId,Double> g2background = new HashMap<>();
        TermId madeUpGene = TermId.of("NCBIGene:42");
        g2background.put(madeUpGene,8.7418); // very high lambda-background for TTN
        GenotypeLikelihoodRatio glr = new GenotypeLikelihoodRatio(BackgroundVariantFrequencyService.of(Map.of(), 0.1), OPTIONS);
        Gene2Genotype g2g = setupGeneToGenotype(0, 0, 0.);
        double score = glr.evaluateGenotype(SAMPLE_ID, g2g,List.of(AUTOSOMAL_RECESSIVE)).lr();
        double expected = 0.05*0.05; // heuristic score for AR
        Assertions.assertEquals(expected,score,EPSILON);
    }

    @Test
    public void thrbExample() {
        GeneIdentifier thrbId = GeneIdentifier.of(TermId.of("NCBIGene:7068"), "THRB");

        Map<TermId, Double> of = Map.of(thrbId.id(), 0.006973);
        GenotypeLikelihoodRatio glr = new GenotypeLikelihoodRatio(BackgroundVariantFrequencyService.of(of, 0.1), OPTIONS);

        Gene2Genotype g2g = Mockito.mock(Gene2Genotype.class);

        when(g2g.geneId()).thenReturn(thrbId);
        when(g2g.hasVariants()).thenReturn(true);
        when(g2g.pathogenicClinVarCount(SAMPLE_ID)).thenReturn(0);
        when(g2g.pathogenicAlleleCount(SAMPLE_ID, PATHOGENICITY_THRESHOLD)).thenReturn(56);
        when(g2g.getSumOfPathBinScores(SAMPLE_ID, PATHOGENICITY_THRESHOLD)).thenReturn(44.80000);

        GenotypeLrWithExplanation explanation = glr.evaluateGenotype(SAMPLE_ID, g2g, List.of(AUTOSOMAL_RECESSIVE));

        // TODO - check
        assertThat(explanation.geneId(), equalTo(thrbId));
        assertThat(explanation.lr(), is(closeTo(1.719420800179587e109, EPSILON)));
        assertThat(explanation.explanation(), equalTo("THRB: P(G|D)=0.0000. P(G|&#172;D)=0.0000.  Mode of inheritance: autosomal recessive. Observed weighted pathogenic variant count: 44.80. &lambda;<sub>disease</sub>=2. &lambda;<sub>background</sub>=0.0070. log<sub>10</sub>(LR)=109.235"));
    }
}
