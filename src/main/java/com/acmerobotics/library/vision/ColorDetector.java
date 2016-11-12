package com.acmerobotics.library.vision;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class ColorDetector {

	public ScalarRange range;

	private List<ColorRegion> regions;
	private Mat mask, hsv, gray, temp;

	public ColorDetector(ScalarRange range) {
		this.range = range;
		this.regions = null;
		this.mask = new Mat();
		this.hsv = new Mat();
		this.gray = new Mat();
		this.temp = new Mat();
	}

	public ScalarRange getColorRange() {
		return range;
	}

	public void setColorRange(ScalarRange range) {
		this.range = range;
	}

	public void analyzeImage(Mat image) {
		Imgproc.cvtColor(image, hsv, Imgproc.COLOR_BGR2HSV);
		Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);

		this.mask = range.inRange(hsv);

		if (BeaconAnalyzer.DEBUG) {
			this.mask.copyTo(temp);
		}

		Size originalSize = new Size(mask.cols(), mask.rows());
		Imgproc.resize(mask, mask, new Size(originalSize.width / 4, originalSize.height / 4));

		if (BeaconAnalyzer.DEBUG) {
	        Imgproc.putText(temp, Integer.toString(Core.countNonZero(temp)), new Point(5, 30), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(255), 2);
	        BeaconAnalyzer.intermediates.put("detector_mask_raw_" + BeaconAnalyzer.currentColor, temp);
		}

		Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
		Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_OPEN, kernel);

		Mat kernel2 = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(11, 11));
        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_CLOSE, kernel2);

		Imgproc.resize(mask, mask, originalSize);

		if (BeaconAnalyzer.DEBUG) BeaconAnalyzer.intermediates.put("detector_mask_" + BeaconAnalyzer.currentColor, this.mask);

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

	public void release() {
		if (mask != null) {
			mask.release();
		}
		if (hsv != null) {
			hsv.release();
		}
		if (gray != null) {
			gray.release();
		}
		if (temp != null) {
			temp.release();
		}
		if (regions != null) {
			for (ColorRegion region : regions) {
				region.release();
			}
			regions = null;
		}
	}

}
