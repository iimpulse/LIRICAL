package org.monarchinitiative.lirical.core.output.svg;

import org.monarchinitiative.lirical.core.analysis.AnalysisResults;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class Posttest2Svg extends Lirical2Svg {
    private static final Logger logger = LoggerFactory.getLogger(Posttest2Svg.class);

    private final AnalysisResults analysisResults;

    private final HpoDiseases diseases;

    private final int MINIMUM_DIFFERENTIALS_TO_SHOW = 3;

    private final int MAXIMUM_NUMBER_OF_DIFFERENTIAL_DX_TO_SHOW = 10;
    /**
     * Number of diseases whose posterior probability we will show in the top of the page.
     */
    private final int numDifferentialsToShowSVG;

    private final int n_belowThresholdDifferentials;

    private final int width;
    /** Coordinate of leftmost part of the 0-100% scale. */
    private final int XSTART = 10;
    /** Height of the 'posttest probability title*/
    private final int TITLE_HEIGHT = 20;
    /** Starting x position for the bars representing post-test probability. */
    private final int minX;
    /**rightmost location of scale/bars representing post-test probability. */
    private final int maxX;
    /** Y position where the probability scale is located. */
    private int probability_scale_Y_location;
    /** Width of the graphic we will show (scaling*width) */
    private final int scaledWidth;
    /** Proportion of SVG to fill with scale/bars */
    private final double scaling = 0.9;
    /** Height of the SVG, calculated dynamically based on number of bars to show */
    private final int height;
    /** Location on Y axis where we start writing (note: higher y is lower in final image).*/
    private final int Ybaseline = 20;
    /** Estimated height of a line of text */
    private final int TEXTHEIGHT = 15;
    /** Used to keep track of current Y position while we are constructing the SVG. */
    private int currentY;
    /** THis is the number of differential diagnoses that will have a detailed "box" in the
     * main HTML output. We limit the total number of bars on the posterior probqbility diagram
     * to be at most 10 and just link to the one after if there are more, but we need
     * to keep track of this to make correct SVG output.
     */
    private final int totalDetailedToShowText;
    /** The threshold that controls the number of detailed results to show in the HTML. */
    private final double thresholdPostTestProb;


    public Posttest2Svg(AnalysisResults results, HpoDiseases diseases, double threshold, int totalDetailedToShowText) {
        this.analysisResults = results;
        this.diseases = diseases;
        int n = Math.toIntExact(results.results().filter(result -> result.posttestProbability()>=threshold).count());
        n_belowThresholdDifferentials = n;
        thresholdPostTestProb = threshold;
        n = Math.max(n, MINIMUM_DIFFERENTIALS_TO_SHOW);
        n = Math.min(n, MAXIMUM_NUMBER_OF_DIFFERENTIAL_DX_TO_SHOW);
        this.numDifferentialsToShowSVG = n;
        width = 1000;
        scaledWidth = (int) (width * scaling);
        this.totalDetailedToShowText = totalDetailedToShowText;
        height = calculateHeight();
        this.minX = XSTART;
        this.maxX = scaledWidth;
    }

    /**
     * Add up all of the height adjustments used to get the total height needed for the SVG document
     * @return total height of the SVG
     */
    private int calculateHeight() {
        int y = Ybaseline + TITLE_HEIGHT + 15;
        y += 2 * MIN_VERTICAL_OFFSET;
        y += this.numDifferentialsToShowSVG * MIN_VERTICAL_OFFSET;
        y += this.numDifferentialsToShowSVG * (TEXTHEIGHT + BOX_HEIGHT + BOX_OFFSET);
        if (totalDetailedToShowText > numDifferentialsToShowSVG) {
            y += MIN_VERTICAL_OFFSET + TEXTHEIGHT + BOX_HEIGHT + BOX_OFFSET;
        }
        y += 20;
        return y;
    }


    /**
     * This method can be used to output the SVG code to any Java Writer. Currently,
     * we use this with a StringWriter to include the code in the HTML output (see {@link #getSvgString()}).
     *
     * @param writer Handle to a Writer object
     * @throws IOException If there is an IO error
     */
    private void writeSvg(Writer writer) throws IOException {
        this.currentY = Ybaseline;
        writeHeader(writer);
        writeTitle(writer);
        writeScale(writer);
        writePosttestBoxes(writer);
       // writeVerticalLines(writer);
        writeFooter(writer);
    }

    public String getSvgString() {
        try {
            StringWriter swriter = new StringWriter();
            writeSvg(swriter);
            return swriter.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ""; // return empty string upon failure
    }


    public int getNumDifferentialsToShowSVG() {
        return numDifferentialsToShowSVG;
    }


    private void writeTitle(Writer writer) throws IOException {
        this.currentY = 30;
        int xpos = 10;
        //font-family="Montserrat"
        writer.write("<text x=\"" + xpos + "\" y=\"" + this.currentY + "\" font-size=\"26\"  fill=\"" + DARKBLUE + "\">Post-test probability</text>\n");
        this.currentY += TITLE_HEIGHT;
    }

    private void writeHeader(Writer writer) throws IOException {
        writer.write("<svg width=\"" + width + "\" height=\"" + height + "\" " +
                "xmlns=\"http://www.w3.org/2000/svg\" " +
                "xmlns:svg=\"http://www.w3.org/2000/svg\">\n");
        writer.write("<!-- Created by LIRICAL -->\n");
        writer.write("<g>\n");
    }


    /**
     * Writes a horizontal scale ("X axis") from 0 to 100% with tick points.
     *
     * @param writer File handle
     * @throws IOException if there is an issue writing the SVG code
     */
    private void writeScale(Writer writer) throws IOException {

        writer.write("<line fill=\"none\" stroke=\"midnightblue\" stroke-width=\"2\" " +
                "x1=\"" + minX + "\" y1=\"" + currentY + "\" x2=\"" + maxX +
                "\" y2=\"" + currentY + "\"/>\n");
        int block = (scaledWidth - minX) / 10;
        currentY += 5;
        for (int i = 0; i <= 10; ++i) {
            int offset = XSTART + block * i;
            String percentage = String.format("%d%%", i * 10);
            writer.write("<line fill=\"none\" stroke=\"midnightblue\" stroke-width=\"2\" " +
                    "x1=\"" + (offset) + "\" y1=\"" + (currentY+5) + "\" x2=\"" + (offset) + "\" y2=\"" + (currentY-5) + "\"/>\n");
            writer.write(String.format("<text x=\"%d\" y=\"%d\" font-size=\"12px\" style=\"stroke: black; fill: black\">%s</text>\n",
                    (offset),
                    currentY + 20,
                    percentage));
        }
        probability_scale_Y_location = currentY;
        currentY += 20;
    }

    private void writePosttestBoxes(Writer writer) throws IOException {
        currentY += 2 * MIN_VERTICAL_OFFSET;
        AtomicInteger lastY = new AtomicInteger(currentY);
        int xOffset = 5;
        Map<TermId, HpoDisease> diseaseById = diseases.diseaseById();
        AtomicInteger rank = new AtomicInteger();
        analysisResults.resultsWithDescendingPostTestProbability()
                .limit(numDifferentialsToShowSVG)
                .forEachOrdered(result -> {
                    try {
                        double postprob = result.posttestProbability();
                        int boxwidth = (int) (scaledWidth * postprob);
                        // either show a b ox or (if the post-test prob is less than 2.5%) show a diamond to symbolize
                        // a small value
                        if (postprob > 0.025) {
                            writer.write(String.format("<rect height=\"%d\" width=\"%d\" y=\"%d\" x=\"%d\" " +
                                            "stroke-width=\"1\" stroke=\"#000000\" fill=\"%s\"/>\n",
                                    BOX_HEIGHT,
                                    boxwidth,
                                    currentY,
                                    XSTART,
                                    BRIGHT_GREEN));
                        } else {
                            writeDiamond(writer, XSTART, currentY);
                        }
                        currentY += MIN_VERTICAL_OFFSET;
                        // now write label of disease and HTML anchor
                        int current = rank.incrementAndGet();
                        HpoDisease disease = diseaseById.get(result.diseaseId());
                        String label = String.format("%d. %s", (current), prettifyDiseaseName(disease.diseaseName()));
                        String anchor = String.format("<a class=\"svg\" href=\"#diagnosis%d\">\n", current);
                        writer.write(anchor);
                        writer.write(String.format("<text x=\"%d\" y=\"%d\" font-size=\"14px\" font-style=\"normal\">%s</text>\n",
                                XSTART + xOffset,
                                currentY + BOX_HEIGHT + BOX_OFFSET,
                                label));
                        writer.write("</a>\n");
                        lastY.set(currentY);
                        currentY += TEXTHEIGHT + BOX_HEIGHT + BOX_OFFSET;
                    } catch (IOException e) {
                        logger.warn("Error: {}", e.getMessage(), e);
                    }
                });
        if (this.n_belowThresholdDifferentials > this.MAXIMUM_NUMBER_OF_DIFFERENTIAL_DX_TO_SHOW) {
            String message = String.format("An additional %d diseases were found to have a post-test probability above %.2f",
                    n_belowThresholdDifferentials - MAXIMUM_NUMBER_OF_DIFFERENTIAL_DX_TO_SHOW,
                    thresholdPostTestProb);
            String anchor = String.format("<a class=\"svg\" href=\"#diagnosis%d\">\n", 1+numDifferentialsToShowSVG);
            writer.write(anchor);
            writer.write(String.format("<text x=\"%d\" y=\"%d\" font-size=\"14px\" font-style=\"normal\">%s</text>\n",
                    XSTART + xOffset,
                    currentY + BOX_HEIGHT + BOX_OFFSET,
                    message));
            writer.write("</a>\n");
        }
        // a line at Y=0% posttest probability from the axis to the last entry
        writer.write("<line stroke-dasharray=\"1, 5\" x1=\"" + XSTART + "\" y1=\"" + probability_scale_Y_location +
                "\" x2=\"" + XSTART + "\" y2=\"" + lastY + "\" style=\"stroke:" + VIOLET +";\"></line>\n");
    }

    private void writeVerticalLines(Writer writer) throws IOException {
        int x = minX + (int)(thresholdPostTestProb * scaledWidth);
        writer.write("<line fill=\"none\" stroke=\"midnightblue\" stroke-width=\"1\" " +
                "x1=\"" + x + "\" y1=\"" + Ybaseline + "\" x2=\"" + x + "\" y2=\"" + height + "\"/>\n");

    }


}
