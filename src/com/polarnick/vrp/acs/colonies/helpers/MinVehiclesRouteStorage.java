package com.polarnick.vrp.acs.colonies.helpers;

import com.polarnick.vrp.model.Route;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Polyarnyi Nickolay, PolarNick239
 */
public class MinVehiclesRouteStorage implements BestRouteStorage {
    private final Map<Integer, Route> bestRoutes;
    private final List<Callback> vehiclesImprovedCallbacks;
    private final List<Callback> distanceImprovedCallbacks;

    public MinVehiclesRouteStorage() {
        this(null);
    }

    public MinVehiclesRouteStorage(Route initialSolution) {
        this.bestRoutes = new HashMap<>();
        this.vehiclesImprovedCallbacks = new ArrayList<>();
        this.distanceImprovedCallbacks = new ArrayList<>();
        if (initialSolution != null) {
            this.suggestBestRoute(initialSolution);
        }
    }

    public void addVehiclesImprovedCallback(Callback vehiclesImproved) {
        this.vehiclesImprovedCallbacks.add(vehiclesImproved);
    }

    public void addDistanceImprovedCallback(Callback distanceImproved) {
        this.distanceImprovedCallbacks.add(distanceImproved);
    }

    @Override
    public Route getBestRoute(int vehiclesNumber) {
        return bestRoutes.get(vehiclesNumber);
    }

    @Override
    public int getMinVehiclesNumber() {
        int minVehiclesNumber = -1;
        for (int i: this.bestRoutes.keySet()) {
            if (minVehiclesNumber == -1 || i < minVehiclesNumber) {
                minVehiclesNumber = i;
            }
        }
        return minVehiclesNumber;
    }

    @Override
    public boolean suggestBestRoute(Route route) {
        int vehiclesNumber = route.getVehiclesNumber();
        Route bestRoute = this.bestRoutes.get(vehiclesNumber);
        if (bestRoute == null) {
            for (Callback callback: this.vehiclesImprovedCallbacks) {
                callback.callback(bestRoute, route);
            }
            if (bestRoute == null || route.getResidual() < bestRoute.getResidual()) {
                for (Callback callback: this.distanceImprovedCallbacks) {
                    callback.callback(bestRoute, route);
                }
            }
            this.bestRoutes.put(vehiclesNumber, route);
            return true;
        } else if (route.getResidual() < bestRoute.getResidual()) {
            for (Callback callback: this.distanceImprovedCallbacks) {
                callback.callback(bestRoute, route);
            }
            this.bestRoutes.put(vehiclesNumber, route);
            return true;
        } else {
            return false;
        }
    }

    public static interface Callback {

        public void callback(Route oldRoute, Route route);

    }
}
