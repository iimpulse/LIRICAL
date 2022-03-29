package org.monarchinitiative.lirical.io;

import com.google.protobuf.util.JsonFormat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.lirical.TestResources;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.phenopackets.schema.v1.Phenopacket;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This test class tests the ingest of the BBS1.json (phenotpacket) and BBS1.yml files.
 * They should provide equivalent information (although the Phenopacket provides richer information)
 */
public class BBS1Test {
    private static YamlParser yamlparser;
    private static PhenopacketImporter phenopacketimporter;

    private static final Path TEST_PHENOPACKET_DIR = TestResources.TEST_BASE.resolve("phenopacket");
    private static final Path TEST_YAML_DIR = TestResources.TEST_BASE.resolve("yaml");


    private final String expectedId = "IV-5/family A";
    private final String expectedGenomeAssembly = "GRCh37";
    private final Path expectedVcf=Path.of("/path/to/examples/BBS1.vcf");


    @BeforeAll
    public static void init() throws IOException {
        yamlparser = new YamlParser(TEST_YAML_DIR.resolve("BBS1.yml"));

        try (BufferedReader reader = Files.newBufferedReader(TEST_PHENOPACKET_DIR.resolve("BBS1.json"))) {
            Phenopacket.Builder phenoPacketBuilder = Phenopacket.newBuilder();
            JsonFormat.parser().merge(reader, phenoPacketBuilder);
            phenopacketimporter = PhenopacketImporter.of(phenoPacketBuilder.build());
        }
    }

    @Test
    public void getIdPhenopacket() {
        assertEquals(expectedId,phenopacketimporter.getSampleId());
    }

    @Test
    public void getIdYaml() {
        assertEquals(expectedId,yamlparser.getSampleId());
    }

    @Test
    public void getGenomeAssemblyPhenopacket() {
        Optional<String> assemblyOptional = phenopacketimporter.getGenomeAssembly();
        assertThat(assemblyOptional.isPresent(), equalTo(true));
        assertEquals(expectedGenomeAssembly, assemblyOptional.get());
    }

    @Test
    public void testGetGenomeAssemblyYaml() {
        assertEquals(expectedGenomeAssembly,yamlparser.getGenomeAssembly());
    }

    @Test
    public void testGetVcfPhenopacket() {
        Optional<Path> vcfPath = phenopacketimporter.getVcfPath();
        assertThat(vcfPath.isPresent(), equalTo(true));
        assertThat(vcfPath.get(), equalTo(expectedVcf));
    }

    @Test
    public void testGetVcfYaml() {
        Optional<Path> vcfOpt = yamlparser.getOptionalVcfPath();
        assertTrue(vcfOpt.isPresent());
        assertEquals(expectedVcf, vcfOpt.get());
    }

    /**
     * Note that the YAML Parser removes the trailing slash of the exomiser data directory path, if present.
     */
    @Test
    public void testGetExomiserPathYaml() {
        assertEquals("/path/to/exomiser_data/1802_hg19",yamlparser.getExomiserDataDir());
    }

    @Test
    public void testGetHpoIdsYaml() {
        String [] expected = {"HP:0007843","HP:0001513","HP:0000608","HP:0000486"};
        List<String> termList = yamlparser.getHpoTermList();
        assertEquals(4,termList.size());
        assertEquals(termList.get(0),expected[0]);
        assertEquals(termList.get(1),expected[1]);
        assertEquals(termList.get(2),expected[2]);
        assertEquals(termList.get(3),expected[3]);
    }

    @Test
    public void testGetHpoIdsPhenopacket() {
        List<TermId> terms = phenopacketimporter.getHpoTerms();
        TermId expected1 = TermId.of("HP:0007843");
        TermId expected2 = TermId.of("HP:0001513");
        TermId expected3 = TermId.of("HP:0000608");
        TermId expected4 = TermId.of("HP:0000486");
        assertTrue(terms.contains(expected1));
        assertTrue(terms.contains(expected2));
        assertTrue(terms.contains(expected3));
        assertTrue(terms.contains(expected4));
    }

    @Test
    public void testGetExlucedTermYaml() {
        final String expected = "HP:0001328"; // only one excluded term
        final List<String> excludedlist = yamlparser.getNegatedHpoTermList();
        assertEquals(1,excludedlist.size());
        assertEquals(expected,excludedlist.get(0));
    }

    @Test
    public void testGetExcludedTermsPhenopacket() {
        TermId expected = TermId.of("HP:0001328"); // only one excluded term
        List<TermId> excluded = phenopacketimporter.getNegatedHpoTerms();
        assertEquals(1,excluded.size());
        assertEquals(expected,excluded.get(0));
    }

    @Test
    public void testGetPrefixYaml() {
        final String expectedPrefix = "BBS1";
        assertEquals(expectedPrefix,yamlparser.getPrefix());
    }



}
