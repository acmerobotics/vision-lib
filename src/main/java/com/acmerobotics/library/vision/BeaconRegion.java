package com.acmerobotics.library.vision;

import com.acmerobotics.library.vision.Beacon.BeaconColor;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class BeaconRegion {
	
	private ColorRegion region;
	private BeaconColor color;
	private List<Circle> buttons;
	private RotatedRect bounds;
	
	public BeaconRegion(ColorRegion colorRegion, BeaconColor color) {
		this.region = colorRegion;
		this.color = color;
		this.buttons = new ArrayList<Circle>();
        this.bounds = Util.fitRotatedRect(this.region.getContour());
	}
	
	public double area() {
		return Imgproc.contourArea(region.getContour());
	}
	
	public void addButton(Circle button) {
		this.buttons.add(button);
	}
	
	public void addButtons(List<Circle> buttons) {
		this.buttons.addAll(buttons);
	}
	
	public MatOfPoint getContour() {
		return region.getContour();
	}
	
	public List<Circle> getButtons() {
		return this.buttons;
	}
	
	public RotatedRect getBounds() {
		return this.bounds;
	}
	
	public BeaconColor getColor() {
		return this.color;
	}
	
	public void draw(Mat image) {
		region.draw(image, color == BeaconColor.RED ? new Scalar(0, 0, 255) : new Scalar(255, 0, 0), 2);
		Circle.drawCircles(image, buttons, new Scalar(0, 255, 255), 2);
	}
	
	public static void drawRegions(Mat image, List<BeaconRegion> regions) {
		for (BeaconRegion region : regions) {
			region.draw(image);
		}
	}

	public void release() {
		region.release();
	}

}
