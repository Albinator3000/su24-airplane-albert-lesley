package airplane.g4;

import airplane.sim.Plane;
import airplane.sim.Player;
import org.apache.log4j.Logger;

import java.awt.geom.Point2D;
import java.util.*;

public class Group4Player2 extends Player {

    private Logger logger = Logger.getLogger(this.getClass());

    private int delayRound = 16;

    private int forecastRound = 7;

    private int conflictDistance = 6;
    /**
     * String: destination
     * Long: estimated arrival round
     */
    private Map<String, Integer> planeArrivalRound;

    @Override
    public String getName() {
        return "Group4Player2";
    }

    @Override
    public void startNewGame(ArrayList<Plane> planes) {
        logger.info("Starting new game!");
        this.planeArrivalRound = new HashMap<>();
    }

    @Override
    public double[] updatePlanes(ArrayList<Plane> planes, int round, double[] bearings) {
        for(int i = 0; i < planes.size(); i++){
            // Before departure
            if(planes.get(i).getBearing() == -1 && round >= planes.get(i).getDepartureTime() + 1){
                // if there is no in air airplane has the same destination with the current plane
                // and the current plane is waiting to departure, then departure.
                if(!planeArrivalRound.containsKey(planes.get(i).getDestination().toString())){
                    bearings[i] = calculateBearing(planes.get(i).getLocation(), planes.get(i).getDestination());
                    double distance = planes.get(i).getLocation().distance(planes.get(i).getDestination());
                    planeArrivalRound.put(planes.get(i).getDestination().toString(), (int) (round + distance));
                }else{
                    // if there is in air airplane has the same destination with the current plane
                    // and the current plane is waiting to departure,
                    // then wait until the difference between departure round and
                    // in air airplane's estimated arrival round is greater than delayRound
                    double distance = planes.get(i).getLocation().distance(planes.get(i).getDestination());
                    int lastPlaneArrivalRound = planeArrivalRound.get(planes.get(i).getDestination().toString());
                    int currentPlaneArrivalRound = (int) (round + distance);
                    if(currentPlaneArrivalRound - lastPlaneArrivalRound >= delayRound){
                        bearings[i] = calculateBearing(planes.get(i).getLocation(), planes.get(i).getDestination());
                        planeArrivalRound.put(planes.get(i).getDestination().toString(), currentPlaneArrivalRound);
                    }
                }
            }
        }

        Set<Long> mayConflictPlanes = new HashSet<>();
        // Key: planeId, Value: forecastLocations
        Map<Long, List<Point2D.Double>> forecastLocations = new HashMap<>();
        for(int i = 0; i < planes.size(); i++) {
            // Calculate all in air airplanes' location in forecast rounds
            if (planes.get(i).getBearing() != -1 && planes.get(i).getBearing() != -2) {
                Point2D.Double currentLocation = planes.get(i).getLocation();
                for(int k = 1; k <= forecastRound; k++){
                    Point2D.Double forecastLocation = calculateNextLocation(currentLocation, bearings[i], k);
                    // check if the plane will be less than 5 units to other planes in forecast round
                    for(Long key : forecastLocations.keySet()){
                        List<Point2D.Double> locations = forecastLocations.get(key);
                        for(Point2D.Double location : locations){
                            if(forecastLocation.distance(location) < conflictDistance){
                                mayConflictPlanes.add((long) i);
                                mayConflictPlanes.add(key);
                            }
                        }
                    }
                }

                // calculate forecast locations
                for(int k = 1; k <= forecastRound; k++) {
                    Point2D.Double forecastLocation = calculateNextLocation(currentLocation, bearings[i], k);
                    if(forecastLocations.containsKey((long) i)) {
                        forecastLocations.get((long) i).add(forecastLocation);
                    }else{
                        List<Point2D.Double> locations = new ArrayList<>();
                        locations.add(forecastLocation);
                        forecastLocations.put((long) i, locations);
                    }
                }
            }
        }

        logger.info("mayConflictPlanes: " + mayConflictPlanes.toString());

        // Find all planes that are possible to collide,
        // then change their bearings
        for(int i = 0; i < planes.size(); i++){
            if(bearings[i] != -1 && bearings[i] != -2){
                if(mayConflictPlanes.contains((long) i)){
                    bearings[i] = (bearings[i] + 10) % 360;

                }else{
                    double targetBearing = calculateBearing(planes.get(i).getLocation(), planes.get(i).getDestination());
                    if(Math.abs(bearings[i] - targetBearing) > 10) {
                        // change the bearing by 10 towards its target bearing
                        bearings[i] = bearings[i] > targetBearing ? bearings[i] - 10 : bearings[i] + 10;
                    }else{
                        bearings[i] = targetBearing;
                    }
                }
            }
        }

        // if the plane arrived, change its bearing to -2
        for(int i = 0; i < planes.size(); i++){
            if(planes.get(i).getLocation().distance(planes.get(i).getDestination()) <= 0.5){
                bearings[i] = -2;
            }
        }


        return bearings;
    }

    private Point2D.Double calculateNextLocation(Point2D.Double currentLocation, double bearing, int round){
        double x = currentLocation.x + Math.cos(Math.toRadians(bearing - 90)) * round;
        double y = currentLocation.y + Math.sin(Math.toRadians(bearing - 90)) * round;
        return new Point2D.Double(x, y);
    }
}