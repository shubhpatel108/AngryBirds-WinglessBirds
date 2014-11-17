/*****************************************************************************
 ** ANGRYBIRDS AI AGENT FRAMEWORK
 ** Copyright (c) 2014, XiaoYu (Gary) Ge, Stephen Gould, Jochen Renz
 **  Sahan Abeyasinghe,Jim Keys,  Andrew Wang, Peng Zhang
 ** All rights reserved.
**This work is licensed under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
**To view a copy of this license, visit http://www.gnu.org/licenses/
 *****************************************************************************/
package ab.demo;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import ab.demo.HeuristicEngine;
import ab.demo.other.ActionRobot;
import ab.demo.other.Shot;
import ab.planner.TrajectoryPlanner;
import ab.utils.StateUtil;
import ab.utils.ABUtil;
import ab.vision.ABObject;
import ab.vision.GameStateExtractor;
import ab.vision.GameStateExtractor.GameState;
import ab.vision.Vision;
import ab.demo.StructureStudy;
import ab.vision.ABType;


public class NaiveAgent implements Runnable {

	private ActionRobot aRobot;
	private Random randomGenerator;
	public int currentLevel = 1;
	public static int time_limit = 12;
	private Map<Integer,Integer> scores = new LinkedHashMap<Integer,Integer>();
	TrajectoryPlanner tp;
	private boolean firstShot;
	private Point prevTarget;

    public ArrayList<Double> normalizedscorelist = new ArrayList<Double>();
    public ArrayList<Integer> levellist = new ArrayList<Integer>();
	public boolean isrepeating = false;
    int repeatingcurrentlevel = 1;

	// a standalone implementation of the Naive Agent
	public NaiveAgent() {
		
		aRobot = new ActionRobot();
		tp = new TrajectoryPlanner();
		prevTarget = null;
		firstShot = true;
		randomGenerator = new Random();
		// --- go to the Poached Eggs episode level selection page ---
		ActionRobot.GoFromMainMenuToLevelSelection();

	}

	
	// run the client
	public void run() {

		aRobot.loadLevel(currentLevel);
		while (true) {
			GameState state = solve();
            if(isrepeating == false) {
				if (state == GameState.WON) {
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					int score = StateUtil.getScore(ActionRobot.proxy);
					if(!scores.containsKey(currentLevel))
						scores.put(currentLevel, score);
					else
					{
						if(scores.get(currentLevel) < score)
							scores.put(currentLevel, score);
					}
					int totalScore = 0;
					for(Integer key: scores.keySet()){

						totalScore += scores.get(key);
						System.out.println(" Level " + key
								+ " Score: " + scores.get(key) + " ");
					}
					if(currentLevel == 8)
                    {
                        isrepeating = true;
                        for(int i = 1;i <=normalizedscorelist.size(); i++)
                        {
                            levellist.add(i, i);
                        }
                        for(int i=0; i< normalizedscorelist.size()-1; i++){
                            for(int j = i + 1; j < normalizedscorelist.size(); j++) {
                                if(normalizedscorelist.get(i) > normalizedscorelist.get(j))
                                {
                                    int temp = levellist.get(i);
                                    levellist.add(i, levellist.get(j));
                                    levellist.add(j, temp);
                                }
                            }
                        }
                        aRobot.loadLevel(levellist.get(0));
                    }
                    else
                    {
                        aRobot.loadLevel(++currentLevel);
                        // make a new trajectory planner whenever a new level is entered
                        tp = new TrajectoryPlanner();
                        // first shot on this level, try high shot first
                        firstShot = true;
                    }
				}
			} else if (state == GameState.LOST)
			{
				if(currentLevel == 8)
                    {
                        isrepeating = true;
                        for(int i = 1;i <=normalizedscorelist.size(); i++)
                        {
                            levellist.add(i, i);
                        }
                        for(int i=0; i< normalizedscorelist.size()-1; i++){
                            for(int j = i + 1; j < normalizedscorelist.size(); j++) {
                                if(normalizedscorelist.get(i) > normalizedscorelist.get(j))
                                {
                                    int temp = levellist.get(i);
                                    levellist.add(i, levellist.get(j));
                                    levellist.add(j, temp);
                                }
                            }
                        }
                        aRobot.loadLevel(levellist.get(0));
                    }
                    else {
                        System.out.println("Restart");
                        aRobot.loadLevel(++currentLevel);
                    }
			} 
			else
			{
                aRobot.loadLevel(levellist.get(repeatingcurrentlevel++));
			} 
			if (state == GameState.LEVEL_SELECTION) {
				System.out
				.println("Unexpected level selection page, go to the last current level : "
						+ currentLevel);
				aRobot.loadLevel(currentLevel);
			} else if (state == GameState.MAIN_MENU) {
				System.out
				.println("Unexpected main menu page, go to the last current level : "
						+ currentLevel);
				ActionRobot.GoFromMainMenuToLevelSelection();
				aRobot.loadLevel(currentLevel);
			} else if (state == GameState.EPISODE_MENU) {
				System.out
				.println("Unexpected episode menu page, go to the last current level : "
						+ currentLevel);
				ActionRobot.GoFromMainMenuToLevelSelection();
				aRobot.loadLevel(currentLevel);
			}

		}

	}

	private double distance(Point p1, Point p2) {
		return Math
				.sqrt((double) ((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y)
						* (p1.y - p2.y)));
	}

	public GameState solve()
	{

		// capture Image
		BufferedImage screenshot = ActionRobot.doScreenShot();

		// process image
		Vision vision = new Vision(screenshot);

		// find the slingshot
		Rectangle sling = vision.findSlingshotMBR();

		// confirm the slingshot
		while (sling == null && aRobot.getState() == GameState.PLAYING) {
			System.out
			.println("No slingshot detected. Please remove pop up or zoom out");
			ActionRobot.fullyZoomOut();
			screenshot = ActionRobot.doScreenShot();
			vision = new Vision(screenshot);
			sling = vision.findSlingshotMBR();
		}
        // get all the pigs
 		List<ABObject> pigs = vision.findPigsMBR();

		GameState state = aRobot.getState();

		// if there is a sling, then play, otherwise just skip.
		if (sling != null) {

			if (!pigs.isEmpty()) {

				Point finalReleasePoint = null;
				GameStateExtractor stateExtractor = new GameStateExtractor();
                int beginScore = stateExtractor.getScoreInGame(screenshot);
				Shot shot = new Shot();
				int dx,dy;
					//ProbeBlocks(vision);
                    List<ABObject> woods = vision.getMBRVision().constructABObjects(vision.getMBRVision().findWoodMBR(), ABType.Wood);
                    List<ABObject> ices = vision.getMBRVision().constructABObjects(vision.getMBRVision().findIceMBR(),ABType.Ice);
                    List<ABObject> stones = vision.getMBRVision().constructABObjects(vision.getMBRVision().findStonesMBR(),ABType.Stone);
                    List<ABObject> TNTs = vision.getMBRVision().constructABObjects(vision.getMBRVision().findTNTsMBR(), ABType.TNT);
                    HeuristicEngine he = new HeuristicEngine(pigs, woods, ices, stones,TNTs, screenshot, aRobot.getBirdTypeOnSling(), sling);
					StructureStudy ss = new StructureStudy(pigs, TNTs, ices, stones, woods);
                	boolean check = true;

                    ABObject blockToHit = GetTopPig(vision);
//                    ABObject blockToHit = he.computeFinalBlocks2();

                    List<Point> releasePoints;
                	Point refPoint = tp.getReferencePoint(sling);

	                if(pigs.size()==1)
	                {
	                    blockToHit = pigs.get(0);
	                    releasePoints=tp.estimateLaunchPoint(sling,blockToHit.getCenter());
	                    dx = (int)releasePoints.get(0).getX() - refPoint.x;
	                    dy = (int)releasePoints.get(0).getY() - refPoint.y;
	                    shot = new Shot(refPoint.x, refPoint.y, dx, dy, 0, 0);
	                    if(ABUtil.isReachable(vision,blockToHit.getCenter(),shot))
	                    {
	                        finalReleasePoint = releasePoints.get(0);
	                        check = false;
	                    }
	                    else
	                    {
	                        if(releasePoints.size()==2) {
	                            finalReleasePoint = releasePoints.get(1);
	                            check = false;
	                        }
	                    }
	                }

	                if(check)
                	{
    					Rectangle outerRectangle = ss.getStructureOutline();
						double[][][] vectoredStructure = ss.calulate_vectors(outerRectangle);
						he.makeAirBlocks(vectoredStructure,outerRectangle);
	                    he.generateSubStructures();
	                    he.calcSupportFactor();
	                    he.calcDownwardFactor();
	                    he.calcDisplacementFactor();
	                    he.calcPenetrationFactor();
	                    he.calcWeakVicinity();



						ArrayList<ABObject> finalCandidateBlocks = he.computeFinalBlocks();

                    	blockToHit = he.filterFinalTarget(finalCandidateBlocks);
	                    if(isrepeating)
	                    {
	                        finalCandidateBlocks.remove(blockToHit);
	                        blockToHit = he.filterFinalTarget(finalCandidateBlocks);
	                    }

	                    System.out.println("Block to hit: " + blockToHit);
	                }
	                Point targetPoint = blockToHit.getCenter();

	                //Right now bottom up so choose low angle shot
	                releasePoints = tp.estimateLaunchPoint(sling,targetPoint);
	                System.out.println("releasepoints="+releasePoints.size());

                    if(releasePoints.size()==0)
                    {
                        {
                            System.out.println("No release point found for the target");
                            System.out.println("Try hitting Top pig");
                            blockToHit = GetTopPig(vision);
                            targetPoint = blockToHit.getCenter();
                            releasePoints = tp.estimateLaunchPoint(sling,targetPoint);
                        }
                    }
                    else
	                    finalReleasePoint = releasePoints.get(0);

//					ABObject pig = GetTopPig(vision);
//					Point _tpt = WeakJoint(vision, pig);

					refPoint = tp.getReferencePoint(sling);


					//Calculate the tapping time according the bird type 
					if (finalReleasePoint != null) {
						double releaseAngle = tp.getReleaseAngle(sling,
                                finalReleasePoint);
						System.out.println("Release Point: " + finalReleasePoint);
						System.out.println("Release Angle: "
								+ Math.toDegrees(releaseAngle));
						int tapInterval = 0;
						switch (aRobot.getBirdTypeOnSling()) 
						{

						case RedBird:
							tapInterval = 0; break;               // start of trajectory
						case YellowBird:
							tapInterval = 65 + 20/*randomGenerator.nextInt(25)*/;break; // 65-90% of the way
						case WhiteBird:
							tapInterval =  70 + randomGenerator.nextInt(20);break; // 70-90% of the way
						case BlackBird:
							tapInterval =  70 + randomGenerator.nextInt(20);break; // 70-90% of the way
						case BlueBird:
							tapInterval =  65 + 20/*randomGenerator.nextInt(20)*/;break; // 65-85% of the way
						default:
							tapInterval =  60;
						}

						int tapTime = tp.getTapTime(sling, finalReleasePoint, targetPoint, tapInterval);
						dx = (int)finalReleasePoint.getX() - refPoint.x;
						dy = (int)finalReleasePoint.getY() - refPoint.y;
						shot = new Shot(refPoint.x, refPoint.y, dx, dy, 0, tapTime);
					}
					else
						{
							System.err.println("No Release Point Found");
							return state;
						}

				// check whether the slingshot is changed. the change of the slingshot indicates a change in the scale.
				{
					int maxscore = 0;
					ActionRobot.fullyZoomOut();
					screenshot = ActionRobot.doScreenShot();
					vision = new Vision(screenshot);
					Rectangle _sling = vision.findSlingshotMBR();
					if(_sling != null)
					{
						double scale_diff = Math.pow((sling.width - _sling.width),2) +  Math.pow((sling.height - _sling.height),2);
						if(scale_diff < 25)
						{
							if(dx < 0)
							{
								aRobot.cshoot(shot);
								state = aRobot.getState();
								if ( state == GameState.PLAYING )
								{
									if(firstShot == true)
										maxscore = he.getMaximumScore();
									screenshot = ActionRobot.doScreenShot();
									vision = new Vision(screenshot);
									List<Point> traj = vision.findTrajPoints();
									tp.adjustTrajectory(traj, sling, finalReleasePoint);
									firstShot = false;
									stateExtractor = new GameStateExtractor();
                                    int finalScore = stateExtractor.getScoreInGame(screenshot);
                                    int scoreGained = finalScore - beginScore;
                                    int totalScore = he.getMaximumScore();
								}
								else if(state==GameState.WON)
                                {
                                    screenshot = ActionRobot.doScreenShot();
                                    stateExtractor = new GameStateExtractor();
                                    int finalScore = stateExtractor.getScoreEndGame(screenshot);
                                    int totalScore = he.getMaximumScore();
                                    int scoreGained = finalScore - beginScore;
                                    normalizedscorelist.add(currentLevel,(double)(100*finalScore)/maxscore);
                                }
							}
						}
						else
							System.out.println("Scale is changed, can not execute the shot, will re-segement the image");
					}
					else
						System.out.println("no sling detected, can not execute the shot, will re-segement the image");
				}

			}

		}
		return state;
	}

	public ABObject GetTopPig(Vision vision)
	{
		List<ABObject> pigs = vision.findPigsMBR();
		int maxIndex = 0;
		Double maxHeight = Double.MAX_VALUE;
		for(int i=0;i<pigs.size();i++)
		{
			if(pigs.get(i).getCenter().getY()<maxHeight)
			{
				maxHeight = pigs.get(i).getCenter().getY();
				maxIndex = i;
			}
		}
		return pigs.get(maxIndex);
	}

	public Point WeakJoint(Vision vision, ABObject pig)
	{
		List<ABObject> blocks = vision.findBlocksMBR();
		ABObject u_target_block=null;
		ABObject l_target_block=null;
		Double u_min_dist = Double.MAX_VALUE;
		Double l_min_dist = Double.MAX_VALUE;
		for(int i=0; i<blocks.size();i++)
		{
			ABObject b = blocks.get(i);
			if(pig.y-b.y>0 && pig.x>(b.x-b.width/2) && pig.x<(b.x+b.width/2) && u_min_dist>pig.y-b.y)
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
			int ximpactAt=0;
			if(pig.x-l_target_block.x>15)
				ximpactAt = pig.x-(int)((l_target_block.width/2));
			else
				ximpactAt = pig.x;
			return  new Point(ximpactAt, l_target_block.y-l_target_block.height/2 - 3);
		}
		else if(u_target_block!=null && l_target_block==null)
		{
			return  new Point(u_target_block.x-(u_target_block.width/2), u_target_block.y-u_target_block.height/2);
		}
		else if(u_target_block!=null && l_target_block!=null)
		{
			return  new Point(l_target_block.x-(l_target_block.width/2), l_target_block.y-l_target_block.height/2);
		}
		else
			return pig.getCenter();
	}

	public void ProbeBlocks(Vision vision)
	{
		List<ABObject> all_blocks = vision.findBlocksRealShape();
		System.out.println("Block Id\tShape\tMaterial");
		ABObject block=null;
		for(int i=0;i<all_blocks.size();i++)
		{
			block = all_blocks.get(i);
			System.out.println(block.id+"\t\t"+block.shape+"\t\t"+block.type);
		}
	}

	public static void main(String args[]) {

		NaiveAgent na = new NaiveAgent();
		if (args.length > 0)
			na.currentLevel = Integer.parseInt(args[0]);
		na.run();

	}
}
