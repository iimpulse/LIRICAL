package org.monarchinitiative.lirical.core.likelihoodratio;

import org.monarchinitiative.phenol.annotations.constants.hpo.HpoModeOfInheritanceTermIds;
import org.monarchinitiative.phenol.annotations.formats.GeneIdentifier;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Objects;


public class GenotypeLrWithExplanation  {
    private final GeneIdentifier geneId;
    /** The likelihood ratio of the genotype. */
    private final double lr;
    private final String explanation;

    static GenotypeLrWithExplanation noVariantsDetectedAutosomalRecessive(GeneIdentifier geneId, double ratio) {
        final String expl = String.format("log<sub>10</sub>(LR)=%.3f. No variants detected with autosomal recessive disease.", Math.log10(ratio));
        return new GenotypeLrWithExplanation(geneId, ratio, expl);
    }

    static GenotypeLrWithExplanation noVariantsDetectedAutosomalDominant(GeneIdentifier geneId, double ratio) {
        final String expl = String.format("log<sub>10</sub>(LR)=%.3f. No variants detected.", Math.log10(ratio));
        return new GenotypeLrWithExplanation(geneId, ratio, expl);
    }

    static GenotypeLrWithExplanation twoPathClinVarAllelesRecessive(GeneIdentifier geneId, double ratio) {
        final String expl = String.format("log<sub>10</sub>(LR)=%.3f. Two pathogenic ClinVar variants detected with autosomal recessive disease.",  Math.log10(ratio));
        return new GenotypeLrWithExplanation(geneId, ratio, expl);
    }

    static GenotypeLrWithExplanation pathClinVar(GeneIdentifier geneId, double ratio) {
        final String expl = String.format("log<sub>10</sub>(LR)=%.3f. Pathogenic ClinVar variant detected.", Math.log10(ratio));
        return new GenotypeLrWithExplanation(geneId, ratio, expl);
    }

     static GenotypeLrWithExplanation explainOneAlleleRecessive(GeneIdentifier geneId, double ratio, double observedWeightedPathogenicVariantCount, double lambda_background) {
        int lambda_disease = 2;
        String expl = String.format("log<sub>10</sub>(LR)=%.3f. One pathogenic allele detected with autosomal recessive disease. " +
             "Observed weighted pathogenic variant count: %.2f. &lambda;<sub>disease</sub>=%d. &lambda;<sub>background</sub>=%.4f.",
                Math.log10(ratio), observedWeightedPathogenicVariantCount, lambda_disease, lambda_background);
         return new GenotypeLrWithExplanation(geneId, ratio, expl);
    }


    static GenotypeLrWithExplanation explainPathCountAboveLambdaB(GeneIdentifier geneId, double ratio, TermId MoI, double lambda_background, double observedWeightedPathogenicVariantCount) {
        int lambda_disease = 1;
        if (MoI.equals(HpoModeOfInheritanceTermIds.AUTOSOMAL_RECESSIVE) || MoI.equals(HpoModeOfInheritanceTermIds.X_LINKED_RECESSIVE)) {
            lambda_disease = 2;
        }
        String expl = String.format("log<sub>10</sub>(LR)=%.3f. %s. Heuristic for high number of observed predicted pathogenic variants. "
                        + "Observed weighted pathogenic variant count: %.2f. &lambda;<sub>disease</sub>=%d. &lambda;<sub>background</sub>=%.4f.",
                Math.log10(ratio), getMoIString(MoI), observedWeightedPathogenicVariantCount, lambda_disease, lambda_background);
        return new GenotypeLrWithExplanation(geneId, ratio, expl);
    }

    static GenotypeLrWithExplanation explanation(GeneIdentifier geneId, double ratio, TermId modeOfInh, double lambda_b, double D, double B, double observedWeightedPathogenicVariantCount) {
        int lambda_disease = 1;
        if (modeOfInh.equals(HpoModeOfInheritanceTermIds.AUTOSOMAL_RECESSIVE) || modeOfInh.equals(HpoModeOfInheritanceTermIds.X_LINKED_RECESSIVE)) {
            lambda_disease = 2;
        }
        String msg = String.format("P(G|D)=%.4f. P(G|&#172;D)=%.4f", D, B);
        msg = String.format("log<sub>10</sub>(LR)=%.3f %s. %s. Observed weighted pathogenic variant count: %.2f. &lambda;<sub>disease</sub>=%d. &lambda;<sub>background</sub>=%.4f.",
                Math.log10(ratio), msg, getMoIString(modeOfInh), observedWeightedPathogenicVariantCount,  lambda_disease, lambda_b);
        return new GenotypeLrWithExplanation(geneId, ratio, msg);
    }

    private static String getMoIString(TermId MoI) {
        if (MoI.equals(HpoModeOfInheritanceTermIds.AUTOSOMAL_DOMINANT)) {
            return " Mode of inheritance: autosomal dominant";
        } else if (MoI.equals(HpoModeOfInheritanceTermIds.AUTOSOMAL_RECESSIVE)) {
            return " Mode of inheritance: autosomal recessive";
        } else if (MoI.equals(HpoModeOfInheritanceTermIds.X_LINKED_RECESSIVE)) {
            return " Mode of inheritance: X-chromosomal recessive";
        } else if (MoI.equals(HpoModeOfInheritanceTermIds.X_LINKED_DOMINANT)) {
            return " Mode of inheritance: X-chromosomal recessive";
        }
        return " Mode of inheritance: not available"; // should never happen
    }

    public static GenotypeLrWithExplanation of(GeneIdentifier geneId, double lr, String explanation) {
        return new GenotypeLrWithExplanation(geneId, lr, explanation);
    }

    private GenotypeLrWithExplanation(GeneIdentifier geneId, double lr, String explanation) {
        this.geneId = Objects.requireNonNull(geneId);
        this.lr = lr;
        this.explanation = Objects.requireNonNull(explanation, "Explanation must not be null");
    }


    public GeneIdentifier geneId() {
        return geneId;
    }

    public double lr() {
        return lr;
    }

    public String explanation() {
        return explanation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GenotypeLrWithExplanation that = (GenotypeLrWithExplanation) o;
        return Double.compare(that.lr, lr) == 0 && Objects.equals(explanation, that.explanation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lr, explanation);
    }

    @Override
    public String toString() {
        return "GenotypeLrWithExplanation{" +
                "LR=" + lr +
                ", explanation='" + explanation + '\'' +
                '}';
    }
}
