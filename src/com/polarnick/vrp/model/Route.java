package com.polarnick.vrp.model;

import com.polarnick.vrp.acs.model.AntColonyProblem;

import java.util.Iterator;
import java.util.List;

/**
 * @author Polyarnyi Nickolay, PolarNick239
 */
public class Route implements Iterable<Integer> {

    public final AntColonyProblem problem;
    private final List<Integer> nodes;
    private final double residual;
    private final int vehiclesNumber;
    private final int customersNumber;
    private final boolean feasible;

    public Route(AntColonyProblem problem, List<Integer> nodes) {
        this.problem = problem;
        this.nodes = nodes;
        this.residual = this.calculateResidual();
        this.vehiclesNumber = this.calculateVehiclesNumber();
        this.customersNumber = this.calculateCustomersNumber();
        this.feasible = this.calculateIsFeasible();
    }

    public double getResidual() {
        return residual;
    }

    public int getVehiclesNumber() {
        return vehiclesNumber;
    }

    public int getCustomersNumber() {
        return customersNumber;
    }

    public boolean isFeasible() {
        return feasible;
    }

    private double calculateResidual() {
        int curI = nodes.get(nodes.size() - 1);

        int fromT = problem.stops[curI].fromT;
        int toT = problem.stops[curI].toT;
        int load = problem.vehicleCapacity;
        double residual = 0.0;

        for (int i = nodes.size() - 2; i >= 0; i--) {
            int prevI = nodes.get(i);
            Stop prev = this.problem.stops[prevI];
            int distance = this.problem.getTimeDistance(prevI, curI);
            fromT -= distance;
            toT -= distance;
            fromT -= prev.delayT;
            toT -= prev.delayT;
            toT = Math.min(prev.toT, toT);
            fromT = Math.max(prev.fromT, fromT);
            assert prev.fromT <= toT;

            load -= prev.demand;
            assert load >= 0;

            if (!prev.isDepot()) {
                residual += this.problem.distances[prevI][curI] + prev.delayT;
                if (fromT > toT) {
                    residual += toT - fromT;
                    fromT = toT;
                }
            } else {
                fromT = prev.fromT;
                toT = prev.toT;
                load = problem.vehicleCapacity;
            }

            curI = prevI;

        }
        return residual;
    }

    private int calculateVehiclesNumber() {
        int depotsNumber = 0;
        for (int i: nodes) {
            if (problem.stops[i].isDepot()) {
                depotsNumber += 1;
            }
        }
        return depotsNumber -  1;
    }

    private int calculateCustomersNumber() {
        int n = 0;
        for (int i: nodes) {
            if (!problem.stops[i].isDepot()) {
                n += 1;
            }
        }
        return n;
    }

    private boolean calculateIsFeasible() {
        return this.calculateCustomersNumber() == problem.customersNumber;
    }

    @Override
    public Iterator<Integer> iterator() {
        return this.nodes.iterator();
    }

    public String toString() {
        StringBuilder res = new StringBuilder("");
        res.append("Route{");
        res.append("residual=" + residual);
        res.append(" vehicles=" + vehiclesNumber);
        res.append(" nodes=[");
        for (int i: nodes) {
            res.append(problem.stops[i].name + ", ");
        }
        res.append("]}");
        return res.toString();
    }
}
