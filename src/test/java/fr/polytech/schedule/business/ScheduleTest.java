package fr.polytech.schedule.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.stream.Collectors;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.UserTransaction;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import arquillian.AbstractScheduleTest;
import fr.polytech.entities.Delivery;
import fr.polytech.entities.Drone;
import fr.polytech.entities.Parcel;
import fr.polytech.entities.TimeState;
import fr.polytech.schedule.components.DeliveryOrganizer;
import fr.polytech.schedule.components.DeliveryScheduler;
import fr.polytech.schedule.components.ScheduleBean;
import fr.polytech.schedule.exception.NoFreeDroneAtThisTimeSlotException;
import fr.polytech.schedule.exception.ZeroDronesInWarehouseException;

@RunWith(Arquillian.class)
@Transactional(TransactionMode.COMMIT)
public class ScheduleTest extends AbstractScheduleTest {
	@EJB(name = "schedule")
	private DeliveryScheduler deliveryScheduler;

	@EJB(name = "schedule")
	private DeliveryOrganizer deliveryOrganizer;

	@Inject
	private ScheduleBean schedule;

	@Inject
	private UserTransaction utx;

	@PersistenceContext
	private EntityManager entityManager;

	private Parcel parcel;

	private List<Drone> drones;

	private Delivery delivery1;
	private Delivery delivery2;
	private Delivery delivery3;
	private GregorianCalendar now;

	@Before
	public void init() {

		this.drones = new ArrayList<>();
		this.drones.add(new Drone("000"));
		entityManager.persist(drones.get(0));

		now = new GregorianCalendar();

		parcel = new Parcel("AAAAAAAAA1", "address", "carrier", "Dupond");
		entityManager.persist(parcel);
		delivery1 = new Delivery("DDDDDDDDD1");
		delivery1.setParcel(parcel);
		delivery2 = new Delivery("DDDDDDDDD2");
		delivery2.setParcel(parcel);
		delivery3 = new Delivery("DDDDDDDDD3");
		delivery3.setParcel(parcel);
		entityManager.persist(delivery1);
		entityManager.persist(delivery2);
		entityManager.persist(delivery3);
	}

	@After
	public void cleaningUp() throws Exception {
		utx.begin();

		for (Drone drone : drones) {
			drone.getTimeSlots().clear();
		}

		delivery1 = entityManager.merge(delivery1);
		entityManager.remove(delivery1);
		delivery2 = entityManager.merge(delivery2);
		entityManager.remove(delivery2);
		delivery3 = entityManager.merge(delivery3);
		entityManager.remove(delivery3);

		parcel = entityManager.merge(parcel);
		entityManager.remove(parcel);

		for (int i = 0; i < this.drones.size(); i++) {
			this.drones.set(i, entityManager.merge(this.drones.get(i)));
			entityManager.remove(this.drones.get(i));
			this.drones.set(i, null);
		}
		utx.commit();
	}

	private List<TimeState> splitString(String modelStr) {
		return (Arrays.asList(modelStr.split(","))).stream().map(str -> {
			switch (str) {
				case "del":
					return TimeState.DELIVERY;
				case "ava":
					return TimeState.AVAILABLE;
				case "una":
					return TimeState.RESERVED_FOR_CHARGE;
				case "rev":
					return TimeState.REVIEW;
				case "cha":
					return TimeState.CHARGING;
				default:
					return null;
			}
		}).collect(Collectors.toList());
	}

	// Fonctionnel
	@Test
	public void scheduleDeliveryTestOpeningHour() throws Exception {
		GregorianCalendar tomorrow = new GregorianCalendar();
		tomorrow.setTimeInMillis(now.getTimeInMillis() + 24l * 60l * 60l * 1000l);
		GregorianCalendar c = new GregorianCalendar(tomorrow.get(GregorianCalendar.YEAR),
				tomorrow.get(GregorianCalendar.MONTH), tomorrow.get(GregorianCalendar.DAY_OF_MONTH), 8, 0);
		schedule.scheduleDelivery(c, delivery1);
		Delivery next = schedule.getNextDelivery(tomorrow);
		assertEquals(delivery1, next);
	}

	@Test(expected = Exception.class)
	public void scheduleDeliveryTestClosingHour() throws Exception {
		GregorianCalendar tomorrow = new GregorianCalendar();
		tomorrow.setTimeInMillis(now.getTimeInMillis() + 24l * 60l * 60l * 1000l);
		GregorianCalendar c = new GregorianCalendar(tomorrow.get(GregorianCalendar.YEAR),
				tomorrow.get(GregorianCalendar.MONTH), tomorrow.get(GregorianCalendar.DAY_OF_MONTH), 0, 0);
		schedule.scheduleDelivery(c, delivery1);
	}

	@Test(expected = NoFreeDroneAtThisTimeSlotException.class)
	public void scheduleDeliveryTestAtTheSameHour() throws Exception {
		GregorianCalendar tomorrow = new GregorianCalendar();
		tomorrow.setTimeInMillis(now.getTimeInMillis() + 24l * 60l * 60l * 1000l);
		GregorianCalendar c = new GregorianCalendar(tomorrow.get(GregorianCalendar.YEAR),
				tomorrow.get(GregorianCalendar.MONTH), tomorrow.get(GregorianCalendar.DAY_OF_MONTH), 8, 0);
		schedule.scheduleDelivery(c, delivery1);
		Delivery next = schedule.getNextDelivery(tomorrow);
		assertEquals(delivery1, next);
		schedule.scheduleDelivery(c, delivery2);
	}

	@Test
	public void getPlanningTestOneDelivery() throws Exception {
		GregorianCalendar tomorrow = new GregorianCalendar();
		tomorrow.setTimeInMillis(now.getTimeInMillis() + 24l * 60l * 60l * 1000l);
		GregorianCalendar c = new GregorianCalendar(tomorrow.get(GregorianCalendar.YEAR),
				tomorrow.get(GregorianCalendar.MONTH), tomorrow.get(GregorianCalendar.DAY_OF_MONTH), 8, 0);
		schedule.scheduleDelivery(c, delivery1);
		List<TimeState> states = schedule.getCurrentPlanning("000");
		List<TimeState> model = splitString(
				"del,ava,ava,cha,cha,cha,cha,ava,ava,ava,una,una,una,una,ava,ava,ava,una,una,una,una,ava,ava,ava,una,una,una,una,ava,ava,ava,una,una,una,una,ava,ava,ava,una,una,una,una");
		for (int i = 0; i < states.size(); i++)
			assertEquals(model.get(i), states.get(i));
	}

	@Test
	public void getPlanningTestTwoDeliveries() throws Exception {
		GregorianCalendar tomorrow = new GregorianCalendar();
		tomorrow.setTimeInMillis(now.getTimeInMillis() + 24l * 60l * 60l * 1000l);

		GregorianCalendar c = new GregorianCalendar(tomorrow.get(GregorianCalendar.YEAR),
				tomorrow.get(GregorianCalendar.MONTH), tomorrow.get(GregorianCalendar.DAY_OF_MONTH), 8, 0);
		schedule.scheduleDelivery(c, delivery1);

		GregorianCalendar c2 = new GregorianCalendar(tomorrow.get(GregorianCalendar.YEAR),
				tomorrow.get(GregorianCalendar.MONTH), tomorrow.get(GregorianCalendar.DAY_OF_MONTH), 8, 15);
		schedule.scheduleDelivery(c2, delivery1);

		List<TimeState> states = schedule.getCurrentPlanning("000");
		List<TimeState> model = splitString(
				"del,del,ava,cha,cha,cha,cha,ava,ava,ava,una,una,una,una,ava,ava,ava,una,una,una,una,ava,ava,ava,una,una,una,una,ava,ava,ava,una,una,una,una,ava,ava,ava,una,una,una,una");
		for (int i = 0; i < states.size(); i++)
			assertEquals(model.get(i), states.get(i));
	}

	@Test
	public void getPlanningTestThreeDeliveries() throws Exception {
		GregorianCalendar tomorrow = new GregorianCalendar();
		tomorrow.setTimeInMillis(now.getTimeInMillis() + 24l * 60l * 60l * 1000l);

		GregorianCalendar c = new GregorianCalendar(tomorrow.get(GregorianCalendar.YEAR),
				tomorrow.get(GregorianCalendar.MONTH), tomorrow.get(GregorianCalendar.DAY_OF_MONTH), 8, 0);
		schedule.scheduleDelivery(c, delivery1);

		GregorianCalendar c2 = new GregorianCalendar(tomorrow.get(GregorianCalendar.YEAR),
				tomorrow.get(GregorianCalendar.MONTH), tomorrow.get(GregorianCalendar.DAY_OF_MONTH), 8, 15);
		schedule.scheduleDelivery(c2, delivery2);

		GregorianCalendar c4 = new GregorianCalendar(tomorrow.get(GregorianCalendar.YEAR),
				tomorrow.get(GregorianCalendar.MONTH), tomorrow.get(GregorianCalendar.DAY_OF_MONTH), 9, 45);
		schedule.scheduleDelivery(c4, delivery3);

		List<TimeState> states = schedule.getCurrentPlanning("000");
		List<TimeState> model = splitString(
				"del,del,ava,cha,cha,cha,cha,del,ava,ava,cha,cha,cha,cha,ava,ava,ava,una,una,una,una,ava,ava,ava,una,una,una,una,ava,ava,ava,una,una,una,una,ava,ava,ava,una,una,una,una");
		for (int i = 0; i < states.size(); i++)
			assertEquals(model.get(i), states.get(i));
	}

	@Test
	public void getPlanningTestWithReview() throws Exception {
		GregorianCalendar tomorrow = new GregorianCalendar();
		tomorrow.setTimeInMillis(now.getTimeInMillis() + 24l * 60l * 60l * 1000l);
		drones.get(0).setFlightTime(79);

		GregorianCalendar c = new GregorianCalendar(tomorrow.get(GregorianCalendar.YEAR),
				tomorrow.get(GregorianCalendar.MONTH), tomorrow.get(GregorianCalendar.DAY_OF_MONTH), 8, 0);
		schedule.scheduleDelivery(c, delivery1);

		GregorianCalendar c2 = new GregorianCalendar(tomorrow.get(GregorianCalendar.YEAR),
				tomorrow.get(GregorianCalendar.MONTH), tomorrow.get(GregorianCalendar.DAY_OF_MONTH), 11, 15);
		schedule.scheduleDelivery(c2, delivery2);

		List<TimeState> states = schedule.getCurrentPlanning("000");
		List<TimeState> model = splitString(
				"del,rev,rev,rev,rev,rev,rev,rev,rev,rev,rev,rev,rev,del,ava,ava,cha,cha,cha,cha,ava,ava,ava,una,una,una,una,ava,ava,ava,una,una,una,una,ava,ava,ava,una,una,una,una");
		for (int i = 0; i < states.size(); i++)
			assertEquals(model.get(i), states.get(i));
	}

	@Test
	public void getNextDeliveriesTest() throws Exception {
		GregorianCalendar yesterday = new GregorianCalendar();
		yesterday.setTimeInMillis(now.getTimeInMillis() - 24l * 60l * 60l * 1000l);
		assertNull(deliveryOrganizer.getNextDelivery(yesterday));
		assertTrue(deliveryScheduler.scheduleDelivery(new GregorianCalendar(yesterday.get(GregorianCalendar.YEAR),
				yesterday.get(GregorianCalendar.MONTH), yesterday.get(GregorianCalendar.DAY_OF_MONTH), 8, 0),
				delivery1));
		assertNull(deliveryOrganizer.getNextDelivery(yesterday));
	}

	/**
	 * Tests DateIsAvailable
	 */
	@Test
	public void dateIsAvailableTestWithNothing() {
		assertEquals(TimeState.AVAILABLE,
				schedule.dateIsAvailable(new GregorianCalendar(now.get(GregorianCalendar.YEAR),
						now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH), 8, 15),
						drones.get(0)));
	}

	@Test
	public void dateIsAvailableTestWithOneDeliveryBefore() throws Exception {
		schedule.createDeliveryTimeSlot(new GregorianCalendar(now.get(GregorianCalendar.YEAR),
				now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH), 8, 0), delivery1,
				drones.get(0));
		assertEquals(TimeState.AVAILABLE,
				schedule.dateIsAvailable(new GregorianCalendar(now.get(GregorianCalendar.YEAR),
						now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH), 8, 15),
						drones.get(0)));
	}

	@Test
	public void dateIsAvailableTestWithOneDeliveryAtSameTime() throws Exception {
		schedule.createDeliveryTimeSlot(new GregorianCalendar(now.get(GregorianCalendar.YEAR),
				now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH), 8, 15), delivery1,
				drones.get(0));
		assertNotEquals(TimeState.AVAILABLE,
				schedule.dateIsAvailable(new GregorianCalendar(now.get(GregorianCalendar.YEAR),
						now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH), 8, 15),
						drones.get(0)));
	}

	@Test
	public void dateIsAvailableTestWithOneTimeSlotBefore() {
		schedule.createChargingTimeSlot(new GregorianCalendar(now.get(GregorianCalendar.YEAR),
				now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH), 8, 0), drones.get(0));
		assertEquals(TimeState.AVAILABLE,
				schedule.dateIsAvailable(new GregorianCalendar(now.get(GregorianCalendar.YEAR),
						now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH), 8, 15),
						drones.get(0)));
	}

	@Test
	public void dateIsAvailableTestWithOneTimeSlotAtSameTime() {
		schedule.createChargingTimeSlot(new GregorianCalendar(now.get(GregorianCalendar.YEAR),
				now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH), 8, 15), drones.get(0));
		assertNotEquals(TimeState.AVAILABLE,
				schedule.dateIsAvailable(new GregorianCalendar(now.get(GregorianCalendar.YEAR),
						now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH), 8, 15),
						drones.get(0)));
	}

	@Test
	public void getFreeDroneWithNoDroneTest() {
		this.drones.set(0, entityManager.merge(this.drones.get(0)));
		entityManager.remove(this.drones.get(0));
		this.drones.set(0, null);

		assertThrows(ZeroDronesInWarehouseException.class, () -> {
			schedule.getFreeDrone(new GregorianCalendar(now.get(GregorianCalendar.YEAR),
					now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH), 8, 30));
		});
	}

	@Test
	public void getFreeDroneTest() throws Exception {

		for (int i = 1; i < 3; i++) {
			this.drones.add(new Drone("00" + i));
			entityManager.persist(drones.get(i));
		}

		GregorianCalendar date = new GregorianCalendar(now.get(GregorianCalendar.YEAR),
				now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH), 8, 30);

		assertTrue(schedule.scheduleDelivery(date, delivery1));
		assertTrue(schedule.scheduleDelivery(date, delivery1));
		assertTrue(schedule.scheduleDelivery(date, delivery1));
		assertThrows(NoFreeDroneAtThisTimeSlotException.class, () -> {
			schedule.scheduleDelivery(date, delivery1);
		});
		this.drones.add(new Drone("004"));
		entityManager.persist(drones.get(3));
		assertTrue(schedule.scheduleDelivery(date, delivery2));

	}

	/*
	 * The following methods are testing the scheduling : D = delivery N = Nothing
	 */

	/**
	 * D1 - D2 - D3 - N Charged or at the beginning of the day -> Delivery -
	 * Delivery - Delivery - Charging - Charging - Charging - Charging
	 *
	 * @throws Exception
	 */
	@Test
	public void setChargingTimeSlotsTestWithThreeDeliveries1() throws Exception {
		schedule.scheduleDelivery(new GregorianCalendar(now.get(GregorianCalendar.YEAR),
				now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH), 8, 0), delivery1);
		drones.set(0, entityManager.merge(drones.get(0)));
		assertEquals(18, drones.get(0).getTimeSlots().stream()
				.filter(timeSlot -> timeSlot.getState() == TimeState.RESERVED_FOR_CHARGE).count());
		assertEquals(1, drones.get(0).getTimeSlots().stream()
				.filter(timeSlot -> timeSlot.getState() == TimeState.DELIVERY).count());
		assertEquals(4, drones.get(0).getTimeSlots().stream()
				.filter(timeSlot -> timeSlot.getState() == TimeState.CHARGING).count());

		assertEquals(TimeState.AVAILABLE,
				schedule.dateIsAvailable(new GregorianCalendar(now.get(GregorianCalendar.YEAR),
						now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH), 8, 30),
						drones.get(0)));
		assertNotEquals(TimeState.AVAILABLE,
				schedule.dateIsAvailable(new GregorianCalendar(now.get(GregorianCalendar.YEAR),
						now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH), 8, 45),
						drones.get(0)));
		assertNotEquals(TimeState.AVAILABLE,
				schedule.dateIsAvailable(new GregorianCalendar(now.get(GregorianCalendar.YEAR),
						now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH), 9, 0),
						drones.get(0)));
		assertNotEquals(TimeState.AVAILABLE,
				schedule.dateIsAvailable(new GregorianCalendar(now.get(GregorianCalendar.YEAR),
						now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH), 9, 15),
						drones.get(0)));
		assertNotEquals(TimeState.AVAILABLE,
				schedule.dateIsAvailable(new GregorianCalendar(now.get(GregorianCalendar.YEAR),
						now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH), 9, 30),
						drones.get(0)));
		assertEquals(TimeState.AVAILABLE,
				schedule.dateIsAvailable(new GregorianCalendar(now.get(GregorianCalendar.YEAR),
						now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH), 9, 45),
						drones.get(0)));

		// 4 Charging
		assertEquals(TimeState.CHARGING,
				schedule.findTimeSlotAtDate(drones.get(0).getTimeSlots(),
						new GregorianCalendar(now.get(GregorianCalendar.YEAR), now.get(GregorianCalendar.MONTH),
								now.get(GregorianCalendar.DAY_OF_MONTH), 9, 0))
						.getState());
		assertEquals(TimeState.CHARGING,
				schedule.findTimeSlotAtDate(drones.get(0).getTimeSlots(),
						new GregorianCalendar(now.get(GregorianCalendar.YEAR), now.get(GregorianCalendar.MONTH),
								now.get(GregorianCalendar.DAY_OF_MONTH), 9, 15))
						.getState());
		assertEquals(TimeState.CHARGING,
				schedule.findTimeSlotAtDate(drones.get(0).getTimeSlots(),
						new GregorianCalendar(now.get(GregorianCalendar.YEAR), now.get(GregorianCalendar.MONTH),
								now.get(GregorianCalendar.DAY_OF_MONTH), 9, 30))
						.getState());

	}

	@Test
	public void setReviewTimeSlotsAndScheduleDeliveryTestWith() throws Exception {

		drones.get(0).setFlightTime(79);

		try {
			schedule.scheduleDelivery(new GregorianCalendar(now.get(GregorianCalendar.YEAR),
					now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH), 8, 15), delivery1);
			fail();
		} catch (Exception e) {
			assertTrue("test passed", true);
		}

		assertEquals(12, drones.get(0).getTimeSlots().stream()
				.filter(timeSlot -> timeSlot.getState() == TimeState.REVIEW).count());

		// 4 Review
		assertEquals(TimeState.REVIEW,
				schedule.findTimeSlotAtDate(drones.get(0).getTimeSlots(),
						new GregorianCalendar(now.get(GregorianCalendar.YEAR), now.get(GregorianCalendar.MONTH),
								now.get(GregorianCalendar.DAY_OF_MONTH), 8, 15))
						.getState());
		assertEquals(TimeState.REVIEW,
				schedule.findTimeSlotAtDate(drones.get(0).getTimeSlots(),
						new GregorianCalendar(now.get(GregorianCalendar.YEAR), now.get(GregorianCalendar.MONTH),
								now.get(GregorianCalendar.DAY_OF_MONTH), 11, 00))
						.getState());

		assertTrue(schedule.scheduleDelivery(new GregorianCalendar(now.get(GregorianCalendar.YEAR),
				now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH), 11, 15), delivery2));

		// Delivery 10:30
		assertEquals(TimeState.DELIVERY,
				schedule.findTimeSlotAtDate(drones.get(0).getTimeSlots(),
						new GregorianCalendar(now.get(GregorianCalendar.YEAR), now.get(GregorianCalendar.MONTH),
								now.get(GregorianCalendar.DAY_OF_MONTH), 11, 15))
						.getState());

	}

	@Test
	public void getIndexFromDateTest() {
		GregorianCalendar date = new GregorianCalendar(now.get(GregorianCalendar.YEAR),
				now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH), ScheduleBean.STARTING_HOUR,
				45);
		assertEquals(3, schedule.getIndexFromDate(date));

	}

	@Test
	public void getDateFromIndexTest() {
		GregorianCalendar date = new GregorianCalendar(now.get(GregorianCalendar.YEAR),
				now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH), ScheduleBean.STARTING_HOUR,
				45);
		assertEquals(date.get(GregorianCalendar.HOUR), schedule.getDateFromIndex(3).get(GregorianCalendar.HOUR));
		assertEquals(date.get(GregorianCalendar.MINUTE), schedule.getDateFromIndex(3).get(GregorianCalendar.MINUTE));
	}
}
