import ab.vision.ABObject;
import ab.vision.ABType;

import java.awt.image.BufferedImage;
import java.util.*;

import java.awt.Rectangle;
import java.awt.Point;
import ab.vision.Vision;

public class HeuristicEngine {

    List<ABObject> allObjects;
    BufferedImage image;
    Vision vision;

    public HeuristicEngine(List<ABObject> all_objects, BufferedImage image) {
    	this.allObjects = all_objects;
        this.image = image;
	    vision = new Vision(image);
    }
}

