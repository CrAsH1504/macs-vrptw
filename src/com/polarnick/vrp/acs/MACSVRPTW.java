package com.polarnick.vrp.acs;

import com.polarnick.vrp.acs.colonies.MinDistanceACS;
import com.polarnick.vrp.acs.colonies.MinVehiclesACS;
import com.polarnick.vrp.acs.colonies.helpers.MinVehiclesRouteStorage;
import com.polarnick.vrp.acs.colonies.helpers.Params;
import com.polarnick.vrp.acs.model.AntColonyProblem;
import com.polarnick.vrp.model.Route;
import com.polarnick.vrp.model.Stop;
import com.polarnick.vrp.model.VRProblem;
import com.polarnick.vrp.utils.Stoppable;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.logging.Logger;

/**
 * @author Polyarnyi Nickolay, PolarNick239
 */
public class MACSVRPTW implements Runnable, Stoppable {

    private final VRProblem problem;
    private final Random random;
    private final MinVehiclesRouteStorage globalBestStorage;
    private final Logger logger;
    private volatile boolean stopped;

    private CountDownLatch currentVehiclesNumberDecreasedCDL;

    public MACSVRPTW(VRProblem problem, long seed) {
        this.problem = problem;
        this.random = new Random(seed);
        this.logger = Logger.getLogger("MACS-VRPTW");
        this.stopped = false;
        this.globalBestStorage = new MinVehiclesRouteStorage();
        this.currentVehiclesNumberDecreasedCDL = null;
    }

    protected static int findNearest(VRProblem problem, Stop curStop, int curLoad, int curTime,
                                     Stop[] customers, boolean[] visited, Stop depot) {
        int minI = -1;
        double minDistanceT = -1.0;
        for (int i = 0; i < problem.customersNumber; i++) {
            Stop candidate = problem.customers[i];
            if (!visited[i] && curLoad >= candidate.demand && candidate.toT >= curTime + curStop.calcDistanceTo(candidate)) {
                int deliveryTime = (int) Math.round(Math.ceil(Math.max(curTime + curStop.calcDistanceTo(candidate), candidate.fromT)));
                if (deliveryTime + candidate.delayT + candidate.calcDistanceTo(depot) > depot.toT) {
                    continue;
                }
                int distanceT = deliveryTime - curTime;
                if (minI == -1 || distanceT < minDistanceT) {
                    minI = i;
                    minDistanceT = distanceT;
                }
            }
        }
        return minI;
    }

    protected static Route calculateNearestNeighbourhoodHeuristic(VRProblem problem) {
        List<Deque<Integer>> routes = new ArrayList<>();
        boolean[] visited = new boolean[problem.customersNumber];
        int visitedNumber = 0;

        do {
            Stop curStop = problem.depot;
            int curLoad = problem.vehicleCapacity;
            int curTime = curStop.fromT + curStop.delayT;

            ArrayDeque<Integer> route = new ArrayDeque<>(problem.customersNumber * 2);

            int nextI = findNearest(problem, curStop, curLoad, curTime, problem.customers, visited, problem.depot);
            while (nextI != -1) {
                Stop nextStop = problem.customers[nextI];
                route.add(nextI);
                assert !visited[nextI];
                visited[nextI] = true;
                visitedNumber += 1;

                curLoad -= nextStop.demand;
                curTime = (int) Math.round(Math.ceil(Math.max(curTime + curStop.calcDistanceTo(nextStop), nextStop.fromT) + nextStop.delayT));
                curStop = nextStop;

                nextI = findNearest(problem, curStop, curLoad, curTime, problem.customers, visited, problem.depot);
            }
            if (route.size() > 0) {
                routes.add(route);
            } else {
                assert visitedNumber == problem.customersNumber;
                break;
            }
        } while (true);

        int vehiclesNumber = routes.size();
        List<Integer> singleRoute = new ArrayList<>(vehiclesNumber + 1 + problem.customersNumber);
        int depotIndex = 0;
        for (Deque<Integer> route : routes) {
            singleRoute.add(depotIndex);
            for (int i : route) {
                singleRoute.add(vehiclesNumber + 1 + i);
            }
            depotIndex += 1;
        }
        if (routes.size() != 0) {
            singleRoute.add(depotIndex);
        }

        AntColonyProblem acProblem = new AntColonyProblem(vehiclesNumber, problem.vehicleCapacity, problem.depot, problem.customers);
        return new Route(acProblem, singleRoute);
    }

    public Route getCurrentBestRoute() {
        return this.globalBestStorage.getBestRoute(this.globalBestStorage.getMinVehiclesNumber());
    }

    @Override
    public void run() {
        Route initialSolution = calculateNearestNeighbourhoodHeuristic(problem);
        this.logger.info("Initial solution calculated: " + initialSolution);
        this.globalBestStorage.suggestBestRoute(initialSolution);

        int antsNumber = 10;
        double pheromoneBaseValue = 1.0 / (problem.customers.length * initialSolution.getResidual());
        Params params = new Params(0.9, 1, 0.1, pheromoneBaseValue);
        int iterationNum = 1;
        do {
            int vehiclesNumber = globalBestStorage.getMinVehiclesNumber();
            Route globalBest = globalBestStorage.getBestRoute(vehiclesNumber);
            logger.info("Running iteration #" + iterationNum + " using " + vehiclesNumber + " vehicles...");

            CountDownLatch vehiclesNumberDecreasedCDL = new CountDownLatch(1);
            currentVehiclesNumberDecreasedCDL = vehiclesNumberDecreasedCDL;
            if (isStopped()) {
                return;
            }
            MinVehiclesRouteStorage routeStorage = new MinVehiclesRouteStorage(globalBest);

            routeStorage.addVehiclesImprovedCallback((Route oldRoute, Route newRoute) -> vehiclesNumberDecreasedCDL.countDown());

            routeStorage.addDistanceImprovedCallback((Route oldRoute, Route newRoute) -> globalBestStorage.suggestBestRoute(newRoute));
            routeStorage.addVehiclesImprovedCallback((Route oldRoute, Route newRoute) -> globalBestStorage.suggestBestRoute(newRoute));

            routeStorage.addDistanceImprovedCallback((Route oldRoute, Route newRoute) ->
                    logger.info("Distance improved: " + oldRoute.getResidual() + " -> " + newRoute.getResidual() + " " + newRoute));
            routeStorage.addVehiclesImprovedCallback((Route oldRoute, Route newRoute) ->
                    logger.info("Vehicles number improved: " + oldRoute.getVehiclesNumber() + " (" + oldRoute.getResidual() + ") -> "
                            + newRoute.getVehiclesNumber() + " (" + newRoute.getResidual() + ")" + " " + newRoute));

            AntColonyProblem minimizeDistance = new AntColonyProblem(vehiclesNumber, problem.vehicleCapacity, problem.depot, problem.customers);
            MinDistanceACS acsMinDistance = new MinDistanceACS(minimizeDistance, antsNumber, routeStorage, random.nextLong(), params);

            AntColonyProblem improveVehiclesNumber = new AntColonyProblem(vehiclesNumber - 1, problem.vehicleCapacity, problem.depot, problem.customers);
            MinVehiclesACS acsMinVehicles = new MinVehiclesACS(improveVehiclesNumber, antsNumber, routeStorage, random.nextLong(), params);

            Thread acsDist = new Thread(acsMinDistance);
            Thread acsVeh = new Thread(acsMinVehicles);
            acsDist.start();
            if (vehiclesNumber > 1) {
                acsVeh.start();
            }
            try {
                vehiclesNumberDecreasedCDL.await();
                if (isStopped()) {
                    return;
                }
                this.logger.info("Vehicles number decreased from " + vehiclesNumber + " to " + globalBestStorage.getMinVehiclesNumber() + "!");
                acsMinDistance.stop();
                acsMinVehicles.stop();
                acsDist.join();
                acsVeh.join();
            } catch (InterruptedException e) {
                e.printStackTrace(); // TODO: handle correctly
            }
            iterationNum += 1;
        } while (!this.isStopped());
    }

    @Override
    public boolean isStopped() {
        return stopped;
    }

    @Override
    public boolean stop() {
        boolean alreadyStopped = this.stopped;
        this.stopped = true;
        if (currentVehiclesNumberDecreasedCDL != null) {
            currentVehiclesNumberDecreasedCDL.countDown();
        }
        return !alreadyStopped;
    }
}
