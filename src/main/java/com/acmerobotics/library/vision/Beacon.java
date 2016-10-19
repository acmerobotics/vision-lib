package com.acmerobotics.library.vision;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class Beacon {
	
	public enum BeaconColor {
		RED,
		BLUE, 
		UNKNOWN
	}
	
	public static final double BEACON_HEIGHT = 5.7;
	public static final double BEACON_WIDTH = 8.5;
	public static final double BEACON_BOTTOM_HEIGHT = 1.1;
	public static final double PARTIAL_BEACON_WH_RATIO = BEACON_WIDTH / (BEACON_HEIGHT - BEACON_BOTTOM_HEIGHT);
	public static final double FULL_BEACON_WH_RATIO = BEACON_WIDTH / BEACON_HEIGHT;
	
	private List<BeaconRegion> beaconRegions;
	private RotatedRect bounds;
	
	private String scoreString;
	
	public Beacon(BeaconRegion center) {
		beaconRegions = new ArrayList<BeaconRegion>();
		beaconRegions.add(center);
		bounds = center.getBounds();
	}
	
	public Beacon(BeaconRegion region1, BeaconRegion region2) {
		beaconRegions = new ArrayList<BeaconRegion>();
		if (region1.getBounds().center.x < region2.getBounds().center.x) {
			beaconRegions.add(region1);
			beaconRegions.add(region2);
			calculateBoundsTwoRegions(region1, region2);
		} else {
			beaconRegions.add(region2);
			beaconRegions.add(region1);
			calculateBoundsTwoRegions(region2, region1);
		}
	}
	
	private void calculateBoundsTwoRegions(BeaconRegion leftRegion, BeaconRegion rightRegion) {
		List<Point> allPoints = new ArrayList<Point>();
		List<Point> leftPoints = leftRegion.getContour().toList();
		List<Point> rightPoints = rightRegion.getContour().toList();
		allPoints.addAll(leftPoints);
		allPoints.addAll(rightPoints);
		MatOfPoint2f points = new MatOfPoint2f();
		points.fromList(allPoints);
		bounds = Imgproc.minAreaRect(points);
	}
	
	public String getScoreString() {
		return this.scoreString;
	}
	
	public int score() {
		int score = 0;
		
		scoreString = "";
		
		double aspectRatioError = getAspectRatioError();
		if (aspectRatioError < 0.05) {
			score += 2;
			scoreString += "A";
		}
		
		if (beaconRegions.size() == 1) {
			int numButtons = beaconRegions.get(0).getButtons().size();
			if (numButtons == 2) {
				score += 4;
				scoreString += "2";
			} else if (numButtons > 0) {
				score += 1;
				scoreString += "?";
			}
		} else {
			int leftButtons = getLeftRegion().getButtons().size();
			if (leftButtons == 1) {
				score += 2;
				scoreString += "L";
			} else if (leftButtons > 1) {
				score += 1;
				scoreString += "L?";
			}
			
			int rightButtons = getRightRegion().getButtons().size();			
			if (rightButtons == 1) {
				score += 2;
				scoreString += "R";
			} else if (rightButtons > 1) {
				score += 1;
				scoreString += "R?";
			}
		}
		
		double totalArea = bounds.size.width * bounds.size.height;
		double leftArea = getLeftRegion().area();
		double rightArea = getRightRegion().area();
		double diffAreaError = Math.pow(leftArea - rightArea, 2) / totalArea;
		
		if (diffAreaError < 350) {
			score += 1;
			scoreString += "D";
		}
		
		
		return score;
	}
	
	public double getAspectRatioError() {
		double partialError = getAspectRatioError(bounds.size, PARTIAL_BEACON_WH_RATIO);
		double fullError = getAspectRatioError(bounds.size, FULL_BEACON_WH_RATIO);
		return Math.min(partialError, fullError);
	}
	
	private double getAspectRatioError(Size size, double ratio) {
		double widthHeightRatio = size.width / size.height;
		double heightWidthRatio = size.height / size.width;
		double whError = Math.pow(widthHeightRatio - ratio, 2);
		double hwError = Math.pow(heightWidthRatio - ratio, 2);
		return Math.min(whError, hwError);
	}
	
	public BeaconRegion getLeftRegion() {
		return beaconRegions.get(0);
	}
	
	public BeaconRegion getRightRegion() {
		return beaconRegions.get(beaconRegions.size() - 1);
	}
	
	public RotatedRect getBounds() {
		return this.bounds;
	}
	
	public List<Circle> getButtons() {
		List<Circle> buttons = new ArrayList<Circle>();
		for (BeaconRegion region : beaconRegions) {
			buttons.addAll(region.getButtons());
		}
		return buttons;
	}
	
	public void draw(Mat image) {
		BeaconRegion.drawRegions(image, beaconRegions);
		
		double area = bounds.size.width * bounds.size.height;
		String areaString = Integer.toString((int) Math.round(area / 1000)) + "K";
		int font = Core.FONT_HERSHEY_SIMPLEX;
		Size textBounds = Imgproc.getTextSize(areaString, font, 1.5, 3, null);
		Imgproc.putText(image, areaString, new Point(bounds.center.x - textBounds.width / 2, bounds.center.y + textBounds.height / 2), font, 1.5, new Scalar(255, 255, 255), 3);
		
		Util.drawRotatedRect(image, bounds, new Scalar(255, 255, 0), 2);
	}
	
}
