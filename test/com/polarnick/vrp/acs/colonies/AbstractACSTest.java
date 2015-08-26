package com.polarnick.vrp.acs.colonies;

import com.polarnick.vrp.acs.model.AntColonyProblem;
import com.polarnick.vrp.model.Stop;
import com.polarnick.vrp.model.VRProblem;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ExecutorService;

import static org.testng.Assert.*;

public class AbstractACSTest {

    private void testInsertionProcedureCase(int vehicleCapacity, int vehicleNumber,
                                            int[] xyDemandFromToDelay, String[] names,
                                            String[] initialRoute,
                                            String[] expectedRoute) throws Exception {
        Stop[] stops = new Stop[names.length];
        for (int i = 0; 6 * i < xyDemandFromToDelay.length; i++) {
            stops[i] = new Stop(names[i], xyDemandFromToDelay[6 * i], xyDemandFromToDelay[6 * i + 1], xyDemandFromToDelay[6 * i + 2],
                    xyDemandFromToDelay[6 * i + 3], xyDemandFromToDelay[6 * i + 4], xyDemandFromToDelay[6 * i + 5]);
        }
        AntColonyProblem problem = AntColonyProblem.buildProblem(new VRProblem(stops, vehicleCapacity), vehicleNumber);
        boolean[] used = new boolean[problem.n];
        List<Integer> nodes = new ArrayList<>();
        for (String name: initialRoute) {
            boolean found = false;
            for (int i = 0; i < problem.n; i++) {
                if (!used[i] && problem.stops[i].name.equals(name)) {
                    used[i] = true;
                    nodes.add(i);
                    found = true;
                    break;
                }
            }
            assert found;
        }
        List<Integer> foundRoute = AbstractACS.insertionProcedure(nodes, problem);
        List<String> foundNames = new ArrayList<>();
        for (int i: foundRoute) {
            foundNames.add(problem.stops[i].name);
        }
        Assert.assertEquals(foundNames, Arrays.asList(expectedRoute), "Found: " + foundNames + ", expected: " + Arrays.asList(expectedRoute));
    }

    @Test
    public void testInsertionProcedureNothingToInsert() throws Exception {
        testInsertionProcedureCase(2, 1,
                new int[]{
                        // x  y  demand from to  delay
                        0, 0, 0, 0, 17, 0,
                        0, 3, 1000, 5, 5, 2,
                        3, 3, 2, 7, 7, 2,
                        3, 0, 2, 12, 12, 2,
                },
                new String[]{"Depot", "Fat", "A", "B"},

                new String[]{"Depot", "A", "Depot"},
                new String[]{"Depot", "A", "Depot"});
    }

    @Test
    public void testInsertionProcedureOneToInsert() throws Exception {
        testInsertionProcedureCase(4, 1,
                new int[]{
                     // x  y  demand from to  delay
                        0, 0, 0,     0,   17, 0,
                        0, 3, 1000,  5,   5,  2,
                        3, 3, 2,     7,   7,  2,
                        3, 0, 2,     12,  12, 2,
                },
                new String[]{"Depot", "Fat", "A", "B"},

                new String[]{"Depot", "A", "Depot"},
                new String[]{"Depot", "A", "B", "Depot"});
    }

    @Test
    public void testInsertionProcedureTwoToInsert() throws Exception {
        testInsertionProcedureCase(2000, 1,
                new int[]{
                     // x  y  demand from to  delay
                        0, 0, 0,     0,   17, 0,
                        1, 1, 1000,  2,   2,  2,
                        3, 3, 2,     7,   7,  2,
                        3, 0, 2,     12,  12, 2,
                },
                new String[]{"Depot", "Fat", "A", "B"},

                new String[]{"Depot", "A", "Depot"},
                new String[]{"Depot", "Fat", "A", "B", "Depot"});
    }

    @Test
    public void testInsertionProcedureAllToInsert() throws Exception {
        testInsertionProcedureCase(2000, 1,
                new int[]{
                     // x  y  demand from to  delay
                        0, 0, 0,     0,   17, 0,
                        1, 1, 1000,  2,   2,  2,
                        3, 3, 3,     7,   7,  2,
                        3, 0, 2,     12,  12, 2,
                },
                new String[]{"Depot", "Fat", "A", "B"},

                new String[]{"Depot", "Depot"},
                new String[]{"Depot", "Fat", "A", "B", "Depot"});
    }

    @Test
    public void testInsertionProcedureTwoRoutesNothingToInsert() throws Exception {
        testInsertionProcedureCase(2, 2,
                new int[]{
                     // x  y  demand from to  delay
                        0, 0, 0,     0,   21, 0,
                        0, 3, 1000,  5,   5,  2,
                        3, 3, 2,     7,   7,  2,
                        3, 0, 2,     12,  12, 2,
                        0, -4,2,     4,   4,  2,
                        3, -4,2,     8,   8,  2,
                        3, -6,2,     12,  12, 2,
                        0, -6,2,     17,  17, 2,
                },
                new String[]{"Depot", "Fat", "A", "B", "X1", "X2", "X3", "X4"},

                new String[]{"Depot", "A", "Depot", "X3", "Depot"},
                new String[]{"Depot", "A", "Depot", "X3", "Depot"});
    }

    @Test
    public void testInsertionProcedureTwoRoutesOneToInsert() throws Exception {
        testInsertionProcedureCase(3, 2,
                new int[]{
                     // x  y  demand from to  delay
                        0, 0, 0,     0,   25, 0,
                        0, 3, 1000,  5,   5,  2,
                        3, 3, 2,     7,   7,  2,
                        3, 0, 2,     12,  12, 2,
                        0, -4,2,     4,   4,  2,
                        3, -4,2,     8,   8,  2,
                        3, -6,2,     12,  12, 2,
                        0, -6,1,     17,  17, 2,
                },
                new String[]{"Depot", "Fat", "A", "B", "X1", "X2", "X3", "X4"},

                new String[]{"Depot", "A", "Depot", "X3", "Depot"},
                new String[]{"Depot", "A", "Depot", "X3", "X4", "Depot"});
    }


}