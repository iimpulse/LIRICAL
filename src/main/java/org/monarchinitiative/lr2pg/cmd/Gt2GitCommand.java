package org.monarchinitiative.lr2pg.cmd;



import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.collect.ImmutableList;
import de.charite.compbio.jannovar.data.JannovarData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.h2.mvstore.MVStore;
import org.monarchinitiative.exomiser.core.genome.GenomeAssembly;
import org.monarchinitiative.exomiser.core.genome.JannovarVariantAnnotator;
import org.monarchinitiative.exomiser.core.model.ChromosomalRegionIndex;
import org.monarchinitiative.exomiser.core.model.RegulatoryFeature;
import org.monarchinitiative.lr2pg.configuration.Lr2PgFactory;
import org.monarchinitiative.lr2pg.exception.Lr2pgException;
import org.monarchinitiative.lr2pg.gt2git.GenicIntoleranceCalculator;

import java.io.File;
import java.util.List;

/**
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
@Parameters(commandDescription = "Calculation of background variant frequency")
public class Gt2GitCommand extends Lr2PgCommand {
    private static final Logger logger = LogManager.getLogger();
    @Parameter(names={"-d","--data"}, description ="directory to download data (default: data)" )
    private String datadir="data";
    /** One of HG38 (default) or HG19. */
    private final GenomeAssembly genomeAssembly;
    /** Name of the output file (e.g., background-hg19.txt). Determined automatically based on genome build..*/
    private final String outputFileName;
    /** (e.g., ="/Users/peterrobinson/Documents/data/exomiser/1802_hg19/1802_hg19_variants.mv.db") */
    @Parameter(names={"-m","--mvStorePath"}, description = "path to Exomiser MVStore file")
    private String mvStorePath;

    /** Path of the Jannovar file. Note this can be taken from the Exomiser distribution, e.g.,
     * {@code exomiser/1802_hg19/1802_hg19_transcripts_refseq.ser}. */
    @Parameter(names={"-j","--jannovar"}, description = "path to Jannovar transcript file")
    private String jannovarFile;
    /** SHould be one of hg19 or hg38. */
    @Parameter(names={"-g", "--genome"}, description = "string representing the genome assembly (hg19,hg38)")
    private String genomeAssemblyString="hg38";
    /** If true, calculate the distribution of ClinVar pathogenicity scores. */
    @Parameter(names="--clinvar", description = "determine distribution of ClinVar pathogenicity scores")
    private boolean doClinvar;

    /**
     *
     */
    public Gt2GitCommand(){
        if (genomeAssemblyString.toLowerCase().contains("hg19")) {
            this.genomeAssembly=GenomeAssembly.HG19;
            outputFileName="background-hg19.txt";
        } else if (genomeAssemblyString.toLowerCase().contains("hg38")) {
            this.genomeAssembly=GenomeAssembly.HG38;
            outputFileName="background-hg19.txt";
        } else {
            logger.warn("Could not determine genome assembly from argument: \""+
                    genomeAssemblyString +"\". We will use the default of hg38");
            this.genomeAssembly=GenomeAssembly.HG38;
            outputFileName="background-hg38.txt";
        }
    }

    @Override
    public void run() throws Lr2pgException  {
        if (this.mvStorePath ==null) {
            throw new Lr2pgException("Need to specify the MVStore file: -m <mvStorePath> to run gt2git command!");
        }
        if (this.jannovarFile==null) {
            throw new Lr2pgException("Need to specify the jannovar transcript file: -j <jannovar> to run gt2git command!");
        }


        Lr2PgFactory.Builder builder = new Lr2PgFactory.Builder()
                .jannovarFile(jannovarFile)
                .mvStore(mvStorePath);

        Lr2PgFactory factory = builder.build();

        MVStore alleleStore = factory.mvStore();
        JannovarData jannovarData = factory.jannovarData();
        List<RegulatoryFeature> emtpylist = ImmutableList.of();
        ChromosomalRegionIndex<RegulatoryFeature> emptyRegionIndex = ChromosomalRegionIndex.of(emtpylist);
        JannovarVariantAnnotator jannovarVariantAnnotator = new JannovarVariantAnnotator(genomeAssembly, jannovarData, emptyRegionIndex);
        createOutputDirectoryIfNecessary();
        String outputpath=String.format("%s%s%s",this.datadir,File.separator,this.outputFileName);
       GenicIntoleranceCalculator calculator = new GenicIntoleranceCalculator(jannovarVariantAnnotator,alleleStore,outputpath,this.doClinvar);
       calculator.run();
    }


    private void createOutputDirectoryIfNecessary(){
        File dir = new File(this.datadir);
        if (! dir.exists() ) {
            boolean b = dir.mkdir();
            if (b) {
                logger.info("Successfully created directory at " + this.datadir);
            } else {
                logger.fatal("Unable to create directory at {}. Terminating program",this.datadir);
                System.exit(1);
            }
        }
    }

}
