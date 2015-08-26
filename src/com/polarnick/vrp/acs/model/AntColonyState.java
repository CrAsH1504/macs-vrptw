package com.polarnick.vrp.acs.model;

/**
 * @author Polyarnyi Nickolay, PolarNick239
 */
public class AntColonyState {

    public final AntColonyProblem problem;
    public final double[][] pheromone;

    public AntColonyState(AntColonyProblem problem) {
        this.problem = problem;
        this.pheromone = new double[problem.n][problem.n];
    }

}
