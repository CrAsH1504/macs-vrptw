package com.polarnick.vrp.acs.colonies.helpers;

import com.polarnick.vrp.model.Route;

/**
 * @author Polyarnyi Nickolay, PolarNick239
 */
public interface BestRouteStorage {

    public Route getBestRoute(int vehiclesNumber);

    public int getMinVehiclesNumber();

    public boolean suggestBestRoute(Route route);
}
