import ab.vision.ABObject;
import ab.vision.ABType;

import java.awt.image.BufferedImage;
import java.util.*;

import java.awt.Rectangle;
import java.awt.Point;
import ab.vision.Vision;
import java.awt.geom.Line2D;

public class HeuristicEngine {

    List<ABObject> allObjects;

    List<ABObject> wood_blocks;
    List<ABObject> stone_blocks;
    List<ABObject> ice_blocks;
    List<ABObject> TNT;
    List<ABObject> birds;
    List<ABObject> hills;
    List<ABObject> pigs;
    BufferedImage image;
    Vision vision;

    public HeuristicEngine(List<ABObject> all_objects, BufferedImage image) {
		this.allObjects = all_objects;
		this.image = image;
		this.vision = new Vision(image);
		this.birds = vision.findBirdsMBR();
		this.wood_blocks = vision.getMBRVision().constructABObjects(vision.getMBRVision().findWoodMBR(), ABType.Wood);
		this.stone_blocks = vision.getMBRVision().constructABObjects(vision.getMBRVision().findStonesMBR(),ABType.Stone);
		this.ice_blocks = vision.getMBRVision().constructABObjects(vision.getMBRVision().findIceMBR(),ABType.Ice);
		this.TNT = vision.getMBRVision().constructABObjects(vision.getMBRVision().findTNTsMBR(), ABType.TNT);
		this.hills = findAllHills(all_objects);
		this.pigs = vision.findPigsMBR();
    }

    List<ABObject> findAllHills(List<ABObject> all_objects)
    {
		List<ABObject> hills = new LinkedList<ABObject>();
		for(ABObject obj : all_objects )
		{
		    if(obj.type == ABType.Hill)
		    {
		        hills.add(obj);
		    }
		}
		return hills;
    }

    int getBlockDensity(ABObject block)
    {
        return block.type==ABType.Wood?woodDensity:block.type==ABType.Stone?stoneDensity:iceDensity;
    }

    public void calcSupportFactor()
    {
        ArrayList<ABObject> allBlocks = new ArrayList<ABObject>();
        if(wood_blocks!=null)
			allBlocks.addAll(wood_blocks)
        if(stones_blocks!=null)
			allBlocks.addAll(stones_blocks)
        if(ice_blocks!=null)
			allBlocks.addAll(ice_blocks)
        if(TNT!=null)
			allBlocks.addAll(TNT)
        for(ABObject block: allBlocks)
        {
            for(ABObject pig:pigs)
            {
                Line2D.Double line = new Line2D.Double(object.getCenterX(),object.getCenterY(),pig.getCenterX(),pig.getCenterY());
                double support=0;
                int inline_block_count = 0;
                for(ABObject obj:allBlocks)
                {
                    if(line.intersects(obj))
                    {
                        sup = obj.getHeight()/obj.getWidth();
                        support+= sup/(obj.area*getBlockDensity(obj));
                        inline_block_count++;
                    }
                }
                object.supportFactor+=support/inline_block_count;
            }
        }
        return;
    }
}
