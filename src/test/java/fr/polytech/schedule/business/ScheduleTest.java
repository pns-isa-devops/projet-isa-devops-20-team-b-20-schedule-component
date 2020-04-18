package fr.polytech.schedule.business;

import arquillian.AbstractScheduleTest;
import fr.polytech.entities.Delivery;
import fr.polytech.entities.Drone;
import fr.polytech.entities.Parcel;
import fr.polytech.entities.TimeSlot;
import fr.polytech.schedule.components.DeliveryOrganizer;
import fr.polytech.schedule.components.DeliveryScheduler;
import fr.polytech.schedule.components.ScheduleBean;
import fr.polytech.schedule.exception.DroneNotFoundException;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.UserTransaction;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.*;

//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.runner.RunWith;

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

    private Drone drone;

    private Delivery delivery1;
    private Delivery delivery2;
    private Delivery delivery3;

    @Before
    public void init() {
        entityManager.persist(new Drone("000"));

        Optional<Drone> d = schedule.findById("000");
        if (d.isPresent()) {
            this.drone = d.get();
        } else {
            throw new DroneNotFoundException("The drone 000 has not been found.");
        }

        Parcel parcel = new Parcel("AAAAAAAAA1", "address", "carrier", "Dupond");
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
        Drone drone;
        utx.begin();
        //Drone dddd = entityManager.merge(this.drone); //merge does the same thing as find by id
        //System.out.println("\n-----------------\n\n" + dddd + "\n\n---------------\n");
        Optional<Drone> d = schedule.findById("000");
        if (d.isPresent()) {
            System.out.println("\n-----------------" + d.get() + "\n\n---------------\n");
            drone = d.get();
            entityManager.refresh(drone);
            entityManager.remove(drone);
        }
        this.drone = null;
        utx.commit();

/*      utx.begin();
        entityManager.merge(delivery1);
        entityManager.remove(delivery1);
        delivery1 = null;
        utx.commit();

        utx.begin();
        entityManager.merge(delivery2);
        entityManager.remove(delivery2);
        delivery2 = null;
        utx.commit();

        utx.begin();
        entityManager.merge(delivery3);
        entityManager.remove(delivery3);
        delivery3 = null;
        utx.commit();*/
    }

    @Test
    // Fonctionnel
    public void scheduleDeliveryTestWithNothing() {
        assertTrue(true);
        GregorianCalendar c = new GregorianCalendar();
        c.setTimeInMillis(c.getTimeInMillis() + 1000);
        schedule.scheduleDelivery(c, delivery1);
        Delivery next = schedule.getNextDelivery();
        assertEquals(delivery1, next);
    }

    /**
     * Tests DateIsAvailable
     */
    @Test
    public void dateIsAvailableTestWithNothing() {
        assertTrue(schedule.dateIsAvailable(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 15), drone));
    }


    @Test
    public void dateIsAvailableTestWithOneDeliveryBefore() throws IllegalAccessException {
        schedule.createDeliveryTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 0), delivery1, drone);
        assertTrue(schedule.dateIsAvailable(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 15), drone));
    }

    @Test
    public void dateIsAvailableTestWithOneDeliveryAtSameTime() throws IllegalAccessException {
        schedule.createDeliveryTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 15), delivery1, drone);
        assertFalse(schedule.dateIsAvailable(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 15), drone));
    }

    @Test
    public void dateIsAvailableTestWithOneTimeSlotBefore() {
        schedule.createChargingTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 0), drone);
        assertTrue(schedule.dateIsAvailable(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 15), drone));
    }

    @Test
    public void dateIsAvailableTestWithOneTimeSlotAtSameTime() {
        schedule.createChargingTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 15), drone);
        assertFalse(schedule.dateIsAvailable(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 15), drone));
    }

    /**
     * Tests getTimeSlotsWithOnlyDeliveries
     **/
    @Test
    public void getTimeSlotsWithOnlyDeliveriesEmpty() {
        assertEquals(0, schedule.getTimeSlotsWithOnlyDeliveries(drone).size());
    }

    @Test
    public void getTimeSlotsWithOnlyDeliveriesWithDeliveries() throws IllegalAccessException {
        schedule.createDeliveryTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 0), delivery1, drone);
        schedule.createDeliveryTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 15), delivery2, drone);
        schedule.createDeliveryTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 30), delivery3, drone);

        assertEquals(3, schedule.getTimeSlotsWithOnlyDeliveries(drone).size());
    }

    @Test
    public void getNextDeliveriesTest() {

        assertNull(deliveryOrganizer.getNextDelivery());
        assertTrue(deliveryScheduler.scheduleDelivery(new GregorianCalendar(), delivery1));
        assertNull(deliveryOrganizer.getNextDelivery());
    }

    @Test
    public void getTimeSlotsWithOnlyDeliveriesWithDeliveryAndOther() throws IllegalAccessException {
        schedule.createChargingTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 0), drone);
        schedule.createChargingTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 30), drone);
        schedule.createDeliveryTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 15), delivery1, drone);

        assertEquals(1, schedule.getTimeSlotsWithOnlyDeliveries(drone).size());
    }

    /**
     * Tests getTimeSlotsWithOnlyDeliveries
     */
    @Test
    public void setChargingTimeSlotsTestWithOneDeliveries() throws IllegalAccessException {
        schedule.createDeliveryTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 15), delivery1, drone);
        Set<TimeSlot> ts = schedule.getTimeSlotsWithOnlyDeliveries(drone);
        assertEquals(1, ts.size());
        schedule.setChargingTimeSlots(ts);
        schedule.setNewSchedule(this.drone, ts);
        assertEquals(1, ts.size());
    }

    @Test
    public void setChargingTimeSlotsTestWithTwoDeliveries1() throws IllegalAccessException {
        schedule.createDeliveryTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 0), delivery1, drone);
        schedule.createDeliveryTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 15), delivery2, drone);
        Set<TimeSlot> ts = schedule.getTimeSlotsWithOnlyDeliveries(drone);
        assertEquals(2, ts.size());
        schedule.setChargingTimeSlots(ts);
        assertEquals(3, ts.size());
        schedule.setNewSchedule(this.drone, ts);
        assertFalse(schedule.dateIsAvailable(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 30), drone));
        assertTrue(schedule.dateIsAvailable(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 45), drone));
    }

    @Test
    public void setChargingTimeSlotsTestWithTwoDeliveries2() throws IllegalAccessException {
        schedule.createDeliveryTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 0), delivery1, drone);
        schedule.createDeliveryTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 30), delivery2, drone);
        Set<TimeSlot> ts = schedule.getTimeSlotsWithOnlyDeliveries(drone);
        assertEquals(2, ts.size());
        schedule.setChargingTimeSlots(ts);
        assertEquals(3, ts.size());
        schedule.setNewSchedule(drone, ts);
        assertFalse(schedule.dateIsAvailable(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 45), drone));
        assertTrue(schedule.dateIsAvailable(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 15), drone));
    }


    /**
     * Tests getTimeSlotsWithOnlyDeliveries
     */
    @Test
    public void setUnavailableTimeSlotsTestsWithTwoDeliveries1() throws IllegalAccessException {
        schedule.createDeliveryTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 15), delivery1, drone);
        schedule.createDeliveryTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 30), delivery2, drone);
        Set<TimeSlot> ts = schedule.getTimeSlotsWithOnlyDeliveries(drone);
        assertEquals(2, ts.size());
        schedule.setChargingTimeSlots(ts);
        schedule.setUnavailableTimeSlots(ts);
        assertEquals(4, ts.size());
        schedule.setNewSchedule(drone, ts);
        assertFalse(schedule.dateIsAvailable(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 45), drone));
        assertFalse(schedule.dateIsAvailable(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 0), drone));
    }

    @Test
    public void setUnavailableTimeSlotsTestsWithTwoDeliveries2() throws IllegalAccessException {
        schedule.createDeliveryTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 15), delivery1, drone);
        schedule.createDeliveryTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 45), delivery2, drone);
        Set<TimeSlot> ts = schedule.getTimeSlotsWithOnlyDeliveries(drone);
        assertEquals(2, ts.size());
        schedule.setChargingTimeSlots(ts);
        schedule.setUnavailableTimeSlots(ts);
        assertEquals(4, ts.size());
        schedule.setNewSchedule(drone, ts);
        assertFalse(schedule.dateIsAvailable(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 30), drone));
        assertFalse(schedule.dateIsAvailable(new GregorianCalendar(2001, Calendar.JANUARY, 2, 9, 0), drone));
    }

}
