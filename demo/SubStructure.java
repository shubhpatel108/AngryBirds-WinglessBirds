package ab.demo;

import ab.utils.ABUtil;
import ab.vision.ABObject;

import java.util.ArrayList;
import java.util.List;

public class SubStructure
{
    public ArrayList<ABObject> structureObjects;
    public static List<ABObject> allObjects;
    public double maxY;
    public double minY;
    public double maxX;
    public double minX;

    public SubStructure()
    {
        structureObjects = new ArrayList<ABObject>();
    }

    public void addBlocks(ABObject obj) {
        List<ABObject> supports = ABUtil.getSupporters(obj, allObjects);
        List<ABObject> supportees = ABUtil.getSupportees(obj, allObjects);
        structureObjects.add(obj);
        allObjects.remove(obj);
        for (ABObject o2 : supports)
        {
            if (!structureObjects.contains(o2))
            {
                addBlocks(o2);
            }
        }

        for (ABObject o2 : supportees)
        {
            if(!structureObjects.contains(o2))
            {
                addBlocks(o2);
            }
        }

    }

    public boolean contains(ABObject block)
    {
        return structureObjects.contains(block);
    }

    public void addSupport(ABObject obj)
    {
        List<ABObject> supports = ABUtil.getSupporters(obj, allObjects);
    }

    public double getMaxY()
    {
        double max = 0;
        if(maxY==0)
        {
            for (ABObject o:structureObjects)
            {
                if(o.getCenterY()>max)
                {
                    max = o.getCenterY();
                }
            }
            maxY = max;
        }
        return maxY;
    }

    public double getMinY()
    {
        double min = 100000;
        if(minY==0)
        {
            for (ABObject o:structureObjects)
            {
                if(o.getCenterY()<min)
                {
                    min = o.getCenterY();
                }
            }
            minY = min;
        }
        return minY;
    }

    public double getMaxX()
    {
        double max = 0;
        if(maxX==0)
        {
            for (ABObject o:structureObjects)
            {
                if(o.getCenterX()>max)
                {
                    max = o.getCenterX();
                }
            }
            maxX = max;
        }
        return maxX;
    }

    public double getMinX()
    {
        double min = 100000;
        if(minX==0)
        {
            for (ABObject o:structureObjects)
            {
                if(o.getCenterX()<min)
                {
                    min = o.getCenterX();
                }
            }
            minX = min;
        }
        return minX;
    }

}
