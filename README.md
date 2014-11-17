AngryBirds: Wingless-Birds
Artificial Intelligence (IT-326)
Guided by: Prof. Sourish Dasgupta

ABSTRACT
This paper briefly describes the approach taken in developing the Wingless-Birds Heuristic Engine to solve the Angry Birds Problem by an Artificial Agent. All the steps used in procedure are the product of ideas of the Prof. Sourish Dasgupta, some students (named above), some conventional probabilistic approaches used by some AI Contest Participants in past, and RL-Learning technique learned during Artificial Intelligence Course thought by Prof. Sourish Dasgupta.
1.	OVERVIEW
The entire problem is divided into two different modules:
  1)	Finding the target block/object for the given structural situation.
  2)	Reinforcement Learning:
    a.	Compare the given structure with those encountered in past.
    b.	Regret if the last target didn’t help in scoring well and change the target in the next iteration.
2.	ALGORITHM
  1)	Finding the target Block/Object
    a.	Divide the Entire Structure into Sub structure by detecting all air blocks in a vertical line that divides the substructure.
    b.	Each Object is assigned density (an integer number) as per its type (Wood, Ice, Stone, TNT).
    c.	Calculate Centre of Mass of all blocks (without Pigs) and of all objects (blocks and pigs both).
    d.	Each object has four factors associated with it:
      i.	Reaching Factor – For all the possibilities of Trajectories see if the block can be reach. (E.g: What if the block is in a hollow hill?). It is mathematically sum of density of all block lying in all possible trajectories.
      ii.	Gravity Effect Factor – This describes the nature of blocks lying under the block and reflects the effect that will happen due to gravity when this block is hit. It is sum of density of all blocks lying exactly under the block
      iii.	Strength Giving Factor – It describes the strength it gives to the pigs. Mathematically, it is sum of density of all blocks that lye on line of connection with each pig. 
      iv.	Weak Vicinity Factor – It describes the closeness of the block to the WeakPoint. The weak point is having least weighted sum of its distance from CoM of Pigs and CoM of all Blocks.
      v.	Horizontal Effect Factor – Sum of density of all blocks that lies horizontally of right of the block.
    e.	Rank all the objects according to sum of weighted sum of the above factors.
    f.	According to the Difference in Centre of Mass of All Objects and CoM of Pigs and cumulative Factors values finalize the Block for hitting.
  
  2)	Reinforcement Learning
    a.	Regret: If the last short doesn’t produce a score put to a defined standard, hit the block with second rank.
    b.	Explore or Exploit? 
      i.	Divide the Structure in small blocks by virtually drawing Grid Lines. Store the sum of density of all blocks in each grid square in 2D array. Store the structure with the target and score in Hash.
      ii.	Compare the new structure with the previous structures and Find the difference in structure.
      iii.	If the difference in more than standard value go for Exploring.
      iv.	Else repeat the same thing done for that structure. Exploit the result found.
  c.	If the level fails for some kind of Sequence of Actions executed, set the failed flag up. Target the blocks next in the ranking if the failed flag is set. For every successfully clearance of level Store the result in Exploit array for future use.

3.	Acknowledgements
Special thanks to Prof. Sourish Dasgupta for everything that is embedded in our brain.
Thanks to the students who participated in the AI Contest in the past for guidance. 
Without all these people imparting Artificial Intelligence would not have been a job of the ones with Natural Intelligence.

4.	Scores when Simulated on Poached Eggs Episode
Level	Score
1	29030
2	52610
3	42250
4	29690
5	59400
6	35600
7	23990
8	47330
9	36870
10	49920
11	39110
12	52440
13	37990
14	67800
15	39930
16	60210
17	37650
18	50290
19	36870
20	39510
21	64220
Total	932710



Thanks!

Shubham Patel (201201182)
Shubham Jethwa (201201151)
Prashant Vithani (201201210)
