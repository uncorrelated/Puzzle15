package com.uncorrelated.p15;

import java.awt.Image;
import java.awt.Component;
import java.awt.MediaTracker;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.PixelGrabber;
import java.awt.image.WritableRaster;
import java.util.Hashtable;

public class ImageConverter {
	public static BufferedImage convert(Image img) {
		if(img instanceof BufferedImage)
			return (BufferedImage)img;
		MediaTracker tracker = new MediaTracker(new Component(){});
		tracker.addImage(img, 0);
		try {
			tracker.waitForAll();
			PixelGrabber pixelGrabber = new PixelGrabber(img, 0, 0, -1, -1, false);
			pixelGrabber.grabPixels();
			ColorModel cm = pixelGrabber.getColorModel();
			final int w = pixelGrabber.getWidth();
			final int h = pixelGrabber.getHeight();
			WritableRaster raster = cm.createCompatibleWritableRaster(w, h);
			BufferedImage renderedImage = new BufferedImage(cm, raster,
					cm.isAlphaPremultiplied(), new Hashtable());
			renderedImage.getRaster().setDataElements(0, 0, w, h,
					pixelGrabber.getPixels());
			return renderedImage;
		} catch (InterruptedException e) {
			return null;
		}
	}
}
