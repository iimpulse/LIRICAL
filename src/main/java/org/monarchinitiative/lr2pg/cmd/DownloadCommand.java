package org.monarchinitiative.lr2pg.cmd;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.monarchinitiative.lr2pg.io.HpoDownloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Download a number of files needed for the analysis
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
@Parameters(commandDescription = "Download files for LR2PG")
public class DownloadCommand extends Lr2PgCommand {
    private static final Logger logger = LoggerFactory.getLogger(DownloadCommand.class);

    @Parameter(names={"-d","--data"}, description ="directory to download data (default: data)" )
    private String datadir="data";
    @Parameter(names={"-o","--overwrite"}, description = "overwrite prevously downloaded files, if any")
    private boolean overwrite;

    public DownloadCommand() {
    }


    @Override
    public void run() {
        logger.info(String.format("Download analysis to %s", datadir));
        HpoDownloader downloader = new HpoDownloader(datadir, overwrite);
        downloader.download();
    }
}
