package ab.demo;
import ab.vision.ABObject;
import ab.vision.ABType;
import ab.vision.real.shape.Poly;

import java.awt.image.BufferedImage;
import java.lang.Double;
import java.lang.System;
import java.util.*;

import java.awt.Rectangle;

import java.awt.Point;
import ab.vision.Vision;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import ab.planner.TrajectoryPlanner;

public class HeuristicEngine {

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
    private List<SubStructure> sub_structures;

    public HeuristicEngine(List<ABObject> pigs, List<ABObject> wood, List<ABObject> ice, List<ABObject> stones,List<ABObject> TNT, BufferedImage screenShot, ABType current_bird, Rectangle sling_shot) {
		this.image = screenShot;
		this.vision = new Vision(image);
		this.birds = vision.findBirdsMBR();
		this.wood_blocks = wood;
		this.stone_blocks = stones;
		this.ice_blocks = ice;
		this.TNT = TNT;
		this.hills = findAllHills(vision.findBlocksRealShape());
		this.pigs = vision.findPigsMBR();
        this.air_blocks = new LinkedList<ABObject>();
        this.current_bird = current_bird;
        this.sling_shot = sling_shot;
        sub_structures = new LinkedList<SubStructure>();
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
        return block.type==ABType.Wood? 2:block.type==ABType.Stone?40:1;
    }

    public void calcSupportFactor()
    {
        ArrayList<ABObject> allBlocks = getAllBlocks();
        for(ABObject block: allBlocks)
        {
            SubStructure current = null;
            for(int i=0; i<sub_structures.size();i++)
            {
                if(sub_structures.get(i).contains(block))
                    current = sub_structures.get(i);
            }
            if(current!=null)
            {
                for(ABObject pig:pigs)
                {
                    Line2D.Double line = new Line2D.Double(block.getCenterX(),block.getCenterY(),pig.getCenterX(),pig.getCenterY());
                    double support=0,sup;
                    double inline_block_count = 1;
                    for(ABObject obj:allBlocks)
                    {
                        if(line.intersects(obj) || current.contains(obj) && obj.id!=block.id)
                        {
                            sup = obj.getHeight()/obj.getWidth();
                            support+= sup/(Math.min(obj.getHeight(), obj.getWidth())*(double)getBlockDensity(obj));
                            inline_block_count+=1;
                        }
                    }
                    block.supportFactor+=support/inline_block_count;
                }
                block.supportFactor*=100000;
                if(block.type==ABType.Wood)
                    block.supportFactor /= 15;
                else if(block.type==ABType.Ice)
                    block.supportFactor /= 10;
                else if(block.type==ABType.Stone)
                    block.supportFactor /= 20;
            }
        }
        double max = 0;
        for(ABObject obj:allBlocks)
        {
            if(obj.supportFactor>max)
            {
                max = obj.supportFactor;
            }
        }
        for(ABObject obj:allBlocks)
        {
            obj.supportFactor = 100*obj.supportFactor/max;
        }
        return;
    }

    public ArrayList<ABObject> getAllBlocks()
    {
        ArrayList<ABObject> allBlocks = new ArrayList<ABObject>();
    	if(wood_blocks!=null)
			allBlocks.addAll(wood_blocks);
        if(stone_blocks!=null)
			allBlocks.addAll(stone_blocks);
        if(ice_blocks!=null)
			allBlocks.addAll(ice_blocks);
        if(TNT!=null)
			allBlocks.addAll(TNT);
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
            int count = 0;
            Line2D verticalLine = new Line2D.Double(obj.getCenter(), new Point2D.Double (obj.getCenterX(),ground_level));

            for(ABObject block: allBlocks)
            {   
                count = 0;
                if(verticalLine.intersects(block) || verticalLine.contains(block))
                {
                    density_sum+=getBlockDensity(block);
                    count++;
                }
            }

            for(ABObject pig: pigs)
            {
                if(pig.getMinY()>obj.getMaxY())
                {
                    // lateral_dist_sum += Math.abs(verticalLine.getX1() - pig.getCenterX());
                    Line2D lineToPig = new Line2D.Double(pig.getCenter(),new Point2D.Double(verticalLine.getX1(),pig.getCenterY()));
                    for(ABObject intermediate_block: allBlocks)
                    {
                        if(lineToPig.intersects(intermediate_block) || lineToPig.contains(intermediate_block))
                          density_sum+=getBlockDensity(intermediate_block);
                          count++;
                    }

                }
            }

            if (density_sum!=0 && count!= 0) {
                density_sum /= count;
            }
            obj.downwardFactor = density_sum;
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

    public double getLastX()
    {
        List<ABObject> blocks = new ArrayList<ABObject>(0);
        if(pigs != null)
            blocks.addAll(pigs);
        if(wood_blocks != null)
            blocks.addAll(wood_blocks);
        if(ice_blocks != null)
            blocks.addAll(ice_blocks);
        if(stone_blocks != null)
            blocks.addAll(stone_blocks);

        double max=0.0;
        for(ABObject r:blocks)
        {
            double last=r.getX()+r.getWidth();
            if(last>max)
                max=last;
        }

        return max;
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
            allBlocks.addAll(pigs);
        if(air_blocks != null)
            allBlocks.addAll(air_blocks);

        for(ABObject obj: allBlocks)
        {
            if(obj.type!=ABType.Air) {
                Line2D.Double line = new Line2D.Double(obj.getX(),obj.getY(),getLastX(),obj.getY());
                double weight = 0;
                int max_value = Integer.MAX_VALUE;
                if(current_bird == ABType.RedBird)
                {
                    for(ABObject block: allBlocks)
                    {
                        double area = Math.min(block.width,block.height);
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

        double max = 0;
        for(ABObject obj:allBlocks)
        {
            if(notAirBlock(obj) && obj.displacementFactor>max)
                max = obj.displacementFactor;

        }

        for(ABObject object:allBlocks)
        {
            if(notAirBlock(object))
                object.displacementFactor = 100*object.displacementFactor/max;
        }

    }

    public boolean notAirBlock(ABObject object)
    {
        return object.type!=ABType.Air;
    }

    public double assignDensity(ABObject block)
    {
        double weight = 0;
        double area = Math.min(block.width,block.height);
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
            allBlocks.addAll(pigs);

        for(ABObject obj: allBlocks)
        {
            double factor = 0;
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
                    for(int i=0;i<hills.size();i++)
                    {
                        Poly h = (Poly) hills.get(i);
                        if(h.polygon.contains(t) && t.getX()<=obj.getX())
                        {
                            flag=1;
                            factor=Double.MIN_VALUE;
                            break;
                        }
                    }

                    if(flag==1)
                        break;

                    for(ABObject block:trajectoryBlocks)
                    {
                        if(block != obj && block.getX() <= obj.getX() && block.contains(t))
                        {
                            if(current_bird == ABType.RedBird || current_bird == ABType.BlueBird)
                                factor+=assignDensity(block);
                            else if (current_bird == ABType.YellowBird || current_bird==ABType.BlackBird)
                                factor+=assignDensity(block);
                            else //for White Bird
                                factor+=assignDensity(block);
                        }
                    }
                }
                obj.penetrationFactor = factor;
            }
        }
    }

    public ABObject[][] computeFinalBlocks() {

        ABObject[][] final_list = new ABObject[5][2];

        ArrayList<ABObject> allBlocks = getAllBlocks();
        if (pigs != null)
            allBlocks.addAll(pigs);

        for (ABObject block : allBlocks)
        {
            block.bottomUpFactor = (0.25 * block.penetrationFactor) + (0.25 * block.displacementFactor) + (0.1 * block.supportFactor) + (0.4 * block.weakVicinityFactor);
            System.out.println("Penetratoin : " + block.penetrationFactor);
            System.out.println("Displacement: "+block.displacementFactor);
            System.out.println("Suport : "+block.supportFactor);
            System.out.println("Bottom Up: " + block.bottomUpFactor);
        }
        for (ABObject block : allBlocks)
        {
            block.topDownFactor = (0.2 * block.penetrationFactor) + (0.2 * block.displacementFactor) + (0.05 * block.downwardFactor) + (0.4 * block.weakVicinityFactor);
            System.out.println("DownwardFactor : " + block.downwardFactor);
            System.out.println("Top Down: " + block.topDownFactor);
        }

        //BottomUp
        for (int i = 0; i < allBlocks.size(); i++) {
            for (int j = 0; j < allBlocks.size() - 1; j++) {
                if (allBlocks.get(j).bottomUpFactor < allBlocks.get(j + 1).bottomUpFactor) {
                    ABObject temp = allBlocks.get(j + 1);
                    allBlocks.remove(j + 1);
                    allBlocks.add(j + 1, allBlocks.get(j));
                    allBlocks.remove(j);
                    allBlocks.add(j, temp);
                }
            }
        }

        int count = 0;
        for (int i = 0; i < 5; i++)
        {
            final_list[i][0] = allBlocks.get(count++);
        }

        //TopDown
        for (int i = 0; i < allBlocks.size(); i++) {
            for (int j = 0; j < allBlocks.size() - 1; j++) {
                if (allBlocks.get(j).topDownFactor < allBlocks.get(j + 1).topDownFactor) {
                    ABObject temp = allBlocks.get(j + 1);
                    allBlocks.remove(j + 1);
                    allBlocks.add(j + 1, allBlocks.get(j));
                    allBlocks.remove(j);
                    allBlocks.add(j, temp);
                }
            }
        }

        count = 0;
        for (int i = 0; i < 5; i++) {
            final_list[i][1] = allBlocks.get(count++);
        }
        return final_list;
    }

    public void calcWeakVicinity()
    {
        ArrayList<ABObject> allBlocks = getAllBlocks();
        ArrayList<ABObject> piggies = new ArrayList<ABObject>();
        if(pigs != null)
            piggies.addAll(pigs);
        Point pigs_com = getCenterOfMass(piggies);
        Point weak_point = WeakJoint(vision, pigs_com);
        double factor = 0;
        System.out.println("++===WVF=+++===");
        for(ABObject block: allBlocks)
        {
            factor = 1/(distance(block.getCenter(), weak_point));
            System.out.println("FACTOR: " + factor);
            block.weakVicinityFactor = factor;
        }
        System.out.println("++====+++===");
    }

    public Point getCenterOfMass(ArrayList<ABObject> objects){
        int mass_length_product_x = 0;
        int mass_length_product_y = 0;
        int total_mass = 0;

        for(ABObject obj : objects){
            mass_length_product_x += (obj.width*obj.height*getBlockDensity(obj))*obj.getCenterX();
            mass_length_product_y += (obj.width*obj.height*getBlockDensity(obj))*obj.getCenterY();
            total_mass += obj.width*obj.height*getBlockDensity(obj);
        }

        return (new Point((mass_length_product_x/total_mass), (mass_length_product_y/total_mass)));
    }

    public int getNetDistanceOfAllPointsFromObject(Point reference_point, ArrayList<ABObject> objects){
        int total_distance = 0;

        for(ABObject obj : objects){
            total_distance += Math.sqrt((reference_point.getX() - obj.getCenterX())*(reference_point.getX() - obj.getCenterX()) + (reference_point.getY() - obj.getCenterY())*(reference_point.getY() - obj.getCenterY()) );
        }

        return total_distance;
    }

    public ABObject computeFinalBlocks2() {
//        ABObject[][] final_list = new ABObject[5][2];
        ArrayList<ABObject> allBlocks = getAllBlocks();
        if(pigs != null)
            allBlocks.addAll(pigs);
        ArrayList<ABObject> piggies = new ArrayList<ABObject>();
        if(pigs != null)
            piggies.addAll(pigs);

        Point total_com = getCenterOfMass(allBlocks);
        Point pigs_com = getCenterOfMass(piggies);
        System.out.println("Totol COM " + total_com);
        System.out.println("Pig COM " + pigs_com);

        System.out.println("Distance Pig COM and Total Com " + (distance(pigs_com, total_com)));
        if(distance(pigs_com, total_com) < 70 || true)
        {
            ABObject top_target = topToppleTarget(total_com);
            ABObject bottom_target = bottomSlideTarget(total_com);
            System.out.println("top_tagert X: " + ((top_target)));
            System.out.println("bootom_tagert X: " + ((bottom_target)));

            if(top_target!=null)
                return top_target;
            else if(bottom_target!=null)
                return bottom_target;
        }
        return null;
    }

    private double distance(Point p1, Point p2) {
        return Math
                .sqrt((double) ((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y)
                        * (p1.y - p2.y)));
    }

    ABObject topToppleTarget(Point total_com)
    {
        ArrayList<ABObject> all_blocks = getAllBlocks();
        ArrayList<ABObject> left_top_blocks = new ArrayList<ABObject>();
        double max_factor = Double.MIN_VALUE;
        ABObject target_block = null;
        for (ABObject block : all_blocks) {
            System.out.println("DisplacementFactor : " + Double.toString(block.displacementFactor));
            if (block.getX() < total_com.getX() && block.getY() < total_com.getY()) {
                left_top_blocks.add(block);
                if (max_factor < (block.displacementFactor + companionFactor(block))) {
                    target_block = block;
                    max_factor = block.displacementFactor + companionFactor(block);
                }
            }
        }

        return target_block;
    }

    public ABObject bottomSlideTarget(Point total_com)
    {
        ArrayList<ABObject> all_blocks = getAllBlocks();
        ArrayList<ABObject> left_bottom_blocks = new ArrayList<ABObject>();
        ABObject target_block = null;

        double max_factor = Double.MIN_VALUE;

        for(ABObject block : all_blocks)
        {
            if(block.getX() < total_com.getX() && block.getY() > total_com.getY() )
            {
                left_bottom_blocks.add(block);
                if(max_factor < (block.displacementFactor + companionFactor(block)))
                {
                    max_factor = block.displacementFactor + companionFactor(block);
                    target_block = block;
                }
            }
        }

        return target_block;
    }

    int companionFactor(ABObject block)
    {
        if(current_bird==ABType.RedBird)
        {
            return block.type==ABType.Wood? 20:block.type==ABType.Stone?10:20;
        }
        else if(current_bird==ABType.YellowBird)
        {
            return block.type==ABType.Wood? 30:block.type==ABType.Stone?10:20;
        }
        else if(current_bird==ABType.BlueBird)
        {
            return block.type==ABType.Wood? 10:block.type==ABType.Stone?5:40;
        }
        else
            return 0;
    }

    public Point WeakJoint(Vision vision, Point pig)
    {
        List<ABObject> blocks = vision.findBlocksMBR();
        ABObject u_target_block=null;
        ABObject l_target_block=null;
        Double u_min_dist = Double.MAX_VALUE;
        Double l_min_dist = Double.MAX_VALUE;
        for(int i=0; i<blocks.size();i++)
        {
            ABObject b = blocks.get(i);
            if(pig.y-b.y>0 && pig.x>(b.x) && pig.x<(b.x+b.width) && u_min_dist>pig.y-b.y)
            {
                u_min_dist = ((double)pig.y-(double)b.y);
                u_target_block = b;
            }
            else if(b.y-pig.y>0 && pig.x>(b.x-b.width/2) && pig.x<(b.x+b.width/2) && l_min_dist>b.y-pig.y)
            {
                l_min_dist = ((double)b.y-(double)pig.y);
                l_target_block = b;
            }
        }
        if(u_target_block==null && l_target_block!=null)
        {
            int ximpactAt=0,yimpactAt=0;
            if(pig.x-l_target_block.x>15)
            {
                ximpactAt = l_target_block.x;
                yimpactAt = l_target_block.y;
            }
            else
            {
                ximpactAt = pig.x;
                yimpactAt = pig.y;
            }
            return  new Point(ximpactAt, yimpactAt);
        }
        else if(u_target_block!=null)
        {
            return  new Point(u_target_block.x, u_target_block.y+u_target_block.height/2);
        }
        else
            return pig;
    }
}

