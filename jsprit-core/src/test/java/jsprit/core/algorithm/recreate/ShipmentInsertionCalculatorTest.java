/*******************************************************************************
 * Copyright (c) 2014 Stefan Schroeder.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either 
 * version 3.0 of the License, or (at your option) any later version.
 *  
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public 
 * License along with this library.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contributors:
 *     Stefan Schroeder - initial API and implementation
 ******************************************************************************/
package jsprit.core.algorithm.recreate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Arrays;

import jsprit.core.algorithm.recreate.listener.InsertionListeners;
import jsprit.core.algorithm.state.StateManager;
import jsprit.core.problem.VehicleRoutingProblem;
import jsprit.core.problem.constraint.ConstraintManager;
import jsprit.core.problem.constraint.ConstraintManager.Priority;
import jsprit.core.problem.constraint.HardActivityStateLevelConstraint;
import jsprit.core.problem.constraint.HardRouteStateLevelConstraint;
import jsprit.core.problem.constraint.PickupAndDeliverShipmentLoadActivityLevelConstraint;
import jsprit.core.problem.constraint.ShipmentPickupsFirstConstraint;
import jsprit.core.problem.cost.VehicleRoutingActivityCosts;
import jsprit.core.problem.cost.VehicleRoutingTransportCosts;
import jsprit.core.problem.driver.Driver;
import jsprit.core.problem.driver.DriverImpl;
import jsprit.core.problem.job.Pickup;
import jsprit.core.problem.job.Service;
import jsprit.core.problem.job.Shipment;
import jsprit.core.problem.misc.JobInsertionContext;
import jsprit.core.problem.solution.route.VehicleRoute;
import jsprit.core.problem.solution.route.activity.TourActivity;
import jsprit.core.problem.solution.route.state.RouteAndActivityStateGetter;
import jsprit.core.problem.vehicle.Vehicle;
import jsprit.core.problem.vehicle.VehicleImpl;
import jsprit.core.problem.vehicle.VehicleType;
import jsprit.core.problem.vehicle.VehicleTypeImpl;
import jsprit.core.util.CostFactory;

import org.junit.Before;
import org.junit.Test;


public class ShipmentInsertionCalculatorTest {
	
	VehicleRoutingTransportCosts routingCosts;
	
	VehicleRoutingActivityCosts activityCosts = new VehicleRoutingActivityCosts(){

		@Override
		public double getActivityCost(TourActivity tourAct, double arrivalTime,Driver driver, Vehicle vehicle) {
			return 0;
		}
		
	};
	
	HardActivityStateLevelConstraint hardActivityLevelConstraint = new HardActivityStateLevelConstraint() {
		
		@Override
		public ConstraintsStatus fulfilled(JobInsertionContext iFacts, TourActivity prevAct,TourActivity newAct, TourActivity nextAct, double prevActDepTime) {
			return ConstraintsStatus.FULFILLED;
		}
	};
	
	HardRouteStateLevelConstraint hardRouteLevelConstraint = new HardRouteStateLevelConstraint(){

		@Override
		public boolean fulfilled(JobInsertionContext insertionContext) {
			return true;
		}
		
	};
	
	ActivityInsertionCostsCalculator activityInsertionCostsCalculator;
	
	ShipmentInsertionCalculator insertionCalculator;
	
	Vehicle vehicle;
	
	@Before
	public void doBefore(){
		routingCosts = CostFactory.createManhattanCosts();
		VehicleType type = VehicleTypeImpl.Builder.newInstance("t").addCapacityDimension(0, 2).setCostPerDistance(1).build();
		vehicle = VehicleImpl.Builder.newInstance("v").setStartLocationId("0,0").setType(type).build();
		activityInsertionCostsCalculator = new LocalActivityInsertionCostsCalculator(routingCosts, activityCosts);
		createInsertionCalculator(hardRouteLevelConstraint);
	}

	private void createInsertionCalculator(HardRouteStateLevelConstraint hardRouteLevelConstraint) {
		ConstraintManager constraintManager = new ConstraintManager(mock(VehicleRoutingProblem.class), mock(RouteAndActivityStateGetter.class));
		constraintManager.addConstraint(hardRouteLevelConstraint);
		insertionCalculator = new ShipmentInsertionCalculator(routingCosts, activityInsertionCostsCalculator, constraintManager);
	}
	
	@Test
	public void whenCalculatingInsertionCostsOfShipment_itShouldReturnCorrectCostValue(){
		Shipment shipment = Shipment.Builder.newInstance("s").addSizeDimension(0, 1).setPickupLocation("0,10").setDeliveryLocation("10,0").build();
		VehicleRoute route = VehicleRoute.emptyRoute();
		
		InsertionData iData = insertionCalculator.getInsertionData(route, shipment, vehicle, 0.0, null, Double.MAX_VALUE);
		assertEquals(40.0,iData.getInsertionCost(),0.05);
	}
	
	@Test
	public void whenCalculatingInsertionIntoExistingRoute_itShouldReturnCorrectCosts(){
		Shipment shipment = Shipment.Builder.newInstance("s").addSizeDimension(0, 1).setPickupLocation("0,10").setDeliveryLocation("10,0").build();
		Shipment shipment2 = Shipment.Builder.newInstance("s2").addSizeDimension(0, 1).setPickupLocation("10,10").setDeliveryLocation("0,0").build();
		VehicleRoute route = VehicleRoute.emptyRoute();
		new Inserter(new InsertionListeners()).insertJob(shipment, new InsertionData(0,0,0,vehicle,null), route);
		
		InsertionData iData = insertionCalculator.getInsertionData(route, shipment2, vehicle, 0.0, null, Double.MAX_VALUE);
		assertEquals(0.0,iData.getInsertionCost(),0.05);
		assertEquals(1,iData.getPickupInsertionIndex());
		assertEquals(2,iData.getDeliveryInsertionIndex());
	}
	
	@Test
	public void whenInsertingShipmentInRouteWithNotEnoughCapacity_itShouldReturnNoInsertion(){
		Shipment shipment = Shipment.Builder.newInstance("s").addSizeDimension(0, 1).setPickupLocation("0,10").setDeliveryLocation("10,0").build();
		Shipment shipment2 = Shipment.Builder.newInstance("s2").addSizeDimension(0, 1).setPickupLocation("10,10").setDeliveryLocation("0,0").build();
		VehicleRoute route = VehicleRoute.emptyRoute();
		new Inserter(new InsertionListeners()).insertJob(shipment, new InsertionData(0,0,0,vehicle,null), route);
		createInsertionCalculator(new HardRouteStateLevelConstraint() {
			
			@Override
			public boolean fulfilled(JobInsertionContext insertionContext) {
				return false;
			}
			
		});
		InsertionData iData = insertionCalculator.getInsertionData(route, shipment2, vehicle, 0.0, null, Double.MAX_VALUE);
		assertEquals(InsertionData.createEmptyInsertionData(),iData);
		
	}
	
	
	@Test
	public void whenInsertingThirdShipment_itShouldCalcCorrectVal(){
		Shipment shipment = Shipment.Builder.newInstance("s").addSizeDimension(0, 1).setPickupLocation("0,10").setDeliveryLocation("10,0").build();
		Shipment shipment2 = Shipment.Builder.newInstance("s2").addSizeDimension(0, 1).setPickupLocation("10,10").setDeliveryLocation("0,0").build();
		Shipment shipment3 = Shipment.Builder.newInstance("s3").addSizeDimension(0, 1).setPickupLocation("0,0").setDeliveryLocation("9,10").build();
		
		VehicleRoute route = VehicleRoute.emptyRoute();
		Inserter inserter = new Inserter(new InsertionListeners());
		inserter.insertJob(shipment, new InsertionData(0,0,0,vehicle,null), route);
		inserter.insertJob(shipment2, new InsertionData(0,1,2,vehicle,null),route);
		
		InsertionData iData = insertionCalculator.getInsertionData(route, shipment3, vehicle, 0.0, null, Double.MAX_VALUE);
		assertEquals(0.0,iData.getInsertionCost(),0.05);
		assertEquals(0,iData.getPickupInsertionIndex());
		assertEquals(1,iData.getDeliveryInsertionIndex());
	}
	
	@Test
	public void whenInsertingThirdShipment_itShouldCalcCorrectVal2(){
		Shipment shipment = Shipment.Builder.newInstance("s").addSizeDimension(0, 1).setPickupLocation("0,10").setDeliveryLocation("10,0").build();
		Shipment shipment2 = Shipment.Builder.newInstance("s2").addSizeDimension(0, 1).setPickupLocation("10,10").setDeliveryLocation("0,0").build();
		Shipment shipment3 = Shipment.Builder.newInstance("s3").addSizeDimension(0, 1).setPickupLocation("0,0").setDeliveryLocation("9,9").build();
		
		VehicleRoute route = VehicleRoute.emptyRoute();
		Inserter inserter = new Inserter(new InsertionListeners());
		inserter.insertJob(shipment, new InsertionData(0,0,0,vehicle,null), route);
		inserter.insertJob(shipment2, new InsertionData(0,1,2,vehicle,null),route);
		
		InsertionData iData = insertionCalculator.getInsertionData(route, shipment3, vehicle, 0.0, null, Double.MAX_VALUE);
		assertEquals(2.0,iData.getInsertionCost(),0.05);
		assertEquals(0,iData.getPickupInsertionIndex());
		assertEquals(1,iData.getDeliveryInsertionIndex());
	}
	
	@Test
	public void whenInstertingShipmentWithLoadConstraintWhereCapIsNotSufficient_capConstraintsAreFulfilled(){
		Shipment shipment = Shipment.Builder.newInstance("s").addSizeDimension(0, 1).setPickupLocation("0,10").setDeliveryLocation("10,0").build();
		Shipment shipment2 = Shipment.Builder.newInstance("s2").addSizeDimension(0, 1).setPickupLocation("10,10").setDeliveryLocation("0,0").build();
		Shipment shipment3 = Shipment.Builder.newInstance("s3").addSizeDimension(0, 1).setPickupLocation("0,0").setDeliveryLocation("9,9").build();
		
		
		
		VehicleRoute route = VehicleRoute.emptyRoute();
		route.setVehicleAndDepartureTime(vehicle, 0.0);
		
		Inserter inserter = new Inserter(new InsertionListeners());
		
		inserter.insertJob(shipment, new InsertionData(0,0,0,vehicle,null), route);
		inserter.insertJob(shipment2, new InsertionData(0,1,2,vehicle,null), route);
		
		VehicleRoutingProblem vrp = mock(VehicleRoutingProblem.class);
		
		StateManager stateManager = new StateManager(vrp.getTransportCosts());
		stateManager.updateLoadStates();		
		stateManager.informInsertionStarts(Arrays.asList(route), null);
		
		ConstraintManager constraintManager = new ConstraintManager(vrp,stateManager);
		constraintManager.addConstraint(new PickupAndDeliverShipmentLoadActivityLevelConstraint(stateManager),Priority.CRITICAL);
		constraintManager.addConstraint(new ShipmentPickupsFirstConstraint(),Priority.CRITICAL);
				
		ShipmentInsertionCalculator insertionCalculator = new ShipmentInsertionCalculator(routingCosts, activityInsertionCostsCalculator, 
				constraintManager);
		
		
		InsertionData iData = insertionCalculator.getInsertionData(route, shipment3, vehicle, 0.0, DriverImpl.noDriver(), Double.MAX_VALUE);
		assertTrue(iData instanceof InsertionData.NoInsertionFound);
		
	}
	
	@Test
	public void whenInsertingServiceWhileNoCapIsAvailable_itMustReturnNoInsertionData(){
		Shipment shipment = Shipment.Builder.newInstance("s").addSizeDimension(0, 1).setPickupLocation("0,10").setDeliveryLocation("0,0").build();
		Shipment shipment2 = Shipment.Builder.newInstance("s2").addSizeDimension(0, 1).setPickupLocation("10,10").setDeliveryLocation("0,0").build();
		VehicleRoute route = VehicleRoute.emptyRoute();
		route.setVehicleAndDepartureTime(vehicle, 0.0);
		
		Inserter inserter = new Inserter(new InsertionListeners());
		
		inserter.insertJob(shipment, new InsertionData(0,0,0,vehicle,null), route);
		inserter.insertJob(shipment2, new InsertionData(0,1,2,vehicle,null), route);
//		inserter.insertJob(shipment2, new InsertionData(0,1,2,vehicle,null), route);
		
		VehicleRoutingProblem vrp = mock(VehicleRoutingProblem.class);
		
		StateManager stateManager = new StateManager(vrp.getTransportCosts());
		stateManager.updateLoadStates();
		stateManager.informInsertionStarts(Arrays.asList(route), null);

		ConstraintManager constraintManager = new ConstraintManager(vrp,stateManager);
		constraintManager.addLoadConstraint();
//		constraintManager.addConstraint(new PickupAndDeliverShipmentLoadActivityLevelConstraint(stateManager),Priority.CRITICAL);
//		constraintManager.addConstraint(new ShipmentPickupsFirstConstraint(),Priority.CRITICAL);
		
		stateManager.informInsertionStarts(Arrays.asList(route), null);
		
		JobCalculatorSwitcher switcher = new JobCalculatorSwitcher();
		ServiceInsertionCalculator serviceInsertionCalc = new ServiceInsertionCalculator(routingCosts, activityInsertionCostsCalculator, constraintManager);
		ShipmentInsertionCalculator insertionCalculator = new ShipmentInsertionCalculator(routingCosts, activityInsertionCostsCalculator, constraintManager);
		switcher.put(Pickup.class, serviceInsertionCalc);
		switcher.put(Service.class, serviceInsertionCalc);
		switcher.put(Shipment.class, insertionCalculator);
		
//		Service service = Service.Builder.newInstance("pick", 1).setLocationId("5,5").build();
		Pickup service = (Pickup)Pickup.Builder.newInstance("pick").addSizeDimension(0, 1).setLocationId("5,5").build();
		InsertionData iData = switcher.getInsertionData(route, service, vehicle, 0, DriverImpl.noDriver(), Double.MAX_VALUE);
//		routeActVisitor.visit(route);
		
		assertEquals(3, iData.getDeliveryInsertionIndex());
	}
	
	
}
