package com.acmerobotics.library.vision;

import com.acmerobotics.library.vision.Beacon.BeaconColor;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BeaconAnalyzer {

	public static class AnalysisIntermediates extends HashMap<String, Mat> {
		@Override
		public Mat put(String s, Mat mat) {
			if (!super.containsKey(s)) {
				super.put(s, new Mat());
			}
			mat.copyTo(super.get(s));
			return null;
		}
	}

	private static ColorDetector redDetector, blueDetector;
	private static List<BeaconRegion> redRegions, blueRegions, allRegions;
	static AnalysisIntermediates intermediates;
    static BeaconColor currentColor;
    public static boolean DEBUG = false;

	public enum ButtonDetectionMethod {
		BUTTON_HOUGH,
		BUTTON_ELLIPSE
	}

	public static void analyzeImage(Mat image, List<Beacon> beacons) {
		analyzeImage(image, ButtonDetectionMethod.BUTTON_ELLIPSE, beacons);
	}

	public static void analyzeImage(Mat image, ButtonDetectionMethod buttonMethod, List<Beacon> beacons) {

		if (intermediates == null) {
			intermediates = new AnalysisIntermediates();
		}

		if (redDetector == null || blueDetector == null) {
			ScalarRange red = new ScalarRange();
			red.add(new Scalar(145, 0, 160), new Scalar(180, 255, 255));
			red.add(new Scalar(0, 0, 160), new Scalar(10, 255, 255));

			ScalarRange blue = new ScalarRange();
			blue.add(new Scalar(90, 40, 180), new Scalar(125, 255, 255));

			redDetector = new ColorDetector(red);
			blueDetector = new ColorDetector(blue);
		}

		if (redRegions == null || blueRegions == null) {
			redRegions = new ArrayList<BeaconRegion>();
			blueRegions = new ArrayList<BeaconRegion>();
		}

		Imgproc.resize(image, image, getSmallSize(image.size(), 640));

		findBeaconRegions(image, redDetector, BeaconColor.RED, buttonMethod, redRegions);
		findBeaconRegions(image, blueDetector, BeaconColor.BLUE, buttonMethod, blueRegions);

		if (allRegions == null) {
			allRegions = new ArrayList<BeaconRegion>();
		} else {
			allRegions.clear();
		}

		allRegions.addAll(redRegions);
		allRegions.addAll(blueRegions);

		int numRegions = allRegions.size();
		for (int i = 0; i < numRegions; i++) {
			BeaconRegion region1 = allRegions.get(i);
			for (int j = 0; j <= i; j++) {
				BeaconRegion region2 = allRegions.get(j);
				Beacon newBeacon;
				if (region1.equals(region2)) {
					newBeacon = new Beacon(region1);
				} else {
					newBeacon = new Beacon(region1, region2);
				}
				if (newBeacon.getScore().getNumericScore() >= 5) beacons.add(newBeacon);
			}
		}
	}

	public static Size getSmallSize(Size big, int maxDimension) {
		if (big.width > big.height) {
			return new Size(maxDimension, big.height * maxDimension / big.width);
		} else {
			return new Size(big.width * maxDimension / big.height, maxDimension);
		}
	}

	public static void findBeaconRegions(Mat image, ColorDetector detector, BeaconColor color, ButtonDetectionMethod method, List<BeaconRegion> beaconRegions) {
        currentColor = color;

		detector.analyzeImage(image);
		List<ColorRegion> regions = detector.getRegions();

		if (DEBUG) {
			Mat temp = new Mat();
			image.copyTo(temp);
			ColorRegion.drawRegions(temp, regions, color == BeaconColor.RED ? new Scalar(0, 0, 255) : new Scalar(255, 0, 0), 2);
			intermediates.put("beacon_regions_" + color, temp);
			temp.release();
		}

		Mat gray = new Mat();
		Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
		detector.clipRegion(gray, gray);

		Imgproc.threshold(gray, gray, 0, 255, Imgproc.THRESH_BINARY_INV | Imgproc.THRESH_OTSU);
		Mat bg = detector.getMask();
		Core.bitwise_and(gray, bg, gray);

		if (DEBUG) intermediates.put("thresholded_regions_" + color, gray);

		List<Circle> buttons = findButtons(gray, method);

		beaconRegions.clear();

		for (ColorRegion region : regions) {
			BeaconRegion beaconRegion = new BeaconRegion(region, color);
			for (Circle button : buttons) {
				if (beaconRegion.getBounds().boundingRect().contains(button.pt)) beaconRegion.addButton(button);
			}
			beaconRegions.add(beaconRegion);
		}
	}

	public static List<Circle> findButtons(Mat gray, ButtonDetectionMethod method) {
		if (method == ButtonDetectionMethod.BUTTON_HOUGH) {
			return findButtonsHough(gray);
		} else if (method == ButtonDetectionMethod.BUTTON_ELLIPSE) {
			return findButtonsEllipse(gray);
		} else {
			throw new RuntimeException("unknown button detection method: " + method.toString());
		}
	}

	public static List<Circle> findButtonsHough(Mat gray) {
		Mat temp = new Mat();
		Imgproc.GaussianBlur(gray, temp, new Size(9, 9), 2);

		Mat circles = new Mat();
		Imgproc.HoughCircles(temp, circles, Imgproc.CV_HOUGH_GRADIENT, 1, 15, 200, 20, 0, 30);

		List<Circle> circleList = new ArrayList<Circle>();
		int numCircles = circles.cols();
		for (int i = 0; i < numCircles; i++) {
			Circle button = Circle.fromDoubleArray(circles.get(0, i));
			circleList.add(button);
		}

		return circleList;
	}

	public static List<Circle> findButtonsEllipse(Mat gray) {
		List<Circle> circles = new ArrayList<Circle>();

		Mat edges = new Mat();
		int nonZero = Core.countNonZero(gray);

		if (DEBUG) {
			Mat temp = new Mat();
			gray.copyTo(temp);
			Imgproc.putText(temp, Integer.toString(nonZero), new Point(0, 30), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(255), 2);
			intermediates.put("buttons_" + currentColor, temp);
			temp.release();
		}
//		 don't morphologically open unless there are enough white pixels
		Mat kernel;
		if (nonZero > 1700) {
			kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(7, 7));
		} else if (nonZero > 500) {
			kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));
		}else {
			kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
		}
		Imgproc.morphologyEx(gray, gray, Imgproc.MORPH_OPEN, kernel);
		if (DEBUG) intermediates.put("button_smooth_" + currentColor, gray);
		Imgproc.GaussianBlur(gray, gray, new Size(5, 5), 2);
		Imgproc.Canny(gray, edges, 200, 100);

		if (DEBUG) intermediates.put("button_edges_" + currentColor, edges);

		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Imgproc.findContours(edges, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

		edges.release();

        MatOfPoint2f ellipseContour = new MatOfPoint2f();
		for (MatOfPoint contour : contours) {
			// at least 5 points are needed to fit an ellipse
			if (contour.rows() < 5) {
				continue;
			}

			Rect boundingRect = Imgproc.boundingRect(contour);
			double eccentricity = ((double) boundingRect.width) / boundingRect.height;

			if (Math.abs(eccentricity - 1) <= 0.3) {
                contour.convertTo(ellipseContour, CvType.CV_32FC2);
				RotatedRect ellipse = Imgproc.fitEllipse(ellipseContour);
				// convert the ellipse into a circle
				double fittedRadius = (ellipse.size.width + ellipse.size.height) / 4;
				if (fittedRadius > 2) {
					circles.add(new Circle(ellipse.center, (int) (fittedRadius + 0.5)));
				}
			}

            contour.release();
		}

        ellipseContour.release();

		return circles;
	}

	public static Map<String, Mat> getIntermediates() {
		return intermediates;
	}

}
