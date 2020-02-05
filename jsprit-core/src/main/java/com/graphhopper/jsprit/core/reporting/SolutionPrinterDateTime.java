package com.graphhopper.jsprit.core.reporting;

import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.job.Job;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;
import com.graphhopper.jsprit.core.util.UnassignedJobReasonTracker;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class SolutionPrinterDateTime {

    private static SimpleDateFormat sdf = new SimpleDateFormat();
    private static final PrintWriter SYSTEM_OUT_AS_PRINT_WRITER = new PrintWriter(System.out);


    public static void printVerbose(VehicleRoutingProblem problem, VehicleRoutingProblemSolution solution, Date inicialDay, UnassignedJobReasonTracker reasonTracker) {
        printVerbose(SYSTEM_OUT_AS_PRINT_WRITER, problem, solution, inicialDay, reasonTracker);
        SYSTEM_OUT_AS_PRINT_WRITER.flush();
    }

    private static String dateFormat(Date begin, double time) {
        return sdf.format(new Date(begin.getTime() + Math.round(time * 1000)));

    }
    public static void printVerbose(PrintWriter out, VehicleRoutingProblem problem, VehicleRoutingProblemSolution solution, Date inicialDay, UnassignedJobReasonTracker reasonTracker) {

        String leftAlgin = "| %-7s | %-20s | %-21s | %-15s | %-19s | %-19s | %-15s | %-15s |%n";
        out.format("+----------------------------------------------------------------------------------------------------------------------------------------------------------+%n");
        out.printf("| detailed solution                                                                                                                                        |%n");
        out.format("+---------+----------------------+-----------------------+-----------------+---------------------+---------------------+-----------------+-----------------|%n");
        out.printf("| route   | vehicle              | activity              | job             | arrTime             | endTime             | Transport Cost  | Activity Cost   |%n");

        int routeNu = 1;

        List<VehicleRoute> list = new ArrayList<VehicleRoute>(solution.getRoutes());
        Collections.sort(list , new com.graphhopper.jsprit.core.util.VehicleIndexComparator());

        double transpCost = 0;
        double actCost = 0;

        for (VehicleRoute route : list) {
            out.format("+---------+----------------------+-----------------------+-----------------+---------------------+---------------------+-----------------+-----------------|%n");

            out.format(leftAlgin, routeNu, getVehicleString(route), route.getStart().getName(), "-",
                "undef", dateFormat(inicialDay,route.getStart().getEndTime()),
                Math.round(transpCost),  Math.round(actCost)
               );
            TourActivity prevAct = route.getStart();
            for (TourActivity act : route.getActivities()) {
                String jobId;
                if (act instanceof TourActivity.JobActivity) {
                    jobId = ((TourActivity.JobActivity) act).getJob().getId();
                } else {
                    jobId = "-";
                }
                transpCost = problem.getTransportCosts().getTransportCost(prevAct.getLocation(), act.getLocation(), prevAct.getEndTime(), route.getDriver(),
                    route.getVehicle());
                actCost = problem.getActivityCosts().getActivityCost(act, act.getArrTime(), route.getDriver(), route.getVehicle());

                out.format(leftAlgin, routeNu, getVehicleString(route), act.getName(), jobId,
                    dateFormat(inicialDay,Math.round(act.getArrTime())),
                    dateFormat(inicialDay,Math.round(act.getEndTime())),
                    Math.round(transpCost), Math.round(actCost));
                prevAct = act;
            }
            transpCost = problem.getTransportCosts().getTransportCost(prevAct.getLocation(), route.getEnd().getLocation(), prevAct.getEndTime(),
                route.getDriver(), route.getVehicle());
            actCost = problem.getActivityCosts().getActivityCost(route.getEnd(), route.getEnd().getArrTime(), route.getDriver(), route.getVehicle());
            out.format(leftAlgin, routeNu, getVehicleString(route), route.getEnd().getName(), "-", dateFormat(inicialDay,route.getEnd().getArrTime()), "undef",
                Math.round(transpCost),Math.round(actCost));
            routeNu++;
        }
        out.format("+----------------------------------------------------------------------------------------------------------------------------------------------------------+%n%n");

        if (!solution.getUnassignedJobs().isEmpty()) {
            out.format("+----------------+--------------------------------------------------------+%n");
            out.format("| unassignedJobs | reason                                                 |%n");
            out.format("+----------------+--------------------------------------------------------+%n");
            String unassignedJobAlgin = "| %-14s | %-54s |%n";
            for (Job j : solution.getUnassignedJobs()) {
                String reason = (reasonTracker != null) ? reasonTracker.getMostLikelyReason(j.getId()) : "";
                out.format(unassignedJobAlgin, j.getId(), reason);
            }
            out.format("+----------------+--------------------------------------------------------+%n");
        }
    }

    private static String getVehicleString(VehicleRoute route) {
        return route.getVehicle().getId();
    }
}
