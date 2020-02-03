/*
 * Licensed to GraphHopper GmbH under one or more contributor
 * license agreements. See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 *
 * GraphHopper GmbH licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.graphhopper.jsprit.examples;

import com.graphhopper.jsprit.analysis.toolbox.GraphStreamViewer;
import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingTransportCosts;
import com.graphhopper.jsprit.core.problem.job.Break;
import com.graphhopper.jsprit.core.problem.job.Job;
import com.graphhopper.jsprit.core.problem.job.Service;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TimeWindow;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl.Builder;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleType;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleTypeImpl;
import com.graphhopper.jsprit.core.reporting.SolutionPrinter;
import com.graphhopper.jsprit.core.util.Coordinate;
import com.graphhopper.jsprit.core.util.Solutions;
import com.graphhopper.jsprit.core.util.UnassignedJobReasonTracker;
import com.graphhopper.jsprit.core.util.VehicleRoutingTransportCostsMatrix;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class MultipleBreaksScenario {

    public static final int WEIGHT_INDEX = 0;

    public Location startLocation;
    public double costPerSecond;
    public double costPerMeter;
    public int fourHoursInSeconds;
    public int eightHoursInSeconds;
    public int twoDaysInSeconds;
    public int weightMax;
    public int oneHourInSeconds = 3600;

    public MultipleBreaksScenario() {
        this.startLocation = new Location.Builder().setId("START").setCoordinate(Coordinate.newInstance(-26.296451, -48.883280)).build();
        this.costPerSecond = 50.0/3600.0; // U$50/3600;
        this.costPerMeter = 0.5/1000; // U$0.5/1000;
        this.weightMax = 1000;
        this.twoDaysInSeconds = 24*3600;
        this.oneHourInSeconds = 3600;
        this.fourHoursInSeconds = 4*3600;
        this.eightHoursInSeconds = 8*3600;

    }

    public static void main(String[] args) {


        VehicleRoutingProblem problem = new MultipleBreaksScenario().createProblem();

        // Get the algorithm
        VehicleRoutingAlgorithm algorithm = Jsprit.createAlgorithm(problem);
        algorithm.setMaxIterations(2048);
        UnassignedJobReasonTracker reasonTracker = new UnassignedJobReasonTracker();
        algorithm.addListener(reasonTracker);

        // And search a solution
        Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();


        // Get the best
        VehicleRoutingProblemSolution bestSolution = Solutions.bestOf(solutions);


        SolutionPrinter.print(problem, bestSolution, SolutionPrinter.Print.VERBOSE);
        for(Job service : bestSolution.getUnassignedJobs()) {
            System.out.println(service.getId() + ": " + reasonTracker.getMostLikelyReason(service.getId()));
        }

        //new GraphStreamViewer(problem, bestSolution).labelWith(GraphStreamViewer.Label.ID).setRenderDelay(200).display();
    }

    public VehicleRoutingProblem createProblem() {

        List<Service> services = this.createServices();
        VehicleImpl vehicle = this.createVehicle();
        VehicleRoutingTransportCosts timeDistanceMatrix = this.createCostMatrix();

        VehicleRoutingProblem.Builder vrpBuilder = VehicleRoutingProblem.Builder.newInstance();
        vrpBuilder.setFleetSize(VehicleRoutingProblem.FleetSize.FINITE);
        vrpBuilder.addVehicle(vehicle);
        vrpBuilder.addAllJobs(services);
        vrpBuilder.setRoutingCost(timeDistanceMatrix);

        return vrpBuilder.build();
    }


    public VehicleType createVehicleType() {
        VehicleTypeImpl.Builder vehicleTypeBuilder = VehicleTypeImpl.Builder.newInstance("vehicleType")
            .addCapacityDimension(WEIGHT_INDEX, weightMax)
            .setCostPerWaitingTime(costPerSecond)
            .setCostPerDistance(costPerMeter);
        return vehicleTypeBuilder.build();

    }

    public Break createBreak() {
        int tolerance = 30 * 60; // 30 min
        int iniLunch = 12 * oneHourInSeconds;

        return (Break) Break.Builder.newInstance("break-lunch")
            .setTimeWindow(TimeWindow.newInstance(iniLunch - tolerance , iniLunch + tolerance))
            .setServiceTime(oneHourInSeconds).build();

    }

    public VehicleImpl createVehicle() {
        Builder vehicleBuilder = Builder.newInstance("vehicle");
        vehicleBuilder.setStartLocation(this.startLocation);
        vehicleBuilder.setType(this.createVehicleType());
        vehicleBuilder.setEarliestStart(eightHoursInSeconds);
        vehicleBuilder.setLatestArrival(twoDaysInSeconds);
        vehicleBuilder.setBreak(createBreak());

        return vehicleBuilder.build();
    }

    public List<Service> createServices() {

        List<Service> servicesList = new ArrayList<>();

        Service service1 = Service.Builder.newInstance("mall")
            .setLocation(new Location.Builder().setId("mall").setCoordinate(Coordinate.newInstance(-26.253478, -48.852200)).build())
            .addSizeDimension(WEIGHT_INDEX, 100)
            .addTimeWindow(0,this.twoDaysInSeconds)
            .setServiceTime(this.fourHoursInSeconds)
            .build();
        servicesList.add(service1);

        Service service2 = Service.Builder.newInstance("airport")
            .setLocation(new Location.Builder().setId("airport").setCoordinate(Coordinate.newInstance(-26.224513, -48.801806)).build())
            .addSizeDimension(WEIGHT_INDEX, 100)
            .addTimeWindow(0,this.twoDaysInSeconds)
            .setServiceTime(this.fourHoursInSeconds)
            .build();
        servicesList.add(service2);

        Service service3 = Service.Builder.newInstance("downtown")
            .setLocation(new Location.Builder().setId("downtown").setCoordinate(Coordinate.newInstance(-26.300609, -48.846049)).build())
            .addSizeDimension(WEIGHT_INDEX, 100)
            .addTimeWindow(0,this.twoDaysInSeconds)
            .setServiceTime(this.fourHoursInSeconds)
            .build();
        servicesList.add(service3);

        Service service4 = Service.Builder.newInstance("south")
            .setLocation(new Location.Builder().setId("south").setCoordinate(Coordinate.newInstance(-26.383166, -48.822479)).build())
            .addSizeDimension(WEIGHT_INDEX, 100)
            .addTimeWindow(0,this.twoDaysInSeconds)
            .setServiceTime(this.fourHoursInSeconds)
            .build();
        servicesList.add(service4);

        return servicesList;
    }

    public VehicleRoutingTransportCosts createCostMatrix() {
        // Data obtained via Graphhopper Matrix API (https://graphhopper.com/api/1/examples/#matrix)

        VehicleRoutingTransportCostsMatrix.Builder costMatrixBuilder = VehicleRoutingTransportCostsMatrix.Builder.newInstance(true);

        // TRANSPORT DISTANCE
        costMatrixBuilder.addTransportDistance("START", "START", 0);
        costMatrixBuilder.addTransportDistance("START", "mall", 8657);
        costMatrixBuilder.addTransportDistance("START", "airport", 14932);
        costMatrixBuilder.addTransportDistance("START", "downtown", 5441);
        costMatrixBuilder.addTransportDistance("START", "south", 15206);

        costMatrixBuilder.addTransportDistance("mall", "START", 8657);
        costMatrixBuilder.addTransportDistance("mall", "mall", 0);
        costMatrixBuilder.addTransportDistance("mall", "airport", 6664);
        costMatrixBuilder.addTransportDistance("mall", "downtown", 5749);
        costMatrixBuilder.addTransportDistance("mall", "south", 16045);

        costMatrixBuilder.addTransportDistance("airport", "START", 14932);
        costMatrixBuilder.addTransportDistance("airport", "mall", 6377);
        costMatrixBuilder.addTransportDistance("airport", "airport", 0);
        costMatrixBuilder.addTransportDistance("airport", "downtown", 12024);
        costMatrixBuilder.addTransportDistance("airport", "south", 22320);

        costMatrixBuilder.addTransportDistance("downtown", "START", 5441);
        costMatrixBuilder.addTransportDistance("downtown", "mall", 6392);
        costMatrixBuilder.addTransportDistance("downtown", "airport", 11358);
        costMatrixBuilder.addTransportDistance("downtown", "downtown", 0);
        costMatrixBuilder.addTransportDistance("downtown", "south", 10622);

        costMatrixBuilder.addTransportDistance("south", "START", 15206);
        costMatrixBuilder.addTransportDistance("south", "mall", 16605);
        costMatrixBuilder.addTransportDistance("south", "airport", 22296);
        costMatrixBuilder.addTransportDistance("south", "downtown", 10646);
        costMatrixBuilder.addTransportDistance("south", "south", 0);

        // TRANSPORT TIME
        costMatrixBuilder.addTransportTime("START", "START", 0);
        costMatrixBuilder.addTransportTime("START", "mall", 817);
        costMatrixBuilder.addTransportTime("START", "airport", 1264);
        costMatrixBuilder.addTransportTime("START", "downtown", 564);
        costMatrixBuilder.addTransportTime("START", "south", 811);

        costMatrixBuilder.addTransportTime("mall", "START", 817);
        costMatrixBuilder.addTransportTime("mall", "mall", 0);
        costMatrixBuilder.addTransportTime("mall", "airport", 468);
        costMatrixBuilder.addTransportTime("mall", "downtown", 513);
        costMatrixBuilder.addTransportTime("mall", "south", 1412);

        costMatrixBuilder.addTransportTime("airport", "START", 1264);
        costMatrixBuilder.addTransportTime("airport", "mall", 457);
        costMatrixBuilder.addTransportTime("airport", "airport", 0);
        costMatrixBuilder.addTransportTime("airport", "downtown", 960);
        costMatrixBuilder.addTransportTime("airport", "south", 1859);

        costMatrixBuilder.addTransportTime("downtown", "START", 564);
        costMatrixBuilder.addTransportTime("downtown", "mall", 600);
        costMatrixBuilder.addTransportTime("downtown", "airport", 979);
        costMatrixBuilder.addTransportTime("downtown", "downtown", 0);
        costMatrixBuilder.addTransportTime("downtown", "south", 947);

        costMatrixBuilder.addTransportTime("south", "START", 811);
        costMatrixBuilder.addTransportTime("south", "mall", 1463);
        costMatrixBuilder.addTransportTime("south", "airport", 1841);
        costMatrixBuilder.addTransportTime("south", "downtown", 946);
        costMatrixBuilder.addTransportTime("south", "south", 0);

        return costMatrixBuilder.build();
    }




}
