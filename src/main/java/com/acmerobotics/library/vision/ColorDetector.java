package com.acmerobotics.library.vision;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class ColorDetector {
	
	public ScalarRange range;
	
	private List<ColorRegion> regions;
	private Mat mask;
	
	public ColorDetector(ScalarRange range) {
		this.range = range;
		this.regions = null;
		this.mask = new Mat();
	}
	
	public ScalarRange getColorRange() {
		return range;
	}
	
	public void setColorRange(ScalarRange range) {
		this.range = range;
	}
	
	public void analyzeImage(Mat image) {
		Mat hsv = new Mat(), gray = new Mat();
		
		Imgproc.cvtColor(image, hsv, Imgproc.COLOR_BGR2HSV);
		Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
		
		this.mask = range.inRange(hsv);
		
		Imgproc.medianBlur(mask, mask, 5);
		
//		Main.writeImage(mask);
		
		Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(7, 7));
		Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_OPEN, kernel);
//		Imgproc.erode(mask, mask, kernel);
//		Main.writeImage(mask);
		Mat kernel2 = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(25, 25));
		Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_CLOSE, kernel2);
//		Main.writeImage(mask);
//		Imgproc.dilate(mask, mask, kernel);
		
//		Core.bitwise_and(gray, mask, gray);
		
		Mat temp = new Mat();
		mask.copyTo(temp);
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Imgproc.findContours(temp, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
		
		regions = new ArrayList<ColorRegion>();
		for (MatOfPoint contour : contours) {
			regions.add(new ColorRegion(contour));
		}
		
	}
	
	public List<ColorRegion> getRegions() {
		return regions;
	}
	
	public void clipRegion(Mat src, Mat dest) {
		Mat tempMask = Util.expandChannels(this.mask, dest.channels());
		Core.bitwise_and(src, tempMask, dest);
	}
	
	public Mat getMask() {
		return this.mask;
	}

}
