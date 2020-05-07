package fr.polytech.schedule.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;
import java.util.stream.Collector;
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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import arquillian.AbstractScheduleTest;
import fr.polytech.entities.Delivery;
import fr.polytech.entities.Drone;
import fr.polytech.entities.Parcel;
import fr.polytech.entities.TimeSlot;
import fr.polytech.entities.TimeState;
import fr.polytech.schedule.components.DeliveryOrganizer;
import fr.polytech.schedule.components.DeliveryScheduler;
import fr.polytech.schedule.components.ScheduleBean;
import fr.polytech.schedule.exception.DroneNotFoundException;
import fr.polytech.schedule.exception.TimeslotUnvailableException;

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

	private Drone drone;

	private Delivery delivery1;
	private Delivery delivery2;
	private Delivery delivery3;
	private GregorianCalendar now;

	@Before
	public void init()  throws Exception  {
		this.drone = new Drone("000");
		entityManager.persist(drone);
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

		delivery1 = entityManager.merge(delivery1);
		entityManager.remove(delivery1);
		delivery2 = entityManager.merge(delivery2);
		entityManager.remove(delivery2);
		delivery3 = entityManager.merge(delivery3);
		entityManager.remove(delivery3);

		parcel = entityManager.merge(parcel);
		entityManager.remove(parcel);

		drone = entityManager.merge(drone);
		entityManager.remove(drone);

		utx.commit();
	}

	private List<TimeState> splitString(String modelStr)
	{
		return (Arrays.asList(modelStr.split(","))).stream().map(str -> {
			switch(str) {
				case "del":
					return TimeState.DELIVERY;
				case "ava":
					return null;
				case "una":
					return TimeState.UNAVAILABLE;
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
	public void scheduleDeliveryTestOpeningHour() throws DroneNotFoundException, TimeslotUnvailableException {
		GregorianCalendar tomorrow = new GregorianCalendar();
		tomorrow.setTimeInMillis(now.getTimeInMillis() + 24l*60l*60l*1000l);
		GregorianCalendar c = new GregorianCalendar(tomorrow.get(GregorianCalendar.YEAR),
		tomorrow.get(GregorianCalendar.MONTH), tomorrow.get(GregorianCalendar.DAY_OF_MONTH), 8, 0);
		schedule.scheduleDelivery(c, delivery1);
		Delivery next = schedule.getNextDelivery();
		assertEquals(delivery1, next);
	}

	@Test(expected = TimeslotUnvailableException.class)
	public void scheduleDeliveryTestClosingHour() throws DroneNotFoundException, TimeslotUnvailableException {
		GregorianCalendar tomorrow = new GregorianCalendar();
		tomorrow.setTimeInMillis(now.getTimeInMillis() + 24l*60l*60l*1000l);
		GregorianCalendar c = new GregorianCalendar(tomorrow.get(GregorianCalendar.YEAR),
		tomorrow.get(GregorianCalendar.MONTH), tomorrow.get(GregorianCalendar.DAY_OF_MONTH), 0, 0);
		schedule.scheduleDelivery(c, delivery1);
	}

	@Test(expected = TimeslotUnvailableException.class)
	public void scheduleDeliveryTestAtTheSameHour() throws DroneNotFoundException, TimeslotUnvailableException {
		GregorianCalendar tomorrow = new GregorianCalendar();
		tomorrow.setTimeInMillis(now.getTimeInMillis() + 24l*60l*60l*1000l);
		GregorianCalendar c = new GregorianCalendar(tomorrow.get(GregorianCalendar.YEAR),
		tomorrow.get(GregorianCalendar.MONTH), tomorrow.get(GregorianCalendar.DAY_OF_MONTH), 8, 0);
		schedule.scheduleDelivery(c, delivery1);
		Delivery next = schedule.getNextDelivery();
		assertEquals(delivery1, next);
		schedule.scheduleDelivery(c, delivery2);
	}

	@Test
	public void getPlanningTestOneDelivery() throws DroneNotFoundException, TimeslotUnvailableException {
		GregorianCalendar tomorrow = new GregorianCalendar();
		tomorrow.setTimeInMillis(now.getTimeInMillis() + 24l*60l*60l*1000l);
		GregorianCalendar c = new GregorianCalendar(tomorrow.get(GregorianCalendar.YEAR),
		tomorrow.get(GregorianCalendar.MONTH), tomorrow.get(GregorianCalendar.DAY_OF_MONTH), 8, 0);
		schedule.scheduleDelivery(c, delivery1);
		List<TimeState> states = schedule.getCurrentPlanning("000");
		List<TimeState> model = splitString("del,ava,ava,cha,cha,cha,cha,ava,ava,ava,una,una,una,una,ava,ava,ava,una,una,una,una,ava,ava,ava,una,una,una,una,ava,ava,ava,una,una,una,una,ava,ava,ava,una,una,una,una");
		for(int i=0; i<states.size(); i++)
		{
		    assertEquals(model.get(i), states.get(i));
		}
	}

	@Test
	public void getPlanningTestTwoDeliveries() throws DroneNotFoundException, TimeslotUnvailableException {
		GregorianCalendar tomorrow = new GregorianCalendar();
		tomorrow.setTimeInMillis(now.getTimeInMillis() + 24l*60l*60l*1000l);

		GregorianCalendar c = new GregorianCalendar(tomorrow.get(GregorianCalendar.YEAR),
		tomorrow.get(GregorianCalendar.MONTH), tomorrow.get(GregorianCalendar.DAY_OF_MONTH), 8, 0);
		schedule.scheduleDelivery(c, delivery1);

		GregorianCalendar c2 = new GregorianCalendar(tomorrow.get(GregorianCalendar.YEAR),
		tomorrow.get(GregorianCalendar.MONTH), tomorrow.get(GregorianCalendar.DAY_OF_MONTH), 8, 15);
		schedule.scheduleDelivery(c2, delivery1);

		List<TimeState> states = schedule.getCurrentPlanning("000");
		List<TimeState> model = splitString("del,del,ava,cha,cha,cha,cha,ava,ava,ava,una,una,una,una,ava,ava,ava,una,una,una,una,ava,ava,ava,una,una,una,una,ava,ava,ava,una,una,una,una,ava,ava,ava,una,una,una,una");
		for(int i=0; i<states.size(); i++)
		{
			System.out.printf("=======>%d\n", i);
			System.out.println(model.get(i) != null ? model.get(i).toString() : "AVAILABLE");
			System.out.println(states.get(i) != null ? states.get(i).toString() : "AVAILABLE");
		    assertEquals(model.get(i), states.get(i));
		}
	}

	@Test
	public void getPlanningTestThreeDeliveries() throws DroneNotFoundException, TimeslotUnvailableException {
		GregorianCalendar tomorrow = new GregorianCalendar();
		tomorrow.setTimeInMillis(now.getTimeInMillis() + 24l*60l*60l*1000l);

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
		List<TimeState> model = splitString("del,del,ava,cha,cha,cha,cha,del,ava,ava,cha,cha,cha,cha,ava,ava,ava,una,una,una,una,ava,ava,ava,una,una,una,una,ava,ava,ava,una,una,una,una,ava,ava,ava,una,una,una,una");
		for(int i=0; i<states.size(); i++)
		{
			System.out.printf("=======>%d\n", i);
			System.out.println(model.get(i) != null ? model.get(i).toString() : "AVAILABLE");
			System.out.println(states.get(i) != null ? states.get(i).toString() : "AVAILABLE");
		    assertEquals(model.get(i), states.get(i));
		}
	}

	@Ignore
	@Test
	public void getPlanningTestWithReview() throws DroneNotFoundException, TimeslotUnvailableException {
		GregorianCalendar tomorrow = new GregorianCalendar();
		tomorrow.setTimeInMillis(now.getTimeInMillis() + 24l*60l*60l*1000l);
		drone.setFlightTime(79);

		GregorianCalendar c = new GregorianCalendar(tomorrow.get(GregorianCalendar.YEAR),
		tomorrow.get(GregorianCalendar.MONTH), tomorrow.get(GregorianCalendar.DAY_OF_MONTH), 8, 0);
		schedule.scheduleDelivery(c, delivery1);

		GregorianCalendar c2 = new GregorianCalendar(tomorrow.get(GregorianCalendar.YEAR),
		tomorrow.get(GregorianCalendar.MONTH), tomorrow.get(GregorianCalendar.DAY_OF_MONTH), 8, 15);
		schedule.scheduleDelivery(c2, delivery2);

		List<TimeState> states = schedule.getCurrentPlanning("000");
		List<TimeState> model = splitString("del,del,del,cha,cha,cha,cha,del,del,ava,cha,cha,cha,cha,ava,ava,ava,una,una,una,una,ava,ava,ava,una,una,una,una,ava,ava,ava,una,una,una,una,ava,ava,ava,una,una,una,una");
		for(int i=0; i<states.size(); i++)
		{
		    assertEquals(model.get(i), states.get(i));
		}
	}

	@Test
	public void getNextDeliveriesTest() throws DroneNotFoundException, TimeslotUnvailableException {
		GregorianCalendar yesterday = new GregorianCalendar();
		yesterday.setTimeInMillis(now.getTimeInMillis() - 24l*60l*60l*1000l);
		assertNull(deliveryOrganizer.getNextDelivery());
		assertTrue(deliveryScheduler.scheduleDelivery(new GregorianCalendar(yesterday.get(GregorianCalendar.YEAR),
		yesterday.get(GregorianCalendar.MONTH), yesterday.get(GregorianCalendar.DAY_OF_MONTH), 8, 0), delivery1));
		assertNull(deliveryOrganizer.getNextDelivery());
	}

	/**
	 * Tests DateIsAvailable
	 */
	@Test
	public void dateIsAvailableTestWithNothing() {
		assertTrue(schedule.dateIsAvailable(new GregorianCalendar(now.get(GregorianCalendar.YEAR),
				now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH), 8, 15),
				drone));
	}

	@Test
	public void dateIsAvailableTestWithOneDeliveryBefore()
			throws IllegalAccessException, TimeslotUnvailableException {
		schedule.createDeliveryTimeSlot(new GregorianCalendar(now.get(GregorianCalendar.YEAR),
				now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH), 8, 0),
				delivery1, drone);
		assertTrue(schedule.dateIsAvailable(new GregorianCalendar(now.get(GregorianCalendar.YEAR),
				now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH), 8, 15),
				drone));
	}

	@Test
	public void dateIsAvailableTestWithOneDeliveryAtSameTime()
			throws IllegalAccessException, TimeslotUnvailableException {
		schedule.createDeliveryTimeSlot(new GregorianCalendar(now.get(GregorianCalendar.YEAR),
				now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH), 8, 15),
				delivery1, drone);
		assertFalse(schedule.dateIsAvailable(new GregorianCalendar(now.get(GregorianCalendar.YEAR),
				now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH), 8, 15),
				drone));
	}

	@Test
	public void dateIsAvailableTestWithOneTimeSlotBefore() {
		schedule.createChargingTimeSlot(new GregorianCalendar(now.get(GregorianCalendar.YEAR),
				now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH), 8, 0),
				drone);
		assertTrue(schedule.dateIsAvailable(new GregorianCalendar(now.get(GregorianCalendar.YEAR),
				now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH), 8, 15),
				drone));
	}

	@Test
	public void dateIsAvailableTestWithOneTimeSlotAtSameTime() {
		schedule.createChargingTimeSlot(new GregorianCalendar(now.get(GregorianCalendar.YEAR),
				now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH), 8, 15),
				drone);
		assertFalse(schedule.dateIsAvailable(new GregorianCalendar(now.get(GregorianCalendar.YEAR),
				now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH), 8, 15),
				drone));
	}

	/*
	 * The following methods are testing the scheduling : D = delivery N = Nothing
	 */

	/**
	 * D1 - D2 - D3 - N Charged or at the beginning of the day -> Delivery -
	 * Delivery - Delivery - Charging - Charging - Charging - Charging
	 *
	 * @throws TimeslotUnvailableException
	 * @throws DroneNotFoundException
	 */
	@Test
	public void setChargingTimeSlotsTestWithThreeDeliveries1()
			throws IllegalAccessException, DroneNotFoundException, TimeslotUnvailableException {
		schedule.scheduleDelivery(new GregorianCalendar(now.get(GregorianCalendar.YEAR),
				now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH), 8, 0),
				delivery1);
		drone = entityManager.merge(drone);
		assertEquals(18, drone.getTimeSlots().stream()
				.filter(timeSlot -> timeSlot.getState() == TimeState.UNAVAILABLE).count());
		assertEquals(1, drone.getTimeSlots().stream()
				.filter(timeSlot -> timeSlot.getState() == TimeState.DELIVERY).count());
		assertEquals(4, drone.getTimeSlots().stream()
				.filter(timeSlot -> timeSlot.getState() == TimeState.CHARGING).count());

		assertTrue(schedule.dateIsAvailable(new GregorianCalendar(now.get(GregorianCalendar.YEAR),
				now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH), 8, 30),
				drone));
		assertFalse(schedule.dateIsAvailable(new GregorianCalendar(now.get(GregorianCalendar.YEAR),
				now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH), 8, 45),
				drone));
		assertFalse(schedule.dateIsAvailable(new GregorianCalendar(now.get(GregorianCalendar.YEAR),
				now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH), 9, 0),
				drone));
		assertFalse(schedule.dateIsAvailable(new GregorianCalendar(now.get(GregorianCalendar.YEAR),
				now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH), 9, 15),
				drone));
		assertFalse(schedule.dateIsAvailable(new GregorianCalendar(now.get(GregorianCalendar.YEAR),
				now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH), 9, 30),
				drone));
		assertTrue(schedule.dateIsAvailable(new GregorianCalendar(now.get(GregorianCalendar.YEAR),
				now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH), 9, 45),
				drone));

		// 4 Charging
		assertEquals(TimeState.CHARGING, schedule
				.findTimeSlotAtDate(drone.getTimeSlots(),
						new GregorianCalendar(now.get(GregorianCalendar.YEAR),
								now.get(GregorianCalendar.MONTH),
								now.get(GregorianCalendar.DAY_OF_MONTH), 9, 0))
				.getState());
		assertEquals(TimeState.CHARGING, schedule
				.findTimeSlotAtDate(drone.getTimeSlots(),
						new GregorianCalendar(now.get(GregorianCalendar.YEAR),
								now.get(GregorianCalendar.MONTH),
								now.get(GregorianCalendar.DAY_OF_MONTH), 9, 15))
				.getState());
		assertEquals(TimeState.CHARGING, schedule
				.findTimeSlotAtDate(drone.getTimeSlots(),
						new GregorianCalendar(now.get(GregorianCalendar.YEAR),
								now.get(GregorianCalendar.MONTH),
								now.get(GregorianCalendar.DAY_OF_MONTH), 9, 30))
				.getState());

	}

	@Test
	public void setReviewTimeSlotsAndScheduleDeliveryTestWith()
			throws IllegalAccessException, DroneNotFoundException, TimeslotUnvailableException {
		drone.setFlightTime(79);

		try {
			schedule.scheduleDelivery(new GregorianCalendar(now.get(GregorianCalendar.YEAR),
					now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH), 8,
					15), delivery1);
			fail();
		} catch (TimeslotUnvailableException e) {
			assertTrue("test passed", true);
		}

		assertEquals(12, drone.getTimeSlots().stream()
				.filter(timeSlot -> timeSlot.getState() == TimeState.REVIEW).count());

		// 4 Review
		assertEquals(TimeState.REVIEW, schedule
				.findTimeSlotAtDate(drone.getTimeSlots(),
						new GregorianCalendar(now.get(GregorianCalendar.YEAR),
								now.get(GregorianCalendar.MONTH),
								now.get(GregorianCalendar.DAY_OF_MONTH), 8, 15))
				.getState());
		assertEquals(TimeState.REVIEW, schedule
				.findTimeSlotAtDate(drone.getTimeSlots(),
						new GregorianCalendar(now.get(GregorianCalendar.YEAR),
								now.get(GregorianCalendar.MONTH),
								now.get(GregorianCalendar.DAY_OF_MONTH), 11, 00))
				.getState());

		assertTrue(schedule.scheduleDelivery(new GregorianCalendar(now.get(GregorianCalendar.YEAR),
				now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH), 11, 15),
				delivery2));

		// Delivery 10:30
		assertEquals(TimeState.DELIVERY, schedule
				.findTimeSlotAtDate(drone.getTimeSlots(),
						new GregorianCalendar(now.get(GregorianCalendar.YEAR),
								now.get(GregorianCalendar.MONTH),
								now.get(GregorianCalendar.DAY_OF_MONTH), 11, 15))
				.getState());

	}

	@Test
	public void getIndexFromDateTest() {
		GregorianCalendar date = new GregorianCalendar(now.get(GregorianCalendar.YEAR),
				now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH),
				ScheduleBean.STARTING_HOUR, 45);
		assertEquals(3, schedule.getIndexFromDate(date));

	}

	@Test
	public void getDateFromIndexTest() {
		GregorianCalendar date = new GregorianCalendar(now.get(GregorianCalendar.YEAR),
				now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH),
				ScheduleBean.STARTING_HOUR, 45);
		assertEquals(date.get(GregorianCalendar.HOUR),
				schedule.getDateFromIndex(3).get(GregorianCalendar.HOUR));
		assertEquals(date.get(GregorianCalendar.MINUTE),
				schedule.getDateFromIndex(3).get(GregorianCalendar.MINUTE));

	}

}
