package com.acmerobotics.library.vision;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class Beacon {
	
	public enum BeaconColor {
		RED("R"),
		BLUE("B"),
		UNKNOWN("?");
		private String colorString;
		BeaconColor(String colorString) {
			this.colorString = colorString;
		}
		@Override
		public String toString() {
			return this.colorString;
		}
	}

	public class Score {
		public final double ratioError;
		public final double areaError;
		public final int leftButtons;
		public final int rightButtons;
		
		private String scoreString;
		private int score;

		public Score(double ratioError, double areaError, int leftButtons, int rightButtons) {
			this.ratioError = ratioError;
			this.areaError = areaError;
			this.leftButtons = leftButtons;
			this.rightButtons = rightButtons;
			
			calculateScore();
		}
		
		private void calculateScore() {
			score = 0;
			scoreString = "";
			
			if (ratioError < 0.05) {
				score += 2;
				scoreString += "A";
			}
			
			if (areaError < 0.05) {
				score += 1;
				scoreString += "D";
			}
			
			if (rightButtons == -1) {
				if (leftButtons == 2) {
					score += 4;
					scoreString += "2";
				} else if (leftButtons > 0) {
					score += 1;
					scoreString += "?";
				}
			} else {
				if (leftButtons == 1) {
					score += 2;
					scoreString += "L";
				} else if (leftButtons > 1) {
					score += 1;
					scoreString += "L?";
				}
					
				if (rightButtons == 1) {
					score += 2;
					scoreString += "R";
				} else if (rightButtons > 1) {
					score += 1;
					scoreString += "R?";
				}
			}
		}

		public int getNumericScore() {
			return score;
		}

		@Override
		public String toString() {
			return scoreString;
		}
	}
	
	public static final double BEACON_HEIGHT = 5.7;
	public static final double BEACON_WIDTH = 8.5;
	public static final double BEACON_BOTTOM_HEIGHT = 1.1;
	public static final double PARTIAL_BEACON_WH_RATIO = BEACON_WIDTH / (BEACON_HEIGHT - BEACON_BOTTOM_HEIGHT);
	public static final double FULL_BEACON_WH_RATIO = BEACON_WIDTH / BEACON_HEIGHT;
	
	private List<BeaconRegion> beaconRegions;
	private RotatedRect bounds;
	private Score score;
	
	public Beacon(BeaconRegion center) {
		beaconRegions = new ArrayList<BeaconRegion>();
		beaconRegions.add(center);
		bounds = center.getBounds();
		
		calculateScore();
	}
	
	public Beacon(BeaconRegion region1, BeaconRegion region2) {
		beaconRegions = new ArrayList<BeaconRegion>();
		if (region1.getBounds().center.x < region2.getBounds().center.x) {
			beaconRegions.add(region1);
			beaconRegions.add(region2);
		} else {
			beaconRegions.add(region2);
			beaconRegions.add(region1);
		}
        this.bounds = Util.combineRotatedRects(region1.getBounds(), region2.getBounds());
		
		calculateScore();
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
	
	public void calculateScore() {
        double partialError = getAspectRatioError(bounds.size, PARTIAL_BEACON_WH_RATIO);
        double fullError = getAspectRatioError(bounds.size, FULL_BEACON_WH_RATIO);
        double aspectRatioError = Math.min(partialError, fullError);

		double totalArea = bounds.size.width * bounds.size.height;
		double leftArea = getLeftRegion().area();
		double rightArea = getRightRegion().area();

		double diffAreaError = Math.pow((leftArea - rightArea) / totalArea, 2);
		
		int leftButtons, rightButtons;
		if (beaconRegions.size() == 1) {
			leftButtons = beaconRegions.get(0).getButtons().size();
			rightButtons = -1;
		} else {
			leftButtons = getLeftRegion().getButtons().size();
			rightButtons = getRightRegion().getButtons().size();			
		}
		
		this.score = new Score(aspectRatioError, diffAreaError, leftButtons, rightButtons);
	}
	
	public Score getScore() {
		return score;
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

    public void release() {
        if (beaconRegions != null) {
            for (BeaconRegion region : beaconRegions) {
                region.release();
            }
            beaconRegions = null;
        }
    }

	@Override
	public String toString() {
		return getLeftRegion().getColor() + "/" + getRightRegion().getColor();
	}

    @Override
    public void finalize() {
        release();
    }
	
}
