package org.monarchinitiative.lirical.configuration;

import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.io.LiricalDataException;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoader;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaderOptions;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaders;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.svart.assembly.GenomicAssemblies;
import org.monarchinitiative.svart.assembly.GenomicAssembly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;

class LoadUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadUtils.class);

    private LoadUtils() {

    }

    static Ontology loadOntology(Path ontologyPath) throws LiricalDataException {
        try {
            LOGGER.debug("Loading HPO from {}", ontologyPath.toAbsolutePath());
            return OntologyLoader.loadOntology(ontologyPath.toFile());
        } catch (PhenolRuntimeException e) {
            throw new LiricalDataException(e);
        }
    }

    static HpoDiseases loadHpoDiseases(Path annotationPath,
                                       Ontology hpo,
                                       HpoDiseaseLoaderOptions options) throws LiricalDataException {
        try {
            LOGGER.debug("Loading HPO annotations from {}", annotationPath.toAbsolutePath());
            HpoDiseaseLoader loader = HpoDiseaseLoaders.defaultLoader(hpo, options);
            return loader.load(annotationPath);
        } catch (IOException e) {
            throw new LiricalDataException(e);
        }
    }

    static BufferedReader openBundledBackgroundFrequencyFile(GenomeBuild genomeBuild) throws LiricalDataException {
        String name = switch (genomeBuild) {
            case HG19 -> "/background/background-hg19.tsv";
            case HG38 -> "/background/background-hg38.tsv";
        };
        InputStream is = LiricalBuilder.class.getResourceAsStream(name);
        if (is == null)
            throw new LiricalDataException("Background file for " + genomeBuild + " is not present at '" + name + '\'');
        LOGGER.debug("Loading bundled background variant frequencies from {}", name);
        return new BufferedReader(new InputStreamReader(is));
    }

    static GenomicAssembly parseSvartGenomicAssembly(GenomeBuild genomeAssembly) {
        switch (genomeAssembly) {
            case HG19:
                return GenomicAssemblies.GRCh37p13();
            default:
                LOGGER.warn("Unknown genome assembly {}. Falling back to GRCh38", genomeAssembly);
            case HG38:
                return GenomicAssemblies.GRCh38p13();
        }
    }
}
