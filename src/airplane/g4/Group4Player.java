package airplane.g4;

import airplane.sim.Plane;
import airplane.sim.Player;
import org.apache.log4j.Logger;

import java.awt.geom.Point2D;
import java.util.ArrayList;

public class Group4Player extends Player {

    private Logger logger = Logger.getLogger(this.getClass());

    private double distance = 0.0;

    private double prepareDistance = 0.0;

    private Double[] originBearings = null;

    private final int prepareStep = 6;

    @Override
    public String getName() {
        return "Group4Player";
    }

    @Override
    public void startNewGame(ArrayList<Plane> planes) {
        logger.info("Starting new game!");
        // 80°
        originBearings = new Double[planes.size()];
        for(int i = 0; i < planes.size(); i++){
            Plane p = planes.get(i);
            distance = p.getLocation().distance(p.getDestination());
            originBearings[i] = calculateBearing(p.getLocation(), p.getDestination());
        }
        for(int i = 10 * prepareStep; i >= 10; i = i - 10){
            prepareDistance += Math.cos(Math.toRadians(i));
        }
    }

    @Override
    public double[] updatePlanes(ArrayList<Plane> planes, int round, double[] bearings) {
        int centerFlyTime = (int) Math.floor(distance - 2 * prepareDistance);
        for(int i = 0; i < planes.size(); i++){
            Plane p = planes.get(i);
            int depTime = p.getDepartureTime() + 1;
            if(round >= depTime){
                int depRound = round - depTime;
                if(depRound <= prepareStep){
                    // Within 6 rounds, it is the preparing phase,
                    // to make sure two planes are 5 steps apart
                    bearings[i] = originBearings[i] - 10 * (prepareStep + 1) + 10 * depRound;
                }else if(depRound <= 6 + centerFlyTime){
                    // Middle phase
                    bearings[i] = originBearings[i];
                }else{
                    // End phase
                    double desBearing = calculateBearing(p.getLocation(), p.getDestination());
                    // If the difference between desBearing and current bearing is greater than 10,
                    // add 10 to the current bearing
                    // otherwise do not change the bearing
                    if(Math.abs(bearings[i] - desBearing) > 10){
                        bearings[i] = bearings[i] + 10;
                    }else{
                        bearings[i] = desBearing;
                    }
                }
            }
        }
        for(int i = 0;i < planes.size();i++){
            Plane p = planes.get(i);
            if(bearings[i] < 0){
                bearings[i] = 360 + bearings[i];
            }
            bearings[i] = bearings[i] % 360;
        }
        return bearings;
    }
}
