/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.be.ua.mbarbier.slicemap.lib.image;

import ij.ImagePlus;

/**
 *
 * @author mbarbier
 */
public class ImagesResized {

	ImagePlus imp1;
	ImagePlus imp2;
	int[] offset1 = new int[2];
	int[] offset2 = new int[2];

	public ImagesResized(ImagePlus imp1, ImagePlus imp2, int newX1, int newY1, int newX2, int newY2) {
		this.imp1 = imp1;
		this.imp2 = imp2;
		this.offset1 = new int[]{newX1, newY1};
		this.offset2 = new int[]{newX2, newY2};
	}

	public ImagePlus getImp1() {
		return imp1;
	}

	public void setImp1(ImagePlus imp1) {
		this.imp1 = imp1;
	}

	public ImagePlus getImp2() {
		return imp2;
	}

	public void setImp2(ImagePlus imp2) {
		this.imp2 = imp2;
	}

	public int[] getOffset1() {
		return offset1;
	}

	public void setOffset1(int[] offset1) {
		this.offset1 = offset1;
	}

	public int[] getOffset2() {
		return offset2;
	}

	public void setOffset2(int[] offset2) {
		this.offset2 = offset2;
	}

}
