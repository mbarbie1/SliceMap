package main.be.ua.mbarbier.external;

import ij.ImagePlus;
import ij.process.ImageProcessor;

/*
 * Object histogram,
 * 2 parameters imp en treshold
 * methodes:
 * *exec
 */
public class Hausdorff_Distance {

    double hausdorffDistance;
    double averagedHausdorffDistance;

	public Hausdorff_Distance() {
		this.hausdorffDistance = 0.0;
		this.averagedHausdorffDistance = 0.0;
	};

    public double getAveragedHausdorffDistance() {
        return averagedHausdorffDistance;
    }

    public double getHausdorffDistance() {
        return hausdorffDistance;
    }

    public void exec(ImagePlus mask1, ImagePlus mask2) {

        // Get outline of binary masks
        ImagePlus maskA = mask1.duplicate();
        ImagePlus maskB = mask2.duplicate();

        int pixelsContourA = maskA.getStatistics().histogram[maskA.getStatistics().histogram.length - 1];
        int pixelsContourB = maskB.getStatistics().histogram[maskB.getStatistics().histogram.length - 1];

        // For every pixelA>0 (part of outlineA): store coordinates (n=pixelsContourB times) in coordA
        int height = maskA.getHeight();
        int width = maskA.getWidth();
        ImageProcessor ip = maskA.getProcessor();
        int[][][] coordA = new int[2][pixelsContourB][pixelsContourA];
        int i = 0;
        for (int y = height - 1; y >= 0; y--) {
            for (int x = 0; x < width; x++) {
                float v = ip.getPixelValue(x, y);
                if (v > 0) {
                    for (int j = 0; j < pixelsContourB; j++) {
                        coordA[0][j][i] = x;
                    }
                    for (int j = 0; j < pixelsContourB; j++) {
                        coordA[1][j][i] = y;
                    }
                    i++;
                }
            }
        }
        // For every pixelB>0 (part of outlineB): store coordinates (n=pixelsContourA times) in coordB
        height = maskB.getHeight();
        width = maskB.getWidth();
        ip = maskB.getProcessor();
        int[][][] coordB = new int[2][pixelsContourB][pixelsContourA];
        i = 0;
        for (int y = height - 1; y >= 0; y--) {
            for (int x = 0; x < width; x++) {
                float v = ip.getPixelValue(x, y);
                if (v > 0) {
                    for (int j = 0; j < pixelsContourA; j++) {
                        //System.out.println(Integer.toString(j));
                        coordB[0][i][j] = x;
                    }
                    for (int j = 0; j < pixelsContourA; j++) {
                        coordB[1][i][j] = y;
                    }
                    i++;
                }
            }
        }

        //Calculate distance between every coordinate of A and B in 2 directions
        double[][] D = new double[pixelsContourB][pixelsContourA];
        for (i = 0; i < pixelsContourB; i++) {
            for (int j = 0; j < pixelsContourA; j++) {
                double diffX = java.lang.Math.pow(coordB[0][i][j] - coordA[0][i][j], 2);
                double diffY = java.lang.Math.pow(coordB[1][i][j] - coordA[1][i][j], 2);
                D[i][j] = java.lang.Math.sqrt(diffX + diffY);
            }
        }

        //Calculate for every pixel the minimum distance based on B
        double[] minB = new double[pixelsContourB];
        for (i = 0; i < pixelsContourB; i++) {
            minB[i] = D[i][0];
            for (int j = 0; j < pixelsContourA; j++) {
                if (minB[i] > D[i][j]) {
                    minB[i] = D[i][j];
                }
            }
        }

        double[] minA = new double[pixelsContourA];
        for (int j = 0; j < pixelsContourA; j++) {
            minA[j] = D[0][j];
            for (i = 0; i < pixelsContourB; i++) {
                if (minA[j] > D[i][j]) {
                    minA[j] = D[i][j];
                }
            }
        }

        //Define max of the minimum distances --> HausdorffDistance
        //Calculate sum of the minimum distance --> Averaged HaudorffDistance
        double maxminB;
        double summinB;
        summinB = 0;
        maxminB = minB[0];
        for (i = 0; i < pixelsContourB; i++) {
            summinB = summinB + minB[i];
            if (maxminB < minB[i]) {
                maxminB = minB[i];

            }
        }
        summinB = summinB / pixelsContourB;

        double maxminA;
        double summinA;
        summinA = 0;
        maxminA = minA[0];
        for (i = 0; i < pixelsContourA; i++) {
            summinA = summinA + minA[i];
            if (maxminA < minA[i]) {
                maxminA = minA[i];

            }
        }
        summinA = summinA / pixelsContourA;


        //HaudorffDistance = max(h(A,B),h(B,A))
        //Averaged HausdorffDistance = max(ah(A,B),ah(B,A))
        double hd;
        if (maxminB > maxminA) {
            hd = maxminB;
        } else {
            hd = maxminA;
        }

        double ahd;
        if (summinB > summinA) {
            ahd = summinB;
        } else {
            ahd = summinA;
        }

        D = null;
        coordA = null;
        coordB = null;
        minB = null;
        minA = null;

        this.hausdorffDistance = hd;
        this.averagedHausdorffDistance = ahd;

    }

}
