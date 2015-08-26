package com.polarnick.vrp.model;

import com.polarnick.vrp.utils.Commons;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @author Polyarnyi Nickolay, PolarNick239
 */
public class VRProblem {

    public final Stop depot;
    public final Stop[] customers;
    public final int customersNumber;
    public final int vehicleCapacity;

    public VRProblem(Stop[] stops, int vehicleCapacity) {
        this.customers = new Stop[stops.length - 1];
        this.customersNumber = stops.length - 1;
        this.vehicleCapacity = vehicleCapacity;
        Stop depot = null;

        int i = 0;
        for (Stop stop : stops) {
            if (stop.isDepot()) {
                assert depot == null;
                depot = stop;
            } else {
                this.customers[i] = stop;
                i += 1;
            }
        }

        assert depot != null;
        this.depot = depot;
    }

    public static VRProblem readFromString(String input) {
        return readFromString(input, -1);
    }

    public static VRProblem readFromString(String input, int countLimit) {
        int vehiclesCapacity = -1;
        List<Stop> stops = new ArrayList<>();

        StringTokenizer tok = new StringTokenizer(input);
        while (tok.hasMoreTokens()) {
            String token = tok.nextToken();
            if (token.equals("capacity")) {
                vehiclesCapacity = Integer.parseInt(tok.nextToken());
            } else {
                int x = Integer.parseInt(token);
                int y = Integer.parseInt(tok.nextToken());
                int demand = Integer.parseInt(tok.nextToken());
                int from = Integer.parseInt(tok.nextToken());
                int to = Integer.parseInt(tok.nextToken());
                int delay = Integer.parseInt(tok.nextToken());
                String name = tok.nextToken();
                Stop stop = new Stop(name, x, y, demand, from, to, delay);
                if (countLimit == -1 || stops.size() < countLimit) {
                    stops.add(stop);
                }
            }
        }
        assert vehiclesCapacity != -1;
        Stop[] stopsA = stops.toArray(new Stop[stops.size()]);
        return new VRProblem(stopsA, vehiclesCapacity);
    }

    public static VRProblem readFromFile(String path) throws IOException {
        int vehiclesCapacity = -1;
        List<Stop> stops = new ArrayList<>();

        BufferedReader in = new BufferedReader(new FileReader(path));
        String s = in.readLine();
        while (s != null) {
            StringTokenizer tok = new StringTokenizer(s);
            String token = tok.nextToken();
            if (token.equals("capacity")) {
                vehiclesCapacity = Integer.parseInt(tok.nextToken());
            } else {
                int x = Integer.parseInt(token);
                int y = Integer.parseInt(tok.nextToken());
                int demand = Integer.parseInt(tok.nextToken());
                int from = Integer.parseInt(tok.nextToken());
                int to = Integer.parseInt(tok.nextToken());
                int delay = Integer.parseInt(tok.nextToken());
                String name = tok.nextToken();
                Stop stop = new Stop(name, x, y, demand, from, to, delay);
                stops.add(stop);
            }
            s = in.readLine();
        }
        assert vehiclesCapacity != -1;
        Stop[] stopsA = stops.toArray(new Stop[stops.size()]);
        return new VRProblem(stopsA, vehiclesCapacity);
    }
}