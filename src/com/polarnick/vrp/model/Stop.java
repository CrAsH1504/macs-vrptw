package com.polarnick.vrp.model;

/**
 * @author Polyarnyi Nickolay, PolarNick239
 */
public class Stop {

    public final String name;

    public final double x;
    public final double y;

    public final int demand;

    public final int fromT;
    public final int toT;
    public final int delayT;

    public Stop(String name, double x, double y, int demand, int fromT, int toT, int delayT) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.demand = demand;
        this.fromT = fromT;
        this.toT = toT;
        this.delayT = delayT;
    }

    public boolean isDepot() {
        return this.demand == 0;
    }

    public double calcDistanceTo(Stop that) {
        double dx = this.x - that.x;
        double dy = this.y - that.y;;
        return Math.sqrt(dx*dx + dy*dy);
    }
}
