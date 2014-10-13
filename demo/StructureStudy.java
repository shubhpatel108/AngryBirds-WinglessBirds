
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

    public VectorQuantizer(List<ABObject> pigs, List<ABObject> TNT, List<ABObject> ice, List<ABObject> stone, List<ABObject> wood) {
        this._pigs = pigs;
        this._wood = wood;
        this._ice = ice;
        this._stone = stone;
        this.TNT = TNT;
        this._vector = new double[LENGTH][LENGTH][5];
    }

}

