import ab.vision.ABObject;
import ab.vision.ABType;

import java.awt.image.BufferedImage;
import java.util.*;

import java.awt.Rectangle;
import java.awt.Point;
import ab.vision.Vision;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

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
    List<ABObject> air_blocks;
    private ABType current_bird;
    private Rectangle sling_shot;

    public HeuristicEngine(List<ABObject> all_objects, BufferedImage image, ABType current_bird, Rectangle sling_shot) {
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
        this.air_blocks = new LinkedList<ABObject>();
        this.current_bird = current_bird;
        this.sling_shot = sling_shot;
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
    	//density increases in scale of 2
        return block.type==ABType.Wood? 2:block.type==ABType.Stone?4:1;
    }

    public void calcSupportFactor()
    {
        ArrayList<ABObject> allBlocks = getAllBlocks();
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

    public ArrayList<ABObject> getAllBlocks()
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
        return allBlocks;
    }

    public void calcDownwardFactor()
    {
        ArrayList<ABObject> allBlocks = getAllBlocks();

        int ground_level = vision.getVisionRealShape().getGroundLevel();

        for(ABObject obj: allBlocks)
        {
            int lateral_dist_sum = 0;
            int density_sum = 0;
            Line2D verticalLine = new Line2D.Double(obj.getCenter(), new Point2D.Double(obj.getCenterX(),ground_level));

            for(ABObject block: allBlocks)
            {
                if(verticalLine.intersects(block))
                    density_sum+=getBlockDensity(block);
            }

            for(ABObject pig: pigs)
            {
                if(pig.getMinY()>obj.getMaxY())
                {
                    lateral_dist_sum += Math.abs(verticalLine.getX1() - pig.getCenterX());
                    Line2D lineToPig = new Line2D.Double(pig.getCenter(),new Point2D.Double(verticalLine.getX1(),pig.getCenterY()));
                    for(ABObject intermediate_block: allBlocks)
                    {
                        if(lineToPig.intersects(intermediate_block))
                          density_sum+=getBlockDensity(intermediate_block);
                    }

                }
            }
            obj.downwardFactor = lateral_dist_sum;
        }
    }

    public void makeAirBlocks(double[][][] structure, Rectangle outerRectangle)
    {
        boolean[][] air = isAir(structure);
        for(int i = 0;i<air.length;i++)
        {
            for(int j = 0;j<air.length;j++)
            {
                if(air[i][j])
                {
                    Rectangle bounding_rec = new Rectangle((int)(outerRectangle.x+i*(outerRectangle.getWidth()/structure.length)),(int)(outerRectangle.y+j*(outerRectangle.getHeight()/structure.length)),(int)(outerRectangle.getWidth()/structure.length),(int)(outerRectangle.getHeight()/structure.length));
                    air_blocks.add(new ABObject(bounding_rec,ABType.Air));
                }
            }
        }
    }

    public boolean[][] isAir(double[][][] structure)
    {
        boolean[][] air = new boolean[structure.length][structure.length];
        for(int i = 0;i<structure.length;i++)
        {
            for(int j=0;j<structure[i].length;j++)
            {
                double pig_percentage = structure[i][j][0];
                double wood_percentage = structure[i][j][1];
                double ice_percentage = structure[i][j][2];
                double stone_percentage = structure[i][j][3];
                double tnt_percentage = structure[i][j][4];
                if(pig_percentage + wood_percentage + ice_percentage + stone_percentage + tnt_percentage<10)
                    air[i][j] = true;
                else
                    air[i][j]  = false;
            }
        }
        return air;
    }

    public void calcDisplacementFactor()
    {
        ArrayList<ABObject> allBlocks = getAllBlocks();
        if(pigs != null)
            allRect.addAll(pigs);

        for(ABObject obj: allBlocks)
        {
            Line2D.Double line = new Line2D.Double(obj.getX(),obj.getY(),getLastX(),obj.getY());
            int weight = 0;
            int max_value = Integer.MAX_VALUE;
            if(current_bird == ABType.RedBird)
            {
                for(ABObject block: allBlocks)
                {
                    int area = Math.min(block.width,block.height);
                    if(line.intersects(block))
                    {
                        if(block.type==ABType.Wood)
                        {
                            weight += 1/(10 * area);
                        }
                        else if(block.type==ABType.Ice)
                            weight += 1/(20 * area);
                        else if(block.type==ABType.Stone)
                            weight += 1/(30 * area);
                        else if(block.type==ABType.Air)
                            weight += max_value/block.getX();
                        else
                        {
                            continue;
                        }
                    }
                }
            }
            else if (current_bird == ABType.BlueBird)
            {
                for(ABObject block: allBlocks)
                {
                    int area = Math.min(block.width,block.height);
                    if(line.intersects(block))
                    {
                        if(block.type==ABType.Wood)
                            weight += 1/(20 * area);
                        else if(block.type==ABType.Ice)
                            weight += 1/(10 * area);
                        else if(block.type==ABType.Stone)
                            weight += 1/(30 * area);
                        else if(block.type==ABType.Air)
                            weight += max_value/block.getX();
                        else
                            continue;
                    }
                }
            }
            else if (current_bird == ABType.YellowBird)
            {
                for(ABObject block: allBlocks)
                {
                    int area = Math.min(block.width,block.height);
                    if(line.intersects(block))
                    {
                        if(block.type==ABType.Wood)
                            weight += 1/(10 * area);
                        else if(block.type==ABType.Ice)
                            weight += 1/(20 * area);
                        else if(block.type==ABType.Stone)
                            weight += 1/(30 * area);
                        else if(block.type==ABType.Air)
                            weight += max_value/block.getX();
                        else
                            continue;
                    }
                }
            }
            else if (current_bird==ABType.WhiteBird)
            {
                for(ABObject block: allBlocks)
                {
                    int area = Math.min(block.width,block.height);
                    if(line.intersects(block))
                    {
                        if(block.type==ABType.Wood)
                            weight += 1/(30 * area);
                        else if(block.type==ABType.Ice)
                            weight += 1/(20 * area);
                        else if(block.type==ABType.Stone)
                            weight += 1/(10 * area);
                        else if(block.type==ABType.Air)
                            weight += max_value/block.getX();
                        else
                            continue;
                    }
                }
            }
            else //for black birds
            {
                for(ABObject block: allBlocks)
                {
                    int area = Math.min(block.width,block.height);
                    if(line.intersects(block))
                    {
                        if(block.type==ABType.Wood)
                            weight += 1/(10 * area);
                        else if(block.type==ABType.Ice)
                            weight += 1/(20 * area);
                        else if(block.type==ABType.Stone)
                            weight += 1/(30 * area);
                        else if(block.type==ABType.Air)
                            weight += max_value/block.getX();
                        else
                            continue;
                    }
                }
            }
            obj.displacementFactor = weight;
        }
    }

    public int assignDensity(ABObject block, int wood, int ice, int stone)
    {
        int weight = 0;
        int area = Math.min(block.width,block.height);
        if(block.type==ABType.Wood)
            weight += 1/(20 * area);
        else if(block.type==ABType.Ice)
            weight += 1/(10 * area);
        else if(block.type==ABType.Stone)
            weight += 1/(30 * area);
        else if(block.type==ABType.Air)
            weight += Integer.MAX_VALUE/block.getX();

        return weight;
    }

    public void calcPenetrationFactor()
    {
        ArrayList<ABObject> allBlocks = getAllBlocks();
        if(pigs != null)
            allRect.addAll(pigs);

        for(ABObject obj: allBlocks)
        {
            int factor = 0;
            TrajectoryPlanner trajectory = new TrajectoryPlanner();
            ArrayList<Point> release_points = trajectory.estimateLaunchPoint(sling_shot,new Point((int) obj.getX(), (int) obj.getY()));

            for(Point p:release_points)
            {
                List<ABObject> trajectoryBlocks = getAllBlocks();

                trajectory.setTrajectory(sling_shot, p);
                ArrayList<Point> trajectory_points = trajectory._trajectory;
                for(Point t:trajectory_points)
                {
                    int flag=0;
                    for(int i=0;i<_hill.size();i++)
                    {
                        Poly h = (Poly) _hill.get(i);
                        if(h.polygon.contains(t) && t.getX()<=obj.getX())
                        {
                            flag=1;
                            factor=Integer.MIN_VALUE;
                            break;
                        }
                    }

                    if(flag==1)
                        break;

                    for(ABObject block:trajectoryBlocks)
                    {
                        if(block != obj && block.getX() <= obj.getX() && block.contains(t))
                        {
                            if(currentBird == ABType.RedBird || currentBird == ABType.BlueBird)
                                factor+=assignDensity(block, 20, 10, 30);
                            else if (currentBird == ABType.YellowBird || currentBird==ABType.BlackBird)
                                factor+=assignDensity(block, 10, 20, 30);
                            else //for White Bird
                                factor+=assignDensity(block, 30, 20, 10);
                        }
                    }
                }
                obj.penetrationFactor = factor;
            }
        }
    }
}
