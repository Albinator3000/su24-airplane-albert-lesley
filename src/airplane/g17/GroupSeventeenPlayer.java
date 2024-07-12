package airplane.g17;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.awt.geom.Point2D;
import org.apache.log4j.Logger;
import airplane.sim.Plane;
import airplane.sim.Player;
import java.lang.Math;

public class GroupSeventeenPlayer extends Player {

    private Logger logger = Logger.getLogger(this.getClass());
    private HashMap<Plane, Integer> planeDelays = new HashMap<>();

    @Override
    public String getName() {
        return "Group 17 Player";
    }

    @Override
    public void startNewGame(ArrayList<Plane> planes) {
        logger.info("Starting new game with " + planes.size() + " planes.");
        planeDelays.clear();
        setDelaysForPlanesWithSameDestination(planes);
    }

    @Override
    public double[] updatePlanes(ArrayList<Plane> planes, int round, double[] bearings) {
        logger.info("Updating planes for round " + round);

        for (int i = 0; i < planes.size(); i++) {
            Plane plane = planes.get(i);
            int delay = planeDelays.getOrDefault(plane, 0);

            if (plane.getBearing() == -1 && round >= plane.getDepartureTime() + delay) {
                // Plane is on the ground and ready to take off (considering delay)
                Point2D.Double destination = plane.getDestination();
                double initialBearing = calculateBearing(plane.getLocation(), destination);
                bearings[i] = initialBearing;
            } else if (plane.getBearing() >= 0 && plane.getBearing() != -2) {
                // Plane is in the air
                Point2D.Double currentLocation = plane.getLocation();
                Point2D.Double destination = plane.getDestination();

                double targetBearing = calculateBearing(currentLocation, destination);
                double currentBearing = plane.getBearing();

                // Check for potential collisions with other planes
                for (int j = 0; j < planes.size(); j++) {
                    if (i != j && planes.get(j).getBearing() >= 0 && planes.get(j).getBearing() != -2) {
                        Plane otherPlane = planes.get(j);
                        Point2D.Double otherLocation = otherPlane.getLocation();
                        double distanceToOtherPlane = currentLocation.distance(otherLocation);

                        if (distanceToOtherPlane <= 15) {
                            // Planes are too close, adjust course
                            double bearingToOtherPlane = calculateBearing(currentLocation, otherLocation);
                            double avoidanceBearing = (bearingToOtherPlane + 90) % 360; // Turn 90 degrees relative to other plane
                            targetBearing = avoidanceBearing;
                            break; // Exit the loop after finding a nearby plane
                        }
                    }
                }

                // Adjust bearing towards the target, respecting the maximum change of 10 degrees
                double bearingDifference = targetBearing - currentBearing;
                if (Math.abs(bearingDifference) > 180) {
                    bearingDifference = bearingDifference > 0 ? bearingDifference - 360 : bearingDifference + 360;
                }

                double bearingChange = Math.min(Math.max(bearingDifference, -10), 10);
                double newBearing = (currentBearing + bearingChange + 360) % 360;

                bearings[i] = newBearing;
            }
            // If bearing is -2, the plane has landed, so we don't need to update it
        }

        return bearings;
    }

    @Override
    protected double[] simulateUpdate(ArrayList<Plane> planes, int round, double[] bearings) {
        return updatePlanes(planes, round, bearings);
    }

    private void setDelaysForPlanesWithSameDestination(ArrayList<Plane> planes) {
        Map<Point2D.Double, ArrayList<Plane>> destinationGroups = new HashMap<>();

        // Group planes by destination
        for (Plane plane : planes) {
            Point2D.Double destination = plane.getDestination();
            destinationGroups.putIfAbsent(destination, new ArrayList<>());
            destinationGroups.get(destination).add(plane);
        }

        // Set delays for planes with the same destination
        for (ArrayList<Plane> planeGroup : destinationGroups.values()) {
            if (planeGroup.size() > 1) {
                for (int i = 0; i < planeGroup.size(); i++) {
                    planeDelays.put(planeGroup.get(i), i * 13);
                    logger.info("Set delay of " + (i * 15) + " rounds for plane to destination " + planeGroup.get(i).getDestination());
                }
            }
        }
    }
}