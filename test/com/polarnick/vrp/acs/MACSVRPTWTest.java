package com.polarnick.vrp.acs;

import com.polarnick.vrp.acs.colonies.AbstractACS;
import com.polarnick.vrp.acs.model.AntColonyProblem;
import com.polarnick.vrp.model.Route;
import com.polarnick.vrp.model.Stop;
import com.polarnick.vrp.model.VRProblem;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;

import static org.testng.Assert.*;

public class MACSVRPTWTest {

    private void testNearestNeighbourhoodHeuristic(int vehicleCapacity,
                                                   int[] xyDemandFromToDelay, String[] names,
                                                   String[] expectedRoute) throws Exception {
        Stop[] stops = new Stop[names.length];
        for (int i = 0; 6 * i < xyDemandFromToDelay.length; i++) {
            stops[i] = new Stop(names[i], xyDemandFromToDelay[6 * i], xyDemandFromToDelay[6 * i + 1], xyDemandFromToDelay[6 * i + 2],
                    xyDemandFromToDelay[6 * i + 3], xyDemandFromToDelay[6 * i + 4], xyDemandFromToDelay[6 * i + 5]);
        }
        VRProblem problem = new VRProblem(stops, vehicleCapacity);
        Route route = MACSVRPTW.calculateNearestNeighbourhoodHeuristic(problem);

        List<String> foundNames = new ArrayList<>();
        for (int i: route) {
            foundNames.add(route.problem.stops[i].name);
        }
        Assert.assertEquals(foundNames, Arrays.asList(expectedRoute), "Found: " + foundNames + ", expected: " + Arrays.asList(expectedRoute));
    }

    @Test
    public void testCalculateNearestNeighbourhoodHeuristic() throws Exception {
        testNearestNeighbourhoodHeuristic(2000,
                new int[]{
                        // x  y  demand from to  delay
                        0, 0, 0, 0, 17, 0,
                        1, 1, 1000, 2, 2, 2,
                        3, 3, 3, 7, 7, 2,
                        3, 0, 2, 12, 12, 2,
                },
                new String[]{"Depot", "Fat", "A", "B"},
                new String[]{"Depot", "Fat", "A", "B", "Depot"});
    }

    @Test
    public void testSimpleCase() throws Exception {
        String input =
                "capacity 1000 " +
                        //x   y   demand from  to   delay name
                        "0   0   0      0     28   0     Depot " +
                        "0   3   1000   5     5    2     Fat " +
                        "3   3   2      7     7    2     A " +
                        "3   0   2      12    12   2     B " +
                        "0  -4   2      4     4    2     X1 " +
                        "3  -4   2      9     9    2     X2 " +
                        "3  -6   2      14    14   2     X3 " +
                        "0  -6   1      19    19   2     X4";
        MACSVRPTW solver = new MACSVRPTW(VRProblem.readFromString(input), 239);

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                solver.stop();
            }
        }, 1 * 1000);

        solver.run();
        Route route = solver.getCurrentBestRoute();
        Assert.assertEquals(route.toString(), "Route{residual=36.0 vehicles=3 nodes=[Depot, X1, X2, X3, X4, Depot, Fat, Depot, A, B, Depot, ]}");
    }

    @Test
    public void testSimpleCase2() throws Exception {
        String input =
                "capacity 3 " +
                        //x   y   demand from  to   delay name
                        "0   0   0      0     28   0     Depot " +
                        "0   3   1      3     3    1     A1 " +
                        "2   3   1      6     6    1     A2 " +
                        "4   3   1      9     9    1     A3 " +
                        "1   3   1      4     4    1     B1 " +
                        "3   3   1      7     7    1     B2 ";
        MACSVRPTW solver = new MACSVRPTW(VRProblem.readFromString(input), 239);

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                solver.stop();
            }
        }, 1 * 1000);

        solver.run();
        Route route = solver.getCurrentBestRoute();
        Assert.assertEquals(route.toString(), "Route{residual=20.242640687119284 vehicles=2 nodes=[Depot, A1, A2, A3, Depot, B1, B2, Depot, ]}");
    }

}

