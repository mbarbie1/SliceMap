/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.be.ua.mbarbier.slicemap.lib.transform;

import ij.IJ;
import ij.process.ImageProcessor;

/**
 *
 * @author mbarbier
 */
public class TransformImage {

	public static final int INDEX_TRANSLATION_X = 0;
    public static final int INDEX_TRANSLATION_Y = 1;
    public static final int INDEX_ROTATION = 2;
    public static final int INDEX_MIRRORY = 3;
	public static final int INDEX_MIRRORX = 4;

	public static ImageProcessor applyRigidMirrorXYTransform(ImageProcessor ip, double tx, double ty, double angle, boolean mirrorx, boolean mirrory) {
		ImageProcessor transformedImage = applyRigidTransform(ip, tx, ty, angle);
		if (mirrorx) {
			transformedImage.flipHorizontal();
		}
		if (mirrory) {
			transformedImage.flipVertical();
		}
		return transformedImage;
	}

	public static ImageProcessor applyRigidMirrorYTransform(ImageProcessor ip, double tx, double ty, double angle, boolean mirrory) {
		ImageProcessor transformedImage = applyRigidTransform(ip, tx, ty, angle);
		if (mirrory) {
			transformedImage.flipVertical();
		}
		return transformedImage;
	}

	public static ImageProcessor applySampleTransform(ImageProcessor ip, double[] pretvecRef, double[] tvecRef, double[] pretvecSample, double[] tvecSample, String method, double[] methodParameters) {

		ImageProcessor transformedImage = applyTransform(ip, pretvecSample, method, methodParameters);
		transformedImage = applyTransform(transformedImage, tvecSample, method, methodParameters);
		transformedImage = applyInverseTransform(transformedImage, tvecRef, method, methodParameters);
		transformedImage = applyInverseTransform(transformedImage, pretvecRef, method, methodParameters);

		return transformedImage;
	}

	public static ImageProcessor applyRefTransform(ImageProcessor ip, double[] pretvecRef, double[] tvecRef, double[] pretvecSample, double[] tvecSample, String method, double[] methodParameters) {

		ImageProcessor transformedImage = applyTransform(ip, pretvecRef, method, methodParameters);
		transformedImage = applyTransform(transformedImage, tvecRef, method, methodParameters);
		transformedImage = applyInverseTransform(transformedImage, tvecSample, method, methodParameters);
		transformedImage = applyInverseTransform(transformedImage, pretvecSample, method, methodParameters);

		return transformedImage;
	}

	public static ImageProcessor applyInverseRigidMirrorYTransform(ImageProcessor ip, double tx, double ty, double angle, boolean mirrory) {
		ImageProcessor transformedImage = ip.duplicate();
		if (mirrory) {
			transformedImage.flipVertical();
		}
		transformedImage = applyInverseRigidTransform(transformedImage, tx, ty, angle);
		return transformedImage;
	}

	public static ImageProcessor applyInverseRigidMirrorXYTransform(ImageProcessor ip, double tx, double ty, double angle, boolean mirrorx, boolean mirrory) {
		ImageProcessor transformedImage = ip.duplicate();
		if (mirrory) {
			transformedImage.flipVertical();
		}
		if (mirrorx) {
			transformedImage.flipHorizontal();
		}
		transformedImage = applyInverseRigidTransform(transformedImage, tx, ty, angle);
		return transformedImage;
	}

	public static ImageProcessor applyInverseRigidTransform(ImageProcessor ip, double tx, double ty, double angle) {

		ImageProcessor transformedImage = ip.duplicate();

		transformedImage.setInterpolationMethod(ImageProcessor.BILINEAR);
		transformedImage.setBackgroundValue(0);
		transformedImage.rotate(-angle);
		transformedImage.translate(-tx, ty);

		return transformedImage;
	}

	public static ImageProcessor applyRigidTransform(ImageProcessor ip, double tx, double ty, double angle) {

		ImageProcessor transformedImage = ip.duplicate();

		transformedImage.setInterpolationMethod(ImageProcessor.BILINEAR);
		transformedImage.setBackgroundValue(0);
		transformedImage.translate(tx, -ty);
		transformedImage.rotate(angle);

		return transformedImage;
	}

	public static ImageProcessor applyTransform(ImageProcessor ip, double[] tvec, String method, double[] methodParameters) {

		ImageProcessor transformedImage = ip;
		switch (method) {
			case "RIGID":
				transformedImage = applyRigidTransform(ip, methodParameters[INDEX_TRANSLATION_X] * tvec[INDEX_TRANSLATION_X], methodParameters[INDEX_TRANSLATION_Y] * tvec[INDEX_TRANSLATION_Y], methodParameters[INDEX_ROTATION] * tvec[INDEX_ROTATION]);
				break;
			case "RIGID_MIRROR":
				transformedImage = applyRigidMirrorYTransform(ip, methodParameters[INDEX_TRANSLATION_X] * tvec[INDEX_TRANSLATION_X], methodParameters[INDEX_TRANSLATION_Y] * tvec[INDEX_TRANSLATION_Y], methodParameters[INDEX_ROTATION] * tvec[INDEX_ROTATION], ((int) Math.abs((int) tvec[INDEX_MIRRORY]) % 2) > 0);
				break;
			case "RIGID_MIRROR_XY":
				transformedImage = applyRigidMirrorXYTransform(ip, methodParameters[INDEX_TRANSLATION_X] * tvec[INDEX_TRANSLATION_X], methodParameters[INDEX_TRANSLATION_Y] * tvec[INDEX_TRANSLATION_Y], methodParameters[INDEX_ROTATION] * tvec[INDEX_ROTATION], ((int) Math.abs((int) tvec[INDEX_MIRRORX]) % 2) > 0, ((int) Math.abs((int) tvec[INDEX_MIRRORY]) % 2) > 0);
				break;
			default:
				IJ.log("applyTransform:: Transformation method " + method + " unknown");
				break;
		}

		return transformedImage;
	}

	public static ImageProcessor applyInverseTransform(ImageProcessor ip, double[] tvec, String method, double[] methodParameters) {

		ImageProcessor transformedImage = ip;
		switch (method) {
			case "RIGID":
				transformedImage = applyInverseRigidTransform(ip, methodParameters[INDEX_TRANSLATION_X] * tvec[INDEX_TRANSLATION_X], methodParameters[INDEX_TRANSLATION_Y] * tvec[INDEX_TRANSLATION_Y], methodParameters[INDEX_ROTATION] * tvec[INDEX_ROTATION]);
				break;
			case "RIGID_MIRROR":
				transformedImage = applyInverseRigidMirrorYTransform(ip, methodParameters[INDEX_TRANSLATION_X] * tvec[INDEX_TRANSLATION_X], methodParameters[INDEX_TRANSLATION_Y] * tvec[INDEX_TRANSLATION_Y], methodParameters[INDEX_ROTATION] * tvec[INDEX_ROTATION], ((int) Math.abs((int) tvec[INDEX_MIRRORY])) % 2 > 0);
				break;
			case "RIGID_MIRROR_XY":
				transformedImage = applyInverseRigidMirrorXYTransform(ip, methodParameters[INDEX_TRANSLATION_X] * tvec[INDEX_TRANSLATION_X], methodParameters[INDEX_TRANSLATION_Y] * tvec[INDEX_TRANSLATION_Y], methodParameters[INDEX_ROTATION] * tvec[INDEX_ROTATION], ((int) Math.abs((int) tvec[INDEX_MIRRORX])) % 2 > 0, ((int) Math.abs((int) tvec[INDEX_MIRRORY])) % 2 > 0);
				break;
			default:
				IJ.log("applyTransform:: Transformation method " + method + " unknown");
				break;
		}

		return transformedImage;
	}

	public static double[] scaleTransform(double[] tvec, double scale) {

		double[] tmp = tvec.clone();
		tmp[INDEX_TRANSLATION_X] = tmp[INDEX_TRANSLATION_X] * scale;
		tmp[INDEX_TRANSLATION_Y] = tmp[INDEX_TRANSLATION_Y] * scale;

		return tmp;
	}
}
