package com.acmerobotics.library.vision;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class ImageOverlay {
	
	public enum ImageRegion {
		TOP_LEFT,
		TOP_CENTER,
		TOP_RIGHT,
		BOTTOM_LEFT,
		BOTTOM_CENTER,
		BOTTOM_RIGHT
	}
	
	private Scalar bgColor = null;
	
	private Mat image;
	private double width, height;
	
	private int padding;
	private int[] offsets = {0, 0, 0, 0, 0, 0};
	
	public ImageOverlay(Mat image, int padding) {
		this.image = image;
		this.padding = padding;
		this.width = image.width();
		this.height = image.height();
	}
	
	public ImageOverlay(Mat image) {
		this(image, 10);
	}
	
	public void drawText(String text, ImageRegion region, int font, double height, Scalar color, int thickness) {
		Size origSize = Imgproc.getTextSize(text, font, 1, thickness, null);
		double fontScale = height / ((origSize.height + 2 * padding) / this.height);
		Size newSize = Imgproc.getTextSize(text, font, fontScale, thickness, null);
		
		int offset = offsets[region.ordinal()];
		
		int x = 0;
		switch(region) {
		case TOP_LEFT:
		case BOTTOM_LEFT:
			x = 0;
			break;
		case TOP_RIGHT:
		case BOTTOM_RIGHT:
			x = (int) (width - (padding + newSize.width));
			break;
		case TOP_CENTER:
		case BOTTOM_CENTER:
			x = (int) (width - newSize.width) / 2;
			break;
		}
		
		Rect rect = null;		
		switch(region) {
		case TOP_LEFT:
		case TOP_CENTER:
		case TOP_RIGHT:
			rect = new Rect(x, offset, (int) (newSize.width + 2 * padding), (int) (newSize.height + 2 * padding));
			break;
		case BOTTOM_LEFT:
		case BOTTOM_CENTER:
		case BOTTOM_RIGHT:
			rect = new Rect(x, (int) (this.height - offset - 2 * padding - newSize.height), (int) (newSize.width + 2 * padding), (int) (newSize.height + 2 * padding));
			break;
		}
		
		if (bgColor != null) {
			Util.drawRect(image, rect, bgColor, -1);
		}
		Imgproc.putText(image, text, new Point(rect.x + padding, rect.y + newSize.height + padding), font, fontScale, color, thickness);
		
		offsets[region.ordinal()] = (int) (offset + 2 * padding + newSize.height);
	}
	
	public void setBackgroundColor(Scalar color) {
		bgColor = color;
	}
	
	public void clearBackgroundColor() {
		bgColor = null;
	}

}
