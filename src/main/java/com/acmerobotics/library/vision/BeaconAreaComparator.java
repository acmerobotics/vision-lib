package com.acmerobotics.library.vision;

import org.opencv.core.Size;

import java.util.Comparator;

public class BeaconAreaComparator implements Comparator<Beacon> {
    @Override
    public int compare(Beacon o1, Beacon o2) {
        Size s1 = o1.getBounds().size;
        Size s2 = o2.getBounds().size;
        double area1 = s1.width * s1.height;
        double area2 = s2.width * s2.height;
        return (area1 > area2) ? -1 : 1;
    }
}
