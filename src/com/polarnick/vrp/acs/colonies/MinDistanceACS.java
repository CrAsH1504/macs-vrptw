package com.polarnick.vrp.acs.colonies;

import com.polarnick.vrp.acs.colonies.helpers.BestRouteStorage;
import com.polarnick.vrp.acs.colonies.helpers.Params;
import com.polarnick.vrp.acs.model.AntColonyProblem;
import com.polarnick.vrp.model.Route;
import com.polarnick.vrp.utils.Commons;

import java.util.List;

/**
 * @author Polyarnyi Nickolay, PolarNick239
 */
public class MinDistanceACS extends AbstractACS {

    private final BestRouteStorage bestRouteStorage;

    public MinDistanceACS(AntColonyProblem problem, int antsNumber, BestRouteStorage bestRouteStorage, long seed, Params params) {
        super(problem, antsNumber, "ACS-MIN", seed, true, params);
        this.bestRouteStorage = bestRouteStorage;
    }

    @Override
    public void run() {
        this.logger.info("Starting ACS...");
        int iterationNumber = 0;
        do {
            List<Route> routes = this.runAnts();
//            logger.info(antsNumber + " ants finished!");
            Route newBestRoute = null;
            for (Route route: routes) {
                if (route.isFeasible() && (newBestRoute == null || route.getResidual() < newBestRoute.getResidual())) {
                    newBestRoute = route;
                }
            }
            if (iterationNumber % 1000 == 0) {
                logger.info("Iteration #" + iterationNumber + " (residual: "+ (newBestRoute == null ? null : newBestRoute.getResidual()) + ")");
            }
            iterationNumber += 1;
            if (newBestRoute != null && newBestRoute.getResidual() < this.bestRouteStorage.getBestRoute(problem.vehiclesNumber).getResidual()) {
                this.bestRouteStorage.suggestBestRoute(newBestRoute);
            }
            this.updatePheromone(this.bestRouteStorage.getBestRoute(problem.vehiclesNumber));
        } while (!this.isStopped());
        this.logger.info("ACS stopped!");
    }

}
