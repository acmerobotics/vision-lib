package com.acmerobotics.library.vision;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class Util {
	
	public static Mat expandChannels(Mat src, int numChannels) {
		Mat dest = new Mat(src.size(), CvType.CV_8UC(numChannels));
		for (int i = 0; i < numChannels; i++) {
			Core.insertChannel(src, dest, i);
		}
		return dest;
	}
	
	public static Rect combineRects(Rect rect1, Rect rect2) {
		int left1 = rect1.x, right1 = left1 + rect1.width;
		int top1 = rect1.y, bottom1 = rect1.y + rect1.height;
		int left2 = rect2.x, right2 = left2 + rect2.width;
		int top2 = rect2.y, bottom2 = rect2.y + rect2.height;
		int minLeft = left1 < left2 ? left1 : left2;
		int maxRight = right1 > right2 ? right1 : right2;
		int minTop = top1 < top2 ? top1 : top2;
		int maxBottom = bottom1 > bottom2 ? bottom1 : bottom2;
		return new Rect(minLeft, minTop, maxRight - minLeft, maxBottom - minTop);
	}
	
	public static void drawRect(Mat image, Rect rect, Scalar color, int thickness) {
		Imgproc.rectangle(image, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), color, thickness);
	}
	
	public static void drawRotatedRect(Mat image, RotatedRect rect, Scalar color, int thickness) {
		Point[] vertices = new Point[4];
		rect.points(vertices);
		for (int i = 0; i < 4; i++) {
			Imgproc.line(image, vertices[i], vertices[(i + 1) % 4], color, thickness);
		}
	}

}
