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

import ab.demo.other.ActionRobot;
import ab.demo.other.Shot;
import ab.planner.TrajectoryPlanner;
import ab.utils.StateUtil;
import ab.vision.ABObject;
import ab.vision.GameStateExtractor.GameState;
import ab.vision.Vision;

public class NaiveAgent implements Runnable {

	private ActionRobot aRobot;
	private Random randomGenerator;
	public int currentLevel = 1;
	public static int time_limit = 12;
	private Map<Integer,Integer> scores = new LinkedHashMap<Integer,Integer>();
	TrajectoryPlanner tp;
	private boolean firstShot;
	private Point prevTarget;
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
				System.out.println("Total Score: " + totalScore);
				aRobot.loadLevel(++currentLevel);
				// make a new trajectory planner whenever a new level is entered
				tp = new TrajectoryPlanner();

				// first shot on this level, try high shot first
				firstShot = true;
			} else if (state == GameState.LOST) {
				System.out.println("Restart");
				aRobot.restartLevel();
			} else if (state == GameState.LEVEL_SELECTION) {
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

				Point releasePoint = null;
				Shot shot = new Shot();
				int dx,dy;
				{
					ProbeBlocks(vision);
					// HeuristicEngine he = new HeuristicEngine();
					// StructureStudy ss = new StructureStudy(pigs, TNT, ice, stone, wood);
					// Rectangle outerRectangle = ss.getStructureOutline();
					// double[][][] vectoredStructure = ss.calculate_vector(outerRectangle);
					//he.makeAirBlocks(vectoredStructure,outerRectangle);
					ABObject pig = GetTopPig(vision);
					
					Point _tpt = WeakJoint(vision, pig);

					// if the target is very close to before, randomly choose a
					// point near it
					// if (prevTarget != null && distance(prevTarget, _tpt) < 10) {
					// 	double _angle = randomGenerator.nextDouble() * Math.PI * 2;
					// 	_tpt.x = _tpt.x + (int) (Math.cos(_angle) * 10);
					// 	_tpt.y = _tpt.y + (int) (Math.sin(_angle) * 10);
					// 	System.out.println("Randomly changing to " + _tpt);
					// }

					prevTarget = new Point(_tpt.x, _tpt.y);

					// estimate the trajectory
					ArrayList<Point> pts = tp.estimateLaunchPoint(sling, _tpt);
					
					// do a high shot when entering a level to find an accurate velocity
					if (firstShot && pts.size() > 1) 
					{
						releasePoint = pts.get(0);
					}
					// do a high shot when entering a level to find an accurate velocity
					if (firstShot && pts.size() > 1) 
					{
						releasePoint = pts.get(1);
					}
					else if (pts.size() == 1)
						releasePoint = pts.get(0);
					else if (pts.size() == 2)
					{
						// randomly choose between the trajectories, with a 1 in
						// 6 chance of choosing the high one
						if (randomGenerator.nextInt(6) == 0)
							releasePoint = pts.get(1);
						else
							releasePoint = pts.get(0);
					}
					else
						if(pts.isEmpty())
						{
							System.out.println("No release point found for the target");
							System.out.println("Try a shot with 45 degree");
							releasePoint = tp.findReleasePoint(sling, Math.PI/4);
						}
					
					// Get the reference point
					Point refPoint = tp.getReferencePoint(sling);


					//Calculate the tapping time according the bird type 
					if (releasePoint != null) {
						double releaseAngle = tp.getReleaseAngle(sling,
								releasePoint);
						System.out.println("Release Point: " + releasePoint);
						System.out.println("Release Angle: "
								+ Math.toDegrees(releaseAngle));
						int tapInterval = 0;
						switch (aRobot.getBirdTypeOnSling()) 
						{

						case RedBird:
							tapInterval = 0; break;               // start of trajectory
						case YellowBird:
							tapInterval = 65 + randomGenerator.nextInt(25);break; // 65-90% of the way
						case WhiteBird:
							tapInterval =  70 + randomGenerator.nextInt(20);break; // 70-90% of the way
						case BlackBird:
							tapInterval =  70 + randomGenerator.nextInt(20);break; // 70-90% of the way
						case BlueBird:
							tapInterval =  65 + randomGenerator.nextInt(20);break; // 65-85% of the way
						default:
							tapInterval =  60;
						}

						int tapTime = tp.getTapTime(sling, releasePoint, _tpt, tapInterval);
						dx = (int)releasePoint.getX() - refPoint.x;
						dy = (int)releasePoint.getY() - refPoint.y;
						shot = new Shot(refPoint.x, refPoint.y, dx, dy, 0, tapTime);
					}
					else
						{
							System.err.println("No Release Point Found");
							return state;
						}
				}

				// check whether the slingshot is changed. the change of the slingshot indicates a change in the scale.
				{
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
									screenshot = ActionRobot.doScreenShot();
									vision = new Vision(screenshot);
									List<Point> traj = vision.findTrajPoints();
									tp.adjustTrajectory(traj, sling, releasePoint);
									firstShot = false;
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
