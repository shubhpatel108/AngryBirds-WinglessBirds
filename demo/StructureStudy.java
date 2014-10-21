
import ab.vision.ABObject;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class StructureStudy {

    List<ABObject> _pigs;
    List<ABObject> _wood;
    List<ABObject> _ice;
    List<ABObject> _stone;
    List<ABObject> TNT;
    ArrayList<Point> _pigPoints;
    ArrayList<Point> _woodPoints;
    ArrayList<Point> _icePoints;
    ArrayList<Point> _stonePoints;
    ArrayList<Point> TNTPoints;
    ArrayList<Point> _allPoints;
    int leftX;
    int rightX;
    int topY;
    int bottomY;

    int LENGTH =20;
    double[][][] _vector;

    public StructureStudy(List<ABObject> pigs, List<ABObject> TNT, List<ABObject> ice, List<ABObject> stone, List<ABObject> wood) {
        this._pigs = pigs;
        this._wood = wood;
        this._ice = ice;
        this._stone = stone;
        this.TNT = TNT;
        this._vector = new double[LENGTH][LENGTH][5];
    }

    /**
     * get boundry of the structure
     */
    public Rectangle getStructureOutline() {
        _pigPoints = decomposeToPoints(_pigs);
        _woodPoints = decomposeToPoints(_wood);
        _icePoints = decomposeToPoints(_ice);
        _stonePoints = decomposeToPoints(_stone);
        TNTPoints = decomposeToPoints(TNT);
        _allPoints = new ArrayList<Point>(0);
        _allPoints.addAll(_pigPoints);
        _allPoints.addAll(_woodPoints);
        _allPoints.addAll(_icePoints);
        _allPoints.addAll(_stonePoints);
        _allPoints.addAll(TNTPoints);
        // Find the bounding rectangle
        int minX = 10000;
        int maxX = 0;
        int minY = 10000;
        int maxY = 0;

        for(Point p : _allPoints) {
            int x = (int)p.getX();
            int y = (int)p.getY();
            if(x < minX) minX = x;
            if(x > maxX) maxX = x;
            if(y < minY) minY = y;
            if(y > maxY) maxY = y;
        }

        leftX = minX;
        rightX = maxX;
        topY = minY;
        bottomY = maxY;

        return new Rectangle(minX, minY, (maxX - minX + 1), (maxY - minY + 1));
    }

    public static ArrayList<Point> decomposeToPoints(List<ABObject> object) {
        ArrayList<Point> objectPoints = new ArrayList<Point>(0);
        for (Rectangle r:object) {
            int x = (int)r.getX();
            int y = (int)r.getY();
            int width = (int)r.getWidth();
            int height = (int)r.getHeight();
            for (int i = x; i < x+width; i++)
                for (int j = y; j < y+height; j++)
                    objectPoints.add(new Point(i,j));
        }
        return objectPoints;
    }

    double[][][] calulate_vectors(Rectangle structure) 
    {
        _vector = new double[LENGTH][LENGTH][5];
        vectorize(structure, _pigPoints, 0);
        vectorize(structure, _woodPoints, 1);
        vectorize(structure, _icePoints, 2);
        vectorize(structure, _stonePoints, 3);
        vectorize(structure,TNTPoints,4);
        scale_values(structure);
        return _vector;
    }

    void vectorize(Rectangle structure, List<Point> object, int index) 
    {
        int topY = (int)structure.getY();
        int leftX = (int)structure.getX();
        for (Point p: object) {
            int x = (int)((p.getX() - leftX)*LENGTH/structure.getWidth());
            int y = (int)((p.getY() - topY)*LENGTH/structure.getHeight());
            if (x >= LENGTH) x = LENGTH-1;
            if (y >= LENGTH) y = LENGTH-1;
            _vector[x][y][index]++;
        }
    }

    void scale_values(Rectangle structure)
    {
        int blockPixels = (int)(structure.getHeight()*structure.getWidth()/(LENGTH*LENGTH));
        for (int i = 0; i < LENGTH; i++)
        {
            for (int j = 0; j < LENGTH; j++)
            {
                for (int k = 0; k < 5; k++)
                {
                    _vector[i][j][k] = _vector[i][j][k]*100.0/blockPixels;
                }
            }
        }
    }
}
