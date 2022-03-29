package org.monarchinitiative.lirical.io;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.monarchinitiative.lirical.TestResources;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.condition.OS.WINDOWS;


public class YamlParserTest {
    private static final Path TEST_YAML_DIR = TestResources.TEST_BASE.resolve("yaml");
    // Paths to the example YAML files in src/test/resources/yaml/
    private static final Path example1path = TEST_YAML_DIR.resolve("example1.yml");
    private static final Path example2path = TEST_YAML_DIR.resolve("example2.yml");

    private static final double EPSILON=0.000001;

    /**
     * In the YAML file, the exomiser path is given as
     * exomiser: /home/robinp/data/exomiserdata/1811_hg19.
     * Here we test if we can extract the correct mvstore and Jannovar files
     * This test is disabled on windows because it depends on the File separator (/ vs \).
     */
    @Test @DisabledOnOs(WINDOWS)
    void testExomiserData() {
        YamlParser parser = new YamlParser(example1path);
        assertEquals("/path/to/1802_hg19", parser.getExomiserDataDir());
    }

    /**
     * The default path for the background frequency is src/main/resources/background/ but it can be
     * overrridden in the YAML file
     */
    @Test
    void testBackFrequencyPath1() {
        YamlParser parser = new YamlParser(example1path);
        // We do not provide the background frequency path in this YAML file
        // therefore, the value should be not present
        Optional<String> backgroundOpt = parser.getBackgroundPath();
        assertFalse(backgroundOpt.isPresent());
    }

    /**
     * The default path for the background frequency is src/main/resources/background/ but it can be
     * overrridden in the YAML file
     */
    @Test
    void testBackFrequencyPath2() {
        YamlParser parser = new YamlParser(example2path);
        Optional<String> backgroundOpt = parser.getBackgroundPath();
        assertTrue(backgroundOpt.isPresent());
        String expected="/path/to/custom_location2";
        assertEquals(expected,backgroundOpt.get());
    }

    @Test
    void testDefaultDataPath() {
        YamlParser parser = new YamlParser(example1path);
        String datadir = parser.getDataDir();
        String expected = "data";
        assertEquals(expected,datadir);
    }


    @Test
    void testCustomDataPath2() {
        YamlParser parser = new YamlParser(example2path);
        String datadir = parser.getDataDir();
        String expected = "/path/to/custom_location1";
        assertEquals(expected,datadir);
    }

    /** example2.yml does not indicate the background frequency and thus isPresent should be false.*/
    @Test
    void testBackFrequencyPathNotPresent() {
        YamlParser parser = new YamlParser(example1path);
        Optional<String> backgroundOpt = parser.getBackgroundPath();
        assertFalse(backgroundOpt.isPresent());
    }

    @Test
    void testGetPrefix() {
        YamlParser yparser = new YamlParser(example2path); // prefix is pfeiffer1 for this YAML file
        assertEquals("example2",yparser.getPrefix());
    }

    @Test
    void testGetHpoIds() {
        YamlParser yparser = new YamlParser(example2path); // [ 'HP:0001363', 'HP:0011304', 'HP:0010055']
        String [] expected = {"HP:0001363", "HP:0011304", "HP:0010055"};
        List<String> hpos =yparser.getHpoTermList();
        assertEquals(expected.length,hpos.size());
        assertEquals(3,hpos.size());
        assertEquals(expected[1],hpos.get(1));
        assertEquals(expected[2],hpos.get(2));
    }

    @Test
    void testNegatedHpoIds1() {
        // example 1 has no negated HPOs
        YamlParser yparser = new YamlParser(example1path);
        List<TermId> emptyList = ImmutableList.of();
        assertEquals(0,yparser.getNegatedHpoTermList().size());
    }

    @Test
    void testNegatedHpoIds2() {
        // example 2 has one negated id
        YamlParser yparser = new YamlParser(example2path);
        String termid = "HP:0001328"; // the negated term
        List<String> expected = ImmutableList.of(termid);
        assertEquals(expected,yparser.getNegatedHpoTermList());
    }

    @Test
    void testOutDir1() {
        // example 1 does not have an out directory
        YamlParser yparser = new YamlParser(example1path);
        assertFalse(yparser.getOutDirectory().isPresent());
    }

    @Test
    void testOutDir2() {
        // example 2 has myoutdirectory
        YamlParser yparser = new YamlParser(example2path);
        String expected="myoutdirectory";
        assertTrue(yparser.getOutDirectory().isPresent());
        assertEquals(expected,yparser.getOutDirectory().get());
    }

    @Test
    void testGlobal1() {
        // example 1 does not have a keep entry
        YamlParser yparser = new YamlParser(example1path);
        assertFalse(yparser.global());
    }

    @Test
    void testGlobal2() {
        // example 2 has keep=true
        YamlParser yparser = new YamlParser(example2path);
        assertTrue(yparser.global());
    }


    @Test
    void testMinDiff1() {
        //example 1 has no mindiff entry,
        // the optional element should be empty
        YamlParser yparser = new YamlParser(example1path);
        assertFalse(yparser.mindiff().isPresent());
    }

    @Test
    void testMinDiff2() {
        //example 2 has  no mindiff entry
        YamlParser yparser = new YamlParser(example2path);
        assertFalse(yparser.mindiff().isPresent());
    }

    @Test
    void testThreshold1() {
        //example 1 has no threshold entry,
        // the optional element should be empty
        YamlParser yparser = new YamlParser(example1path);
        assertFalse(yparser.threshold().isPresent());
    }

    @Test
    void testThreshold2() {
        //example 2 has threshold: 0.05,
        // the optional element should be empty
        YamlParser yparser = new YamlParser(example2path);
        assertTrue(yparser.threshold().isPresent());
        double expected = 0.05;
        assertEquals(expected,yparser.threshold().get(),EPSILON);
    }

    @Test
    void testTsv1() {
        //example 1 has no tsv entry,
        YamlParser yparser = new YamlParser(example1path);
        assertFalse(yparser.doTsv());
    }

    @Test
    void testTsv2() {
        //example 2 has  tsv: true,
        YamlParser yparser = new YamlParser(example2path);
        assertTrue(yparser.doTsv());
    }

    @Test
    void testHtml1() {
        //example 1 has html with false entry,
        YamlParser yparser = new YamlParser(example1path);
        assertTrue(yparser.doHtml());
    }

    @Test
    void testHtml2() {
        //example 2 has  html: true,
        YamlParser yparser = new YamlParser(example2path);
        assertFalse(yparser.doHtml());
    }


}
