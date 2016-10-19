package com.acmerobotics.library.vision;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class ColorRegion {
	
	private MatOfPoint contour;
	
	public ColorRegion(MatOfPoint contour) {
		this.contour = contour;
	}
	
	public MatOfPoint getContour() {
		return contour;
	}
	
	public void draw(Mat image, Scalar color, int thickness) {
		List<MatOfPoint> regionsToDraw = new ArrayList<MatOfPoint>();
		regionsToDraw.add(contour);
		Imgproc.drawContours(image, regionsToDraw, -1, color, thickness);
	}

	public static void drawRegions(Mat image, List<ColorRegion> regions, Scalar color, int thickness) {
		for (ColorRegion region : regions) {
			region.draw(image, color, thickness);
		}
	}

}
