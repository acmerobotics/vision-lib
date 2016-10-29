package com.acmerobotics.library.vision;

import com.acmerobotics.library.vision.Beacon.BeaconColor;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class BeaconAnalyzer {

	private static ColorDetector redDetector, blueDetector;
	private static List<BeaconRegion> redRegions, blueRegions, allRegions;
	
	public enum ButtonDetectionMethod {
		BUTTON_HOUGH,
		BUTTON_ELLIPSE
	}
	
	public static List<Beacon> analyzeImage(Mat image) {
		return analyzeImage(image, ButtonDetectionMethod.BUTTON_ELLIPSE);
	}
	
	public static List<Beacon> analyzeImage(Mat image, ButtonDetectionMethod buttonMethod) {
		if (redDetector == null || blueDetector == null) {
			ScalarRange red = new ScalarRange();
			red.add(new Scalar(155, 0, 160), new Scalar(180, 255, 255));
			red.add(new Scalar(0, 0, 160), new Scalar(2, 255, 255));

			ScalarRange blue = new ScalarRange();
			blue.add(new Scalar(90, 40, 180), new Scalar(125, 255, 255));

			redDetector = new ColorDetector(red);
			blueDetector = new ColorDetector(blue);
		}
		
		if (redRegions == null || blueRegions == null) {
			redRegions = new ArrayList<BeaconRegion>();
			blueRegions = new ArrayList<BeaconRegion>();
		}
		
		findBeaconRegions(image, redDetector, BeaconColor.RED, buttonMethod, redRegions);
		findBeaconRegions(image, blueDetector, BeaconColor.BLUE, buttonMethod, blueRegions);

		if (allRegions == null) {
			allRegions = new ArrayList<BeaconRegion>();
		} else {
			allRegions.clear();
		}
		
		allRegions.addAll(redRegions);
		allRegions.addAll(blueRegions);
		
		List<Beacon> beacons = new ArrayList<Beacon>();
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
				if (newBeacon.getScore().getNumericScore() >= 6) beacons.add(newBeacon);
			}
		}
		
		return beacons;
	}
	
	public static void findBeaconRegions(Mat image, ColorDetector detector, BeaconColor color, ButtonDetectionMethod method, List<BeaconRegion> beaconRegions) {
		detector.analyzeImage(image);
		List<ColorRegion> regions = detector.getRegions();
		
		Mat gray = new Mat();
		Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
		detector.clipRegion(gray, gray);
		
		Imgproc.threshold(gray, gray, 0, 255, Imgproc.THRESH_BINARY_INV | Imgproc.THRESH_OTSU);
		Mat bg = detector.getMask();
		Core.bitwise_and(gray, bg, gray);
		
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
		// don't morphologically open unless there are enough white pixels
		if (nonZero > 250) {
			Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(7, 7));
			Imgproc.morphologyEx(gray, gray, Imgproc.MORPH_OPEN, kernel);
		}
		Imgproc.GaussianBlur(gray, gray, new Size(5, 5), 2);
		
		Imgproc.Canny(gray, edges, 200, 100);

		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Imgproc.findContours(edges, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
		
		for (MatOfPoint contour : contours) {
			// at least 5 points are needed to fit an ellipse
			if (contour.rows() < 5) {
				continue;
			}
			
			Rect boundingRect = Imgproc.boundingRect(contour);
			double eccentricity = ((double) boundingRect.width) / boundingRect.height;
			
			if (Math.abs(eccentricity - 1) <= 0.3) {
				MatOfPoint2f ellipseContour = new MatOfPoint2f();
				ellipseContour.fromArray(contour.toArray());
				RotatedRect ellipse = Imgproc.fitEllipse(ellipseContour);
				// convert the ellipse into a circle
				double fittedRadius = (ellipse.size.width + ellipse.size.height) / 4;
				if (fittedRadius > 2) {
					circles.add(new Circle(ellipse.center, (int) (fittedRadius + 0.5)));
				}
			}
		}
		
		return circles;
	}

}
