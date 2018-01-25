/*
 * The MIT License
 *
 * Copyright 2018 University of Antwerp.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package main.be.ua.mbarbier.slicemap.lib.image;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Plot;
import ij.plugin.Binner;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import java.util.Arrays;
import main.be.ua.mbarbier.slicemap.lib.Lib;

/**
 *
 * @author mbarbier
 */
public class LibImageJava7 {

	public static boolean debug = false;
	
    public static ImagePlus binImageAlternative( ImagePlus imp, int binning ) {

        ij.plugin.Binner binner = new Binner();
        ImagePlus imp_scaled = binner.shrink(imp, binning, binning, 1, Binner.AVERAGE);
        
        return imp_scaled;
    }

	/**
     * Subtract the background intensity of an image using a percentile (minus exactly zero pixels)
     *
     * @param nBins
     * @param ref
     * @param ip
     * @return
     */
    public static ImageProcessor subtractBackground(ImageProcessor ip, double perc) {

        ip.resetRoi();
        // Obtain histograms
        ImageStatistics ipStats;
        ipStats = ip.getStatistics();
        int[] ipH;
        switch (ip.getBitDepth()) {
            case 16:
                ipH = ipStats.histogram16;
                break;
            case 8:
                ipH = ipStats.histogram;
                break;
            default:
                ipH = ipStats.histogram;
                break;
        }
        double ipArea = ipStats.area;
        double ipZeroArea = ipH[0];
        double ipNonZeroArea = ipArea - ipZeroArea;

        // Remove zero intensity from histogram
        ipH = Arrays.copyOfRange(ipH, 1, ipH.length);

        // Normalize to number of nonzero pixels
        double[] ipHd = new double[ipH.length];
        for (int i = 0; i < ipH.length; i++) {
            ipHd[i] = (double) ipH[i] / ipNonZeroArea;
        }

        // Cumulative distributions
        double[] ipHcum = new double[ipH.length];
        ipHcum[0] = ipHd[0];
        for (int i = 1; i < ipH.length; i++) {
            ipHcum[i] = ipHcum[i - 1] + ipHd[i];
        }
		
        // Compute pixelfractions
        //double[] bins = Lib.linearSpacedSequence(0.0, 1.0, nBins);
		double[] bins = new double[]{perc/100.0};
        // Search indices of the bins in the ref
        int[] ipIdx = findIndicesBins(ipHcum, bins);
        double[] ipIdxd = Lib.intArrayToDoubleArray(ipIdx);

        if (debug) {
            double[] x = Lib.intArrayToDoubleArray(Lib.intSequence(1, ipHcum.length));
            IJ.log("N bins: " + x.length);
            Plot ph_ip = new Plot("sample cumulative histogram", "Intensity", "pixel number", x, ipHd);
            Plot pc_ip = new Plot("sample cumulative histogram", "Intensity", "Cumulative histogram", x, ipHcum);
            Plot pi_ip = new Plot("sample indices bins", "index", "Cumulative histogram", bins, ipIdxd);
            ph_ip.show();
            pc_ip.show();
            pi_ip.show();
        }

        // Subtract intensity
        ImageProcessor ip_new = ip.duplicate();
		ip_new.subtract(ipIdxd[0]);
        ImagePlus imp_new = new ImagePlus("subtracted " + ipIdxd[0] + " image", ip_new);

        return ip_new;
    }
	
    public static int[] findIndicesBins(double[] cumValues, double[] bins) {

        int n = bins.length;
        int[] idx = new int[n];
        double eps = 0.00001;

        for (int i = 0; i < n; i++) {
            idx[i] = findIndexThreshold(cumValues, bins[i] - eps);
        }

        return idx;
    }
	
	/**
     * Find the index of the first value of an array of increasing values which
     * is larger than or equal to the threshold
     *
     * @param incValues
     * @param t
     * @return index as integer of threshold ( if not found, -1 is returned )
     */
    public static int findIndexThreshold(double[] incValues, double t) {

        int n = incValues.length;
        for (int i = 0; i < n; i++) {
            if (incValues[i] >= t) {
                return i;
            }
        }
        return -1;
    }
}
