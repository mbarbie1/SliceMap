/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.ua.mbarbier.slicemap.lib.image.colocalization;

import algorithms.CostesSignificanceTest;
import algorithms.Histogram2D;
import algorithms.KendallTauRankCorrelation;
import algorithms.LiHistogram2D;
import algorithms.LiICQ;
import algorithms.MandersColocalization;
import algorithms.MissingPreconditionException;
import algorithms.PearsonsCorrelation;
import algorithms.SpearmanRankCorrelation;
import ij.ImagePlus;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.NumericType;

/**
 * This class implements some of the co-localization algorithm's from coloc_2
 * 
 * @author Michael
 * @param <T>
 */
public class Colocalization<T extends RealType< T > & NativeType< T > & NumericType< T > > {
    
    // the images to work on
    Img<T> img1, img2;

    protected PearsonsCorrelation<T> pearsonsCorrelation;
    protected LiHistogram2D<T> liHistogramCh1;
    protected LiHistogram2D<T> liHistogramCh2;
    protected LiICQ<T> liICQ;
    protected SpearmanRankCorrelation<T> SpearmanRankCorrelation;
    protected MandersColocalization<T> mandersCorrelation;
    protected KendallTauRankCorrelation<T> kendallTau;
    protected Histogram2D<T> histogram2D;
    protected CostesSignificanceTest<T> costesSignificance;

    boolean useLiCh1 = false;
    boolean useLiCh2 = false;
    boolean useLiICQ = false;
    boolean useSpearmanRank = false;
    boolean useManders = false;
    boolean useKendallTau = false;
    boolean useScatterplot = false;
    boolean useCostes = false;
    int psf = 3;
    int nrCostesRandomisations = 10;
    boolean displayShuffledCostes = false;

    public Colocalization() {
        super();
    };

    public Colocalization( ImagePlus imp1, ImagePlus imp2 ) {
        super();
        this.img1 = ImageJFunctions.wrap(imp1);//.convertFloat(imp1);
        this.img2 = ImageJFunctions.wrap(imp2);

    };

    public double getPearson() {
        
        pearsonsCorrelation = new PearsonsCorrelation<>(PearsonsCorrelation.Implementation.Fast);
        try {
            return pearsonsCorrelation.calculatePearsons( this.img1, this.img2 );
        } catch (MissingPreconditionException ex) {
            Logger.getLogger(Colocalization.class.getName()).log(Level.SEVERE, null, ex);
        }
        return -2;
    }

    public void coloc() {
        
        // Parse algorithm options
        pearsonsCorrelation = new PearsonsCorrelation<T>(PearsonsCorrelation.Implementation.Fast);
        if (useLiCh1) {
            liHistogramCh1 = new LiHistogram2D<T>("Li - Ch1", true);
        }
        if (useLiCh2) {
            liHistogramCh2 = new LiHistogram2D<T>("Li - Ch2", false);
        }
        if (useLiICQ) {
            liICQ = new LiICQ<T>();
        }
        if (useSpearmanRank) {
            SpearmanRankCorrelation = new SpearmanRankCorrelation<T>();
        }
        if (useManders) {
            mandersCorrelation = new MandersColocalization<T>();
        }
        if (useKendallTau) {
            kendallTau = new KendallTauRankCorrelation<T>();
        }
        if (useScatterplot) {
            histogram2D = new Histogram2D<T>("2D intensity histogram");
        }
        if (useCostes) {
            costesSignificance = new CostesSignificanceTest<T>(pearsonsCorrelation,
                    psf, nrCostesRandomisations, displayShuffledCostes);
        }

	}
}
