package com.polarnick.vrp.acs.colonies.helpers;

/**
 * @author Polyarnyi Nickolay, PolarNick239
 */
public class Params {

    public final double exploitationP;
    public final double heuristicValueImportance;
    public final double pheromoneFading;
    public final double pheromoneBaseValue;

    public Params(double exploitationP, double heuristicValueImportance, double pheromoneFading, double pheromoneBaseValue) {
        this.exploitationP = exploitationP;
        this.heuristicValueImportance = heuristicValueImportance;
        this.pheromoneFading = pheromoneFading;
        this.pheromoneBaseValue = pheromoneBaseValue;
    }
}
