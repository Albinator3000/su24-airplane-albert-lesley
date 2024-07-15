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
    private double conflictDistance = 7;
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
        logger.info("Before round: " + round + ", bearings: " + Arrays.toString(bearings));

        handleDepartures(planes, round, bearings);
        handleAirbornePlanes(planes, round, bearings);
        handleArrivals(planes, bearings);

        logger.info("After round: " + round + ", bearings: " + Arrays.toString(bearings));
        return bearings;
    }

    private void handleDepartures(ArrayList<Plane> planes, int round, double[] bearings) {
        boolean departureThisRound = false;
        for (int i = 0; i < planes.size(); i++) {
            Plane plane = planes.get(i);
            if (plane.getBearing() == -1 && round >= plane.getDepartureTime() + 1 && !departureThisRound) {
                if (canTakeOff(planes, plane)) {
                    String destinationKey = plane.getDestination().toString();
                    if (!planeArrivalRound.containsKey(destinationKey) ||
                            round - planeArrivalRound.get(destinationKey) >= delayRound) {
                        initiateTakeoff(plane, i, round, bearings);
                        departureThisRound = true;
                    }
                }
            }
        }
    }

    private boolean canTakeOff(ArrayList<Plane> planes, Plane plane) {
        for (Plane otherPlane : planes) {
            if (otherPlane != plane && otherPlane.getBearing() >= 0 && otherPlane.getBearing() != -2
                    && otherPlane.getLocation().distance(plane.getLocation()) <= 30) {
                return false;
            }
        }
        return true;
    }

    private void initiateTakeoff(Plane plane, int index, int round, double[] bearings) {
        bearings[index] = calculateBearing(plane.getLocation(), plane.getDestination());
        double distance = plane.getLocation().distance(plane.getDestination());
        planeArrivalRound.put(plane.getDestination().toString(), (int) (round + distance));
        logger.info("Plane " + index + " taking off at round " + round);
    }

    private void handleAirbornePlanes(ArrayList<Plane> planes, int round, double[] bearings) {
        Map<Integer, List<Point2D.Double>> forecastLocations = calculateForecastLocations(planes, bearings);
        List<Set<Integer>> conflictGroups = findConflictGroups(forecastLocations);

        for (Set<Integer> group : conflictGroups) {
            if (group.size() > 1) {
                resolveConflict(planes, bearings, group);
            } else {
                adjustBearingToDestination(planes, bearings, group.iterator().next());
            }
        }
    }

    private Map<Integer, List<Point2D.Double>> calculateForecastLocations(ArrayList<Plane> planes, double[] bearings) {
        Map<Integer, List<Point2D.Double>> forecastLocations = new HashMap<>();
        for (int i = 0; i < planes.size(); i++) {
            Plane plane = planes.get(i);
            if (plane.getBearing() >= 0 && plane.getBearing() != -2) {
                List<Point2D.Double> locations = new ArrayList<>();
                Point2D.Double currentLocation = plane.getLocation();
                double arrivalLeftRound = currentLocation.distance(plane.getDestination());
                for (int k = 1; k <= Math.min(arrivalLeftRound, forecastRound); k++) {
                    locations.add(calculateNextLocation(currentLocation, bearings[i], k));
                }
                forecastLocations.put(i, locations);
            }
        }
        return forecastLocations;
    }

    private List<Set<Integer>> findConflictGroups(Map<Integer, List<Point2D.Double>> forecastLocations) {
        List<Set<Integer>> conflictGroups = new ArrayList<>();
        for (int i : forecastLocations.keySet()) {
            boolean added = false;
            for (Set<Integer> group : conflictGroups) {
                if (isInConflict(i, forecastLocations, group)) {
                    group.add(i);
                    added = true;
                    break;
                }
            }
            if (!added) {
                Set<Integer> newGroup = new HashSet<>();
                newGroup.add(i);
                conflictGroups.add(newGroup);
            }
        }
        return conflictGroups;
    }

    private boolean isInConflict(int planeId, Map<Integer, List<Point2D.Double>> forecastLocations, Set<Integer> group) {
        List<Point2D.Double> locations = forecastLocations.get(planeId);
        for (int otherPlaneId : group) {
            List<Point2D.Double> otherLocations = forecastLocations.get(otherPlaneId);
            for (int i = 0; i < Math.min(locations.size(), otherLocations.size()); i++) {
                if (locations.get(i).distance(otherLocations.get(i)) < conflictDistance) {
                    return true;
                }
            }
        }
        return false;
    }

    private void resolveConflict(ArrayList<Plane> planes, double[] bearings, Set<Integer> group) {
        if (group.size() == 2) {
            handleTwoPlaneConflict(planes, bearings, group);
        } else {
            handleMultiPlaneConflict(planes, bearings, group);
        }
    }

    private void handleTwoPlaneConflict(ArrayList<Plane> planes, double[] bearings, Set<Integer> group) {
        Iterator<Integer> iter = group.iterator();
        int planeId1 = iter.next();
        int planeId2 = iter.next();

        Plane plane1 = planes.get(planeId1);
        Plane plane2 = planes.get(planeId2);

        double distanceToDestination1 = plane1.getLocation().distance(plane1.getDestination());
        double distanceToDestination2 = plane2.getLocation().distance(plane2.getDestination());

        int planeToAdjust = (distanceToDestination1 > distanceToDestination2) ? planeId1 : planeId2;

        double distance = Math.max(distanceToDestination1, distanceToDestination2);
        double angle = distance > 10 ? 9.9 : 3;
        bearings[planeToAdjust] = (bearings[planeToAdjust] + angle + 360) % 360;
    }

    private void handleMultiPlaneConflict(ArrayList<Plane> planes, double[] bearings, Set<Integer> group) {
        double minDistance = Double.MAX_VALUE;
        int planeId1 = -1, planeId2 = -1;

        for (int id1 : group) {
            for (int id2 : group) {
                if (id1 != id2) {
                    double distance = planes.get(id1).getLocation().distance(planes.get(id2).getLocation());
                    if (distance < minDistance) {
                        minDistance = distance;
                        planeId1 = id1;
                        planeId2 = id2;
                    }
                }
            }
        }

        if (planeId1 != -1 && planeId2 != -1) {
            Plane plane1 = planes.get(planeId1);
            Plane plane2 = planes.get(planeId2);

            double distanceToDestination1 = plane1.getLocation().distance(plane1.getDestination());
            double distanceToDestination2 = plane2.getLocation().distance(plane2.getDestination());

            int planeToAdjust = (distanceToDestination1 > distanceToDestination2) ? planeId1 : planeId2;
            bearings[planeToAdjust] = (bearings[planeToAdjust] + 9.9 + 360) % 360;
        }
    }

    private void adjustBearingToDestination(ArrayList<Plane> planes, double[] bearings, int planeId) {
        Plane plane = planes.get(planeId);
        double targetBearing = calculateBearing(plane.getLocation(), plane.getDestination());
        double currentBearing = bearings[planeId];

        if (Math.abs(currentBearing - targetBearing) > 9) {
            double adjustment = 9;
            if (currentBearing > targetBearing) {
                adjustment = (currentBearing - targetBearing > 180) ? adjustment : -adjustment;
            } else {
                adjustment = (targetBearing - currentBearing > 180) ? -adjustment : adjustment;
            }
            bearings[planeId] = (currentBearing + adjustment + 360) % 360;
        } else {
            bearings[planeId] = targetBearing;
        }
    }

    private void handleArrivals(ArrayList<Plane> planes, double[] bearings) {
        for (int i = 0; i < planes.size(); i++) {
            if (planes.get(i).getLocation().distance(planes.get(i).getDestination()) <= 0.5) {
                bearings[i] = -2;
                planeArrivalRound.remove(planes.get(i).getDestination().toString());
            }
        }
    }

    private Point2D.Double calculateNextLocation(Point2D.Double currentLocation, double bearing, int round) {
        double x = currentLocation.x + Math.cos(Math.toRadians(bearing - 90)) * round;
        double y = currentLocation.y + Math.sin(Math.toRadians(bearing - 90)) * round;
        return new Point2D.Double(x, y);
    }
}