package be.ua.mbarbier.external;

import java.awt.Polygon;
import ij.gui.Roi;

/*
 * Object histogram,
 * 2 parameters imp en treshold
 * methodes:
 * *exec
 */
public class Hausdorff_Distance2 {

    double hausdorffDistance;
    double averagedHausdorffDistance;

	public Hausdorff_Distance2() {
		this.hausdorffDistance = 0.0;
		this.averagedHausdorffDistance = 0.0;
	};

    public double getAveragedHausdorffDistance() {
        return averagedHausdorffDistance;
    }

    public double getHausdorffDistance() {
        return hausdorffDistance;
    }

    public void exec(Roi roiA, Roi roiB) {

        // Get outline of binary masks
        Polygon polA = roiA.getPolygon();
        Polygon polB = roiB.getPolygon();
        int pixelsContourA = polA.npoints;
        int pixelsContourB = polB.npoints;

        //Calculate distance between every coordinate of A and B in 2 directions
        double[][] D = new double[pixelsContourB][pixelsContourA];
        for (int i = 0; i < pixelsContourB; i++) {
            for (int j = 0; j < pixelsContourA; j++) {
                double diffX = java.lang.Math.pow(polB.xpoints[i] - polA.xpoints[j], 2);
                double diffY = java.lang.Math.pow(polB.ypoints[i] - polA.ypoints[j], 2);
                D[i][j] = java.lang.Math.sqrt(diffX + diffY);
            }
        }

        //Calculate for every pixel the minimum distance based on B
        double[] minB = new double[pixelsContourB];
        for (int i = 0; i < pixelsContourB; i++) {
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
            for (int i = 0; i < pixelsContourB; i++) {
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
        for (int i = 0; i < pixelsContourB; i++) {
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
        for (int i = 0; i < pixelsContourA; i++) {
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
        minB = null;
        minA = null;

        this.hausdorffDistance = hd;
        this.averagedHausdorffDistance = ahd;

    }

}
