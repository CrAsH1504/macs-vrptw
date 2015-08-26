package com.polarnick.vrp.acs.colonies;

import com.polarnick.vrp.acs.colonies.helpers.Params;
import com.polarnick.vrp.acs.model.AntColonyProblem;
import com.polarnick.vrp.acs.model.AntColonyState;
import com.polarnick.vrp.model.Route;
import com.polarnick.vrp.model.Stop;
import com.polarnick.vrp.utils.Stoppable;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author Polyarnyi Nickolay, PolarNick239
 */
public abstract class AbstractACS implements Runnable, Stoppable {

    protected final AntColonyProblem problem;
    protected final AntColonyState state;
    protected final int antsNumber;
    protected final String colonyName;
    protected final long seed;
    protected final Random random;
    protected final boolean withLocalSearch;
    protected final Params params;

    private volatile boolean stopped;
    private final ExecutorService executor;
    protected final Logger logger;

    public AbstractACS(AntColonyProblem problem, int antsNumber, String colonyName, long seed, boolean withLocalSearch, Params params) {
        this.problem = problem;
        this.state = new AntColonyState(this.problem);
        this.antsNumber = antsNumber;
        this.colonyName = colonyName;
        this.seed = seed;
        this.random = new Random(seed);
        this.withLocalSearch = withLocalSearch;
        this.params = params;
        this.executor = Executors.newFixedThreadPool(this.antsNumber);
        this.stopped = false;
        this.logger = Logger.getLogger(colonyName);
    }

    public boolean stop() {
        boolean alreadyStopped = this.stopped;
        this.stopped = true;
        logger.info("Stopping ACS!");
        return !alreadyStopped;
    }

    public boolean isStopped() {
        return this.stopped;
    }

    public List<Route> runAnts() {
        List<Ant> ants = new ArrayList<>(this.antsNumber);
        for (int id = 0; id < this.antsNumber; id++) {
            long antSeed = this.random.nextLong();
            Ant ant = new Ant(id, antSeed);
            ants.add(ant);
        }
//        logger.info(antsNumber + " ants started...");
        try {
            List<Future<Route>> results = this.executor.invokeAll(ants);
            for (Future<Route> future: results) {
                future.get();
            }
        } catch (InterruptedException e) {
            e.printStackTrace(); // TODO: handle it correctly
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        List<Route> routes = new ArrayList<>(this.antsNumber);
        routes.addAll(ants.stream().map(Ant::getRoute).collect(Collectors.toList()));
        return routes;
    }

    protected int getIgnoresCount(int nodeIndex) {
        return 0;
    }

    protected void updatePheromone(Route route) {
        int prevI = -1;
        for (int curI: route) {
            if (prevI == -1) {
                prevI = curI;
                continue;
            }

            state.pheromone[prevI][curI] = (1 - params.pheromoneFading) * state.pheromone[prevI][curI] + params.pheromoneFading / route.getResidual();
        }
    }

    protected static LinkedList<Integer> calcCustomersToInsertByDemand(List<Integer> nodes, AntColonyProblem problem) {
        boolean[] visited = new boolean[problem.n];
        int customersLeft = problem.customersNumber;
        for (int i: nodes) {
            assert !visited[i];
            visited[i] = true;
            if (!problem.stops[i].isDepot()) {
                customersLeft -= 1;
            }
        }
        Integer[] customersToVisit = new Integer[customersLeft];
        int nextI = 0;
        for (int i = 0; i < problem.n; i++) {
            if (!problem.stops[i].isDepot() && !visited[i]) {
                customersToVisit[nextI] = i;
                nextI += 1;
            }
        }
        Arrays.sort(customersToVisit, (i0, i1) -> -(problem.stops[((int) i0)].demand - problem.stops[((int) i1)].demand));
        return new LinkedList<>(Arrays.asList(customersToVisit));
    }

    protected static List<Integer> insertionProcedure(List<Integer> nodes, AntColonyProblem problem) {
        LinkedList<Integer> customersToInsert = new LinkedList<>(calcCustomersToInsertByDemand(nodes, problem));
        List<Integer> newNodes = new ArrayList<>(nodes.size() + customersToInsert.size());
        if (nodes.size() == 0) {
            return newNodes;
        }

        int[] maxT = new int[nodes.size()];
        int[] reqiuredDemand = new int[nodes.size()];
        {
            Stop last = problem.stops[nodes.get(nodes.size() - 1)];
            assert last.isDepot();
            maxT[nodes.size() - 1] = last.toT;
            reqiuredDemand[nodes.size() - 1] = 0;
        }
        for (int nextI = nodes.size() - 1; nextI > 0; nextI--) {
            int prevI = nextI - 1;
            Stop prev = problem.stops[nodes.get(prevI)];
            Stop next = problem.stops[nodes.get(nextI)];
            if (prev.isDepot()) {
                maxT[prevI] = prev.toT;
                reqiuredDemand[prevI] = 0;
            } else {
                maxT[prevI] = Math.min(maxT[nextI] - problem.getTimeDistance(nodes.get(prevI), nodes.get(nextI)) - prev.delayT, prev.toT);
                reqiuredDemand[prevI] = reqiuredDemand[nextI] + prev.demand;
            }
        }

        int minTAfterCur = problem.stops[nodes.get(0)].fromT + problem.stops[nodes.get(0)].delayT;
        int curLoad = problem.vehicleCapacity;
        for (int i = 0; i < nodes.size() - 1; i++) {
            int curI = nodes.get(i);
            int nextI = nodes.get(i + 1);
            int maxTNext = maxT[i + 1];
            int requiredDemandNext = reqiuredDemand[i + 1];
            newNodes.add(curI);

            boolean inserted = false;
            do {
                Iterator<Integer> candidateIter = customersToInsert.iterator();
                inserted = false;
                while (candidateIter.hasNext()) {
                    int candidateI = candidateIter.next();
                    Stop candidate = problem.stops[candidateI];

                    if (curLoad - candidate.demand < requiredDemandNext || candidate.isDepot()) {
                        continue;
                    }

                    int minTCandidateStart = Math.max(minTAfterCur + problem.getTimeDistance(curI, candidateI), candidate.fromT);
                    if (minTCandidateStart > candidate.toT) {
                        continue;
                    }

                    int minTAfterCandidate = minTCandidateStart + candidate.delayT;
                    int minTNextStart = minTAfterCandidate + problem.getTimeDistance(candidateI, nextI);
                    if (minTNextStart > maxTNext) {
                        continue;
                    }

                    inserted = true;
                    candidateIter.remove();
                    newNodes.add(candidateI);

                    curI = candidateI;
                    curLoad -= candidate.demand;
                    minTAfterCur = minTAfterCandidate;
                    break;
                }
            } while(inserted);

            Stop next = problem.stops[nextI];
            curLoad -= next.demand;
            minTAfterCur = Math.max(minTAfterCur + problem.getTimeDistance(curI, nextI), next.fromT);
            assert minTAfterCur <= next.toT;
            assert minTAfterCur <= maxTNext;

            minTAfterCur += next.delayT;

            if (next.isDepot()) {
                curLoad = problem.vehicleCapacity;
                minTAfterCur = next.fromT + next.demand;
            }
        }
        newNodes.add(nodes.get(nodes.size() - 1));
        return newNodes;
    }

    protected static List<Integer> localSearchProcedure(List<Integer> nodes, AntColonyProblem problem) {
        // TODO: implement
        return nodes;
    }

    private class Ant implements Runnable, Callable<Route> {

        private final int id;
        private final Random random;
        private Route resultRoute;
        private final Logger logger;

        public Ant(int id, long seed) {
            this.id = id;
            this.random = new Random(seed);
            this.resultRoute = null;
            this.logger = Logger.getLogger("Ant #" + id);
        }

        public Route getRoute() {
            return this.resultRoute;
        }

        @Override
        public void run() {
            try {
                this.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Route call() throws Exception {
            List<Integer> nodes = new ArrayList<>(problem.n);
            boolean[] visited = new boolean[problem.n];

            int startDepot = this.random.nextInt(problem.vehiclesNumber);
            nodes.add(startDepot);
            visited[startDepot] = true;

            int curNode = startDepot;
            int curTime = problem.stops[curNode].fromT + problem.stops[curNode].delayT;
            int load = problem.vehicleCapacity;
            int vehiclesLeft = problem.vehiclesNumber - 1;
            while (true) {
                List<Integer> candidates = new ArrayList<>(problem.n);
                List<Double> values = new ArrayList<>(problem.n);
                double valuesSum = 0;
                double maxValue = 0;
                int maxIndex = -1;
                for (int i = 0; i < problem.n; i++) {
                    Stop candidate = problem.stops[i];
                    if (!visited[i] && load >= candidate.demand && candidate.toT >= curTime + problem.distances[curNode][i]) {
                        int deliveryTime = Math.max(curTime + problem.getTimeDistance(curNode, i), candidate.fromT);
                        if (deliveryTime + candidate.delayT + candidate.calcDistanceTo(problem.depot) > problem.depot.toT) {
                            continue;
                        }
                        int distance = (deliveryTime - curTime) * (candidate.toT - curTime);
                        distance = Math.max(1, distance - getIgnoresCount(i));

                        candidates.add(i);
                        double attractiveness = 1.0 / distance;
                        double value = state.pheromone[curNode][i] * Math.pow(attractiveness, params.heuristicValueImportance);
                        values.add(value);
                        valuesSum += value;
                        if (maxIndex == -1 || value > maxValue) {
                            maxValue = value;
                            maxIndex = values.size() - 1;
                        }
                    }
                }

                if (candidates.isEmpty()) {
                    break;
                }

                int candidateIndex = 0;
                if (this.random.nextDouble() < params.exploitationP) {
                    candidateIndex = maxIndex;
                } else {
                    double p = this.random.nextDouble();
                    while (candidateIndex < values.size() - 1 && p > values.get(candidateIndex) / valuesSum) {
                        candidateIndex += 1;
                        p -= values.get(candidateIndex) / valuesSum;
                    }
                }

                int from = curNode;
                int to = candidates.get(candidateIndex);
                state.pheromone[from][to] = (1 - params.pheromoneFading) * state.pheromone[from][to] + params.pheromoneFading * params.pheromoneBaseValue;

                nodes.add(to);
                visited[to] = true;

                curTime = Math.max(curTime + problem.getTimeDistance(from, to), problem.stops[to].fromT);
                curNode = to;
                load -= problem.stops[to].demand;

                if (problem.stops[to].isDepot()) {
                    curTime = problem.stops[to].fromT + problem.stops[to].delayT;
                    load = problem.vehicleCapacity;
                    if (vehiclesLeft == 0) {
                        break;
                    }
                    vehiclesLeft -= 1;
                }
            }
//            logger.info("Insertion procedure...");
            nodes = insertionProcedure(nodes, problem);
            if (withLocalSearch) {
//                logger.info("Local search procedure...");
                nodes = localSearchProcedure(nodes, problem);
            }
            this.resultRoute = new Route(problem, nodes);
            return this.resultRoute;
        }
    }

}