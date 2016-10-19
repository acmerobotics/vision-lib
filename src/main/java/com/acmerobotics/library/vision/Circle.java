package com.acmerobotics.library.vision;

import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class Circle {

    public Point pt;
    public int radius;

    public Circle(Point pt, int radius) {
        this.pt = pt;
        this.radius = radius;
    }

    public static Circle fromDoubleArray(double[] arr) {
        Point pt = new Point();
        pt.x = Math.round(arr[0]);
        pt.y = Math.round(arr[1]);
        int radius = (int) Math.round(arr[2]);
        return new Circle(pt, radius);
    }
    
    public void draw(Mat image, Scalar color, int thickness) {
    	Imgproc.circle(image, pt, radius, color, thickness);
    }

	public static void drawCircles(Mat image, List<Circle> circles, Scalar color, int thickness) {
		for (Circle circle : circles) {
			Imgproc.circle(image, circle.pt, circle.radius, color, thickness);
		}
	}

}
