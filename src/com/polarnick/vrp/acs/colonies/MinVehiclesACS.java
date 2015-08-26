package com.polarnick.vrp.acs.colonies;

import com.polarnick.vrp.acs.colonies.helpers.BestRouteStorage;
import com.polarnick.vrp.acs.colonies.helpers.Params;
import com.polarnick.vrp.acs.model.AntColonyProblem;
import com.polarnick.vrp.model.Route;
import com.polarnick.vrp.model.Stop;
import com.polarnick.vrp.utils.Commons;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * @author Polyarnyi Nickolay, PolarNick239
 */
public class MinVehiclesACS extends AbstractACS {

    private final BestRouteStorage bestRouteStorage;
    private final int[] skippedSolutions;

    public MinVehiclesACS(AntColonyProblem problem, int antsNumber, BestRouteStorage bestRouteStorage, long seed, Params params) {
        super(problem, antsNumber, "ACS-VEH", seed, false, params);
        this.bestRouteStorage = bestRouteStorage;
        this.skippedSolutions = new int[this.problem.n];
    }

    private void updateSkippedNodes(List<Route> routes) {
        for (Route route: routes) {
            boolean[] was = new boolean[this.problem.n];
            for (int node: route) {
                was[node] = true;
            }
            for (int i = 0; i < this.problem.n; i++) {
                if (!was[i]) {
                    this.skippedSolutions[i] += 1;
                }
            }
        }
    }

    protected int getIgnoresCount(int nodeIndex) {
        return this.skippedSolutions[nodeIndex];
    }

    private Route calculateNearestNeighbourhoodHeuristic() {
        List<Integer> nodes = new ArrayList<>(problem.n);
        boolean[] visited = new boolean[problem.n];

        int startNode = random.nextInt(problem.vehiclesNumber);
        visited[startNode] = true;
        nodes.add(startNode);

        int curNode = startNode;
        int curTime = problem.stops[curNode].fromT + problem.stops[curNode].delayT;
        int load = problem.vehicleCapacity;
        while (true) {
            int minI = -1;
            double minDistanceT = -1.0;

            for (int i = problem.n - 1; i >= 0; i--) {
                Stop candidate = problem.stops[i];
                if (candidate.isDepot() && minI != -1) {
                    continue;
                }
                if (!visited[i] && load >= candidate.demand && candidate.toT >= curTime + problem.getTimeDistance(curNode, i)) {
                    int afterCandidateT = Math.max(curTime + problem.getTimeDistance(curNode, i), candidate.fromT) + candidate.delayT;
                    if (afterCandidateT + problem.getTimeDistance(i, 0) > problem.depot.toT) {
                        continue;
                    }
                    double distanceT = Math.max(curTime + problem.distances[curNode][i], candidate.fromT) - curTime;
                    if (minI == -1 || distanceT < minDistanceT) {
                        minI = i;
                        minDistanceT = distanceT;
                    }
                }
            }

            if (minI == -1) {
                break;
            }

            visited[minI] = true;
            nodes.add(minI);

            Stop target = problem.stops[minI];
            curNode = minI;
            curTime = Math.max(curTime + problem.getTimeDistance(curNode, minI), target.fromT) + target.delayT;
            load -= target.demand;
            if (target.isDepot()) {
                curTime = target.fromT + target.delayT;
                load = problem.vehicleCapacity;
            }
        }
        return new Route(problem, nodes);
    }

    private void updatePheromoneWithDifferentVehiclesNumber(Route route) {
        int prevI = -1;
        int vehs = route.getVehiclesNumber();
        for (int curI: route) {
            if (curI >= vehs) {
                curI = problem.vehiclesNumber + (curI - vehs);
            } else {
                if (curI >= problem.vehiclesNumber) {
                    curI = -1;
                }
            }
            if (prevI != -1 && curI != -1) {
                state.pheromone[prevI][curI] = (1 - params.pheromoneFading) * state.pheromone[prevI][curI] + params.pheromoneFading / route.getResidual();
            }
            prevI = curI;
        }
    }

    @Override
    public void run() {
        this.logger.info("Starting ACS...");
        int iterationNumber = 0;
        Route currentSolution = this.calculateNearestNeighbourhoodHeuristic();
        do {
            List<Route> routes = this.runAnts();
            this.updateSkippedNodes(routes);

            Route newBestRoute = Commons.getMaximum(routes, (Route r0, Route r1) -> r0.getCustomersNumber() - r1.getCustomersNumber());

            if (iterationNumber % 1000 == 0) {
                logger.info("Iteration #" + iterationNumber + " (residual: " + newBestRoute.getResidual() + ")");
            }
            iterationNumber += 1;

            if (newBestRoute.getCustomersNumber() > currentSolution.getCustomersNumber()) {
                currentSolution = newBestRoute;
                Arrays.fill(this.skippedSolutions, 0);
                if (currentSolution.isFeasible()) {
                    this.bestRouteStorage.suggestBestRoute(newBestRoute);
                }
            }
            this.updatePheromoneWithDifferentVehiclesNumber(this.bestRouteStorage.getBestRoute(this.bestRouteStorage.getMinVehiclesNumber()));
            this.updatePheromone(currentSolution);
        } while (!this.isStopped());
        this.logger.info("ACS stopped!");
    }

}
