package airplane.g4;

import airplane.sim.Plane;
import airplane.sim.Player;
import org.apache.log4j.Logger;

import java.awt.geom.Point2D;
import java.util.*;

public class Group4Player4 extends Player {

    private Logger logger = Logger.getLogger(this.getClass());

    private int delayRound = 20;

    private int forecastRound = 9;

    private int conflictDistance = 7;
    /**
     * String: destination
     * Long: estimated arrival round
     */
    private Map<String, Integer> planeArrivalRound;

    @Override
    public String getName() {
        return "Group4Player4";
    }

    @Override
    public void startNewGame(ArrayList<Plane> planes) {
        logger.info("Starting new game!");
        this.planeArrivalRound = new HashMap<>();
    }

    @Override
    public double[] updatePlanes(ArrayList<Plane> planes, int round, double[] bearings) {
        logger.info("before round: " + round + ", bearings: " + Arrays.toString(bearings));
        // In the same round, only one plane could take off
        Set<Long> departurePlanes = new HashSet<>();
        for(int i = 0; i < planes.size(); i++){
            // Before departure
            if(planes.get(i).getBearing() == -1 && round >= planes.get(i).getDepartureTime() + 1){
                // if there is plane in the radius of 30 from the origin, wait
                boolean canTakeOff = true;
                for(int j = 0; j < planes.size(); j++){
                    if(i != j && planes.get(j).getBearing() != -2 && planes.get(j).getBearing() != -1
                            && planes.get(j).getLocation().distance(planes.get(i).getLocation()) <= 30){
                        canTakeOff = false;
                        break;
                    }
                }
                if(departurePlanes.size() > 0){
                    canTakeOff = false;
                }

                if(!canTakeOff) continue;

                // if there is no in air airplane has the same destination with the current plane
                // and the current plane is waiting to departure, then departure.
                if(!planeArrivalRound.containsKey(planes.get(i).getDestination().toString())){
                    bearings[i] = calculateBearing(planes.get(i).getLocation(), planes.get(i).getDestination());
                    double distance = planes.get(i).getLocation().distance(planes.get(i).getDestination());
                    planeArrivalRound.put(planes.get(i).getDestination().toString(), (int) (round + distance));
                    departurePlanes.add((long) i);
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
                        departurePlanes.add((long) i);
                    }
                }
            }
        }

        UnionFind unionFind = new UnionFind();
        // Key: planeId, Value: forecastLocations
        Map<Long, List<Point2D.Double>> forecastLocations = new HashMap<>();
        for(int i = 0; i < planes.size(); i++) {
            // Calculate all in air airplanes' location in forecast rounds
            if (planes.get(i).getBearing() != -1 && planes.get(i).getBearing() != -2) {
                Long planeId = (long) i;
                unionFind.add(planeId);
                Point2D.Double currentLocation = planes.get(i).getLocation();
                // Calculate the forecast rounds based on current bearing
                double arrivalLeftRound = currentLocation.distance(planes.get(i).getDestination());
                for(int k = 1; k <= Math.min(arrivalLeftRound,forecastRound); k++){
                    Point2D.Double forecastLocation = calculateNextLocation(currentLocation, bearings[i], k);
                    forecastLocations.computeIfAbsent(planeId, v -> new ArrayList<>()).add(forecastLocation);
                    // check if the plane will be less than 5 units to other planes in forecast round
                    for (Map.Entry<Long, List<Point2D.Double>> entry : forecastLocations.entrySet()) {
                        Long otherPlaneId = entry.getKey();
                        if (!planeId.equals(otherPlaneId)) {
                            for (Point2D.Double location : entry.getValue()) {
                                if (forecastLocation.distance(location) < conflictDistance) {
                                    unionFind.union(planeId, otherPlaneId);
                                }
                            }
                        }
                    }
                }
            }
        }

        Map<Long, Set<Long>> conflictGroups = unionFind.getConnectedComponents();
        List<Set<Long>> listOfConflictGroups = new ArrayList<>(conflictGroups.values());
        logger.info("round: " + round + ", listOfConflictGroups: " + listOfConflictGroups.toString());
        // Find all planes that are possible to collide,
        // then change their bearings
        for (Map.Entry<Long, Set<Long>> entry : conflictGroups.entrySet()) {
            Set<Long> group = entry.getValue();
            if (group.size() > 1) {
                if(group.size() == 2){
                    // if there is only 2 conflict planes,
                    // find the one that are further from the destination to change its bearing
                    Iterator<Long> iter = group.iterator();
                    Long planeId1 = iter.next();
                    Long planeId2 = iter.next();

                    Point2D.Double location1 = planes.get(planeId1.intValue()).getLocation();
                    Point2D.Double destination1 = planes.get(planeId1.intValue()).getDestination();
                    Point2D.Double location2 = planes.get(planeId2.intValue()).getLocation();
                    Point2D.Double destination2 = planes.get(planeId2.intValue()).getDestination();

                    // Calculate distance to the destination
                    double distanceToDestination1 = location1.distance(destination1);
                    double distanceToDestination2 = location2.distance(destination2);

                    Long planeToAdjust = (distanceToDestination1 > distanceToDestination2) ? planeId1 : planeId2;

                    // Change the bearing
                    double distance = Math.max(distanceToDestination1, distanceToDestination2);
                    double angle = distance > 10 ? 9.9 : 3;
                    bearings[planeToAdjust.intValue()] = (bearings[planeToAdjust.intValue()] + angle + 360) % 360;
                }else {
                    // There are more than 2 conflict planes,
                    // find the most urgent pair of conflict planes,
                    // and find the one that are further from the destination to change its bearing
                    double minDistance = Double.MAX_VALUE;
                    Long planeId1 = null;
                    Long planeId2 = null;
                    for (Long planeId : group) {
                        for(Long otherPlaneId : group){
                            if(!planeId.equals(otherPlaneId)){
                                Point2D.Double location = planes.get(planeId.intValue()).getLocation();
                                Point2D.Double otherLocation = planes.get(otherPlaneId.intValue()).getLocation();
                                double distance = location.distance(otherLocation);
                                if(distance < minDistance){
                                    minDistance = distance;
                                    planeId1 = planeId;
                                    planeId2 = otherPlaneId;
                                }
                            }
                        }
                    }

                    Point2D.Double location1 = planes.get(planeId1.intValue()).getLocation();
                    Point2D.Double destination1 = planes.get(planeId1.intValue()).getDestination();
                    Point2D.Double location2 = planes.get(planeId2.intValue()).getLocation();
                    Point2D.Double destination2 = planes.get(planeId2.intValue()).getDestination();

                    // calculate distance to the destination
                    double distanceToDestination1 = location1.distance(destination1);
                    double distanceToDestination2 = location2.distance(destination2);

                    Long planeToAdjust = (distanceToDestination1 > distanceToDestination2) ? planeId1 : planeId2;

                    // change the bearing
                    bearings[planeToAdjust.intValue()] = (bearings[planeToAdjust.intValue()] + 9.9 + 360) % 360;
                }
            }else{
                for(Long planeId : group){
                    // for plane with no conflicts, change its bearing to the destination
                    double targetBearing = calculateBearing(planes.get(planeId.intValue()).getLocation(), planes.get(planeId.intValue()).getDestination());
                    if(Math.abs(bearings[planeId.intValue()] - targetBearing) > 9) {
                        // check if it is better to change bearings clockwise or counterclockwise
                        if (bearings[planeId.intValue()] > targetBearing) {
                            if (bearings[planeId.intValue()] - targetBearing > 180) {
                                bearings[planeId.intValue()] = (bearings[planeId.intValue()] + 9) % 360;
                            } else {
                                bearings[planeId.intValue()] = (bearings[planeId.intValue()] - 9 + 360) % 360;
                            }
                        } else {
                            if (targetBearing - bearings[planeId.intValue()] > 180) {
                                bearings[planeId.intValue()] = (bearings[planeId.intValue()] - 9 + 360) % 360;
                            } else {
                                bearings[planeId.intValue()] = (bearings[planeId.intValue()] + 9) % 360;
                            }
                        }
                    }else{
                        bearings[planeId.intValue()] = targetBearing;
                    }
                }
            }
        }


        // If the plane arrives, change its bearing to -2
        for(int i = 0; i < planes.size(); i++){
            if(planes.get(i).getLocation().distance(planes.get(i).getDestination()) <= 0.5){
                bearings[i] = -2;
            }
        }
        logger.info("after round: " + round + ", bearings: " + Arrays.toString(bearings));
        logger.info("current round: " + round + ", departurePlanes: " + departurePlanes.toString());
        return bearings;
    }

    private Point2D.Double calculateNextLocation(Point2D.Double currentLocation, double bearing, int round){
        double x = currentLocation.x + Math.cos(Math.toRadians(bearing - 90)) * round;
        double y = currentLocation.y + Math.sin(Math.toRadians(bearing - 90)) * round;
        return new Point2D.Double(x, y);
    }

    public class UnionFind {
        private Map<Long, Long> parent = new HashMap<>();

        public void add(Long x) {
            parent.putIfAbsent(x, x);
        }

        public Long find(Long x) {
            if (!Objects.equals(parent.get(x), x)) {
                parent.put(x, find(parent.get(x)));
            }
            return parent.get(x);
        }

        public void union(Long x, Long y) {
            Long rootX = find(x);
            Long rootY = find(y);
            if (!Objects.equals(rootX, rootY)) {
                parent.put(rootX, rootY);
            }
        }

        public Map<Long, Set<Long>> getConnectedComponents() {
            Map<Long, Set<Long>> components = new HashMap<>();
            for (Long x : parent.keySet()) {
                Long root = find(x);
                components.putIfAbsent(root, new HashSet<>());
                components.get(root).add(x);
            }
            return components;
        }
    }

}
