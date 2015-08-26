package com.polarnick.vrp.acs.model;

import com.polarnick.vrp.model.Stop;
import com.polarnick.vrp.model.VRProblem;

/**
 * @author Polyarnyi Nickolay, PolarNick239
 */
public class AntColonyProblem {

    public final int n;
    public final int vehiclesNumber;
    public final int vehicleCapacity;
    public final int customersNumber;

    public final Stop depot;
    public final Stop[] stops;

    public final double[][] distances;

    public AntColonyProblem(int vehiclesNumber, int vehicleCapacity, Stop depot, Stop[] customers) {
        this.n = vehiclesNumber + 1 + customers.length;
        this.vehiclesNumber = vehiclesNumber;
        this.vehicleCapacity = vehicleCapacity;
        this.customersNumber = customers.length;

        this.depot = depot;
        this.stops = new Stop[this.n];
        for (int i = 0; i < vehiclesNumber + 1; i++) {
            this.stops[i] = depot;
        }
        for (int i = 0; i < customers.length; i++) {
            this.stops[vehiclesNumber + 1 + i] = customers[i];
        }

        this.distances = new double[this.n][this.n];
        for (int i = 0; i < this.n; i++) {
            for (int j = 0; j < this.n; j++) {
                double dx = this.stops[i].x - this.stops[j].x;
                double dy = this.stops[i].y - this.stops[j].y;
                this.distances[i][j] = Math.sqrt(dx*dx + dy*dy);
            }
        }
    }

    public int getTimeDistance(int from, int to) {
        return (int) Math.round(Math.ceil(this.distances[from][to]));
    }

    public static AntColonyProblem buildProblem(VRProblem baseProblem, int vehiclesNumber) {
        return new AntColonyProblem(vehiclesNumber, baseProblem.vehicleCapacity, baseProblem.depot, baseProblem.customers);
    }

}
