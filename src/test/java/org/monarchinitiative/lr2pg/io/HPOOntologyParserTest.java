package org.monarchinitiative.lr2pg.io;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.monarchinitiative.phenol.formats.hpo.HpoInheritanceTermIds;
import org.monarchinitiative.phenol.formats.hpo.HpoOntology;
import org.monarchinitiative.phenol.io.obo.hpo.HpoOboParser;
import org.monarchinitiative.phenol.ontology.data.ImmutableTermId;
import org.monarchinitiative.phenol.ontology.data.ImmutableTermPrefix;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.data.TermPrefix;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.monarchinitiative.phenol.ontology.algo.OntologyAlgorithm.existsPath;
import static org.monarchinitiative.phenol.ontology.algo.OntologyAlgorithm.getAncestorTerms;


public class HPOOntologyParserTest {

    private static HpoOntology ontology =null;

    private static TermPrefix hpoPrefix=null;
    private final static TermId PHENOTYPIC_ABNORMALITY = ImmutableTermId.constructWithPrefix("HP:0000118");


    @BeforeClass
    public static void setup() throws IOException {
        ClassLoader classLoader = HPOOntologyParserTest.class.getClassLoader();
        String hpoPath = classLoader.getResource("hp.obo").getFile();
        HpoOboParser parser = new HpoOboParser(new File(hpoPath));
        ontology=parser.parse();
        hpoPrefix = new ImmutableTermPrefix("HP");
    }


    @Test
    public void testNonNullPhenotypeOntology() {
        Assert.assertNotNull(ontology);
    }


    /** There are currently over 13000 terms, don't know exact number, but we should get over 10,000 */
    @Test
    public void testGetAtLeastTenThousandTerms() {
        int count = ontology.countAllTerms();
        Assert.assertTrue(count>10_000);
    }

    /** The term for "Sporadic" is in the inheritance subontology and not the phenotype subontology. */
    @Test
    public void testInCorrectSubontology1() {
        TermId sporadic = new ImmutableTermId(hpoPrefix,"0003745");
        Assert.assertTrue(existsPath(ontology,sporadic,HpoInheritanceTermIds.INHERITANCE_ROOT));
        Assert.assertFalse(existsPath(ontology,sporadic, PHENOTYPIC_ABNORMALITY));
    }



    /** The term for "Otitis media" is in the phenotype subontology and not the inheritance subontology */
    @Test
    public void testInCorrectSubontology2() {
        TermId otitisMedia = new ImmutableTermId(hpoPrefix,"0000388");
        Assert.assertFalse(existsPath(ontology,otitisMedia,HpoInheritanceTermIds.INHERITANCE_ROOT));
        Assert.assertTrue(existsPath(ontology,otitisMedia, PHENOTYPIC_ABNORMALITY));
    }

    /** Abnormality of the middle ear (HP:0000370) should have the ancestors Abnormality of the ear (HP:0000598)
     * and Phenotypic abnormality (HP:0000118). Note that ancestors includes the term itself! */
    @Test
    public void testGetAncestors() {
        TermId abnMiddleEar = new ImmutableTermId(hpoPrefix,"0000370");
        TermId abnEar = new ImmutableTermId(hpoPrefix,"0000598");
        TermId rootId = new ImmutableTermId(hpoPrefix,"0000118");
        Set<TermId> ancTermIds = getAncestorTerms(ontology,abnMiddleEar);
        TermId root = ImmutableTermId.constructWithPrefix("HP:0000001"); // the very root of the ontology
        Set<TermId> expected = new HashSet<>();
        expected.add(rootId);
        expected.add(abnEar);
        expected.add(abnMiddleEar);
        if (ancTermIds.contains(root)) { expected.add(root); }
        Assert.assertEquals(expected,ancTermIds);
    }


    /** The term for "Autosomal dominant inheritance" is in the inheritance subontology and not the phenotype subontology. */
    @Test
    public void testInCorrectSubontology3() {
        TermId autosomalDominant = new ImmutableTermId(hpoPrefix,"0000006");
        Assert.assertTrue(existsPath(ontology,autosomalDominant,HpoInheritanceTermIds.INHERITANCE_ROOT));
        Assert.assertFalse(existsPath(ontology,autosomalDominant, PHENOTYPIC_ABNORMALITY));
    }

    /** The term for "Functional abnormality of the bladder" is in the phenotype subontology and not the inheritance subontology. */
    @Test
    public void testInCorrectSubontology4() {
        TermId fctnlAbnBladder = new ImmutableTermId(hpoPrefix,"0000009");
        Assert.assertFalse(existsPath(ontology,fctnlAbnBladder,HpoInheritanceTermIds.INHERITANCE_ROOT));
        Assert.assertTrue(existsPath(ontology,fctnlAbnBladder, PHENOTYPIC_ABNORMALITY));
    }

    /**
     * Get the ancestors of HP:0000009
     */
    @Test
    public void testGetAncestors2() {
        TermId abnFuncBladder = new ImmutableTermId(hpoPrefix,"0000009");
        TermId abnBladder = new ImmutableTermId(hpoPrefix,"0000014");
        TermId abnlowerUrinary = new ImmutableTermId(hpoPrefix,"0010936");
        TermId abnormalityUrinary = new ImmutableTermId(hpoPrefix,"0000079");
        TermId abnormalityGenitourinary = new ImmutableTermId(hpoPrefix,"0000119");
        TermId phenotypicAbnormality = new ImmutableTermId(hpoPrefix,"0000118");

        Set<TermId> ancTermIds = getAncestorTerms(ontology,abnFuncBladder);
        Set<TermId> expected = new HashSet<>();
        expected.add(abnlowerUrinary);
        expected.add(abnBladder);
        expected.add(abnFuncBladder);
        expected.add(abnormalityUrinary);
        expected.add(abnormalityGenitourinary);
        expected.add(phenotypicAbnormality);
        TermId root = ImmutableTermId.constructWithPrefix("HP:0000001"); // the very root of the ontology
        if (ancTermIds.contains(root)) { expected.add(root); }
        Assert.assertEquals(expected,ancTermIds);
    }


}
