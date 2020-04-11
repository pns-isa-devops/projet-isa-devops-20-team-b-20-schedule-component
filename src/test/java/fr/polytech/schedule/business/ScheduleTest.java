package fr.polytech.schedule.business;

import arquillian.AbstractScheduleTest;
import fr.polytech.entities.Delivery;
import fr.polytech.entities.Parcel;
import fr.polytech.entities.TimeSlot;
import fr.polytech.schedule.components.DeliveryOrganizer;
import fr.polytech.schedule.components.DeliveryScheduler;
import fr.polytech.schedule.components.ScheduleBean;
import fr.polytech.warehouse.utils.CarrierAPI;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Set;

import static org.junit.Assert.*;

//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class ScheduleTest extends AbstractScheduleTest {

    @EJB(name = "schedule")
    private DeliveryScheduler deliveryScheduler;

    @EJB(name = "schedule")
    private DeliveryOrganizer deliveryOrganizer;

    @Inject
    private ScheduleBean schedule;

    @PersistenceContext
    private EntityManager entityManager;

    private Delivery delivery1;
    private Delivery delivery2;
    private Delivery delivery3;

    @Before
    public void init() {
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

    @Test
        // Fonctionnel
    public void scheduleDeliveryTestWithNothing() {
        Delivery delivery = delivery1;
        GregorianCalendar c = new GregorianCalendar();
        c.setTimeInMillis(c.getTimeInMillis() + 1000);
        schedule.scheduleDelivery(c, delivery);
        Delivery next = schedule.getNextDelivery();
        assertEquals(delivery, next);
    }

    /**
     * Tests DateIsAvailable
     */
    @Test
    public void dateIsAvailableTestWithNothing() {
        assertTrue(schedule.dateIsAvailable(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 15)));
    }

    @Test
    public void dateIsAvailableTestWithOneDeliveryBefore() throws IllegalAccessException {
        schedule.createDeliveryTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 0), delivery1);
        assertTrue(schedule.dateIsAvailable(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 15)));
    }

    @Test
    public void dateIsAvailableTestWithOneDeliveryAtSameTime() throws IllegalAccessException {
        schedule.createDeliveryTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 15), delivery1);
        assertFalse(schedule.dateIsAvailable(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 15)));
    }

    @Test
    public void dateIsAvailableTestWithOneTimeSlotBefore() {
        schedule.createChargingTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 0));
        assertTrue(schedule.dateIsAvailable(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 15)));
    }

    @Test
    public void dateIsAvailableTestWithOneTimeSlotAtSameTime() {
        schedule.createChargingTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 15));
        assertFalse(schedule.dateIsAvailable(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 15)));
    }

    /**
     * Tests getTimeSlotsWithOnlyDeliveries
     */
    @Test
    public void getTimeSlotsWithOnlyDeliveriesEmpty() {
        assertEquals(0, schedule.getTimeSlotsWithOnlyDeliveries().size());
    }

    @Test
    public void getTimeSlotsWithOnlyDeliveriesWithDeliveries() throws IllegalAccessException {
        schedule.createDeliveryTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 0), delivery1);
        schedule.createDeliveryTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 15), delivery2);
        schedule.createDeliveryTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 30), delivery3);

        assertEquals(3, schedule.getTimeSlotsWithOnlyDeliveries().size());
    }

    @Test
    public void getNextDeliveriesTest() {

        assertNull(deliveryOrganizer.getNextDelivery());
        assertTrue(deliveryScheduler.scheduleDelivery(new GregorianCalendar(), delivery1));
        assertNull(deliveryOrganizer.getNextDelivery());

    }

    @Test
    public void getTimeSlotsWithOnlyDeliveriesWithDeliveryAndOther() throws IllegalAccessException {
        schedule.createChargingTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 0));
        schedule.createChargingTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 30));
        schedule.createDeliveryTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 15), delivery1);

        assertEquals(1, schedule.getTimeSlotsWithOnlyDeliveries().size());
    }

    /**
     * Tests getTimeSlotsWithOnlyDeliveries
     */
    @Test
    public void setChargingTimeSlotsTestWithOneDeliveries() throws IllegalAccessException {
        schedule.createDeliveryTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 15), delivery1);
        Set<TimeSlot> ts = schedule.getTimeSlotsWithOnlyDeliveries();
        assertEquals(1, ts.size());
        schedule.setChargingTimeSlots(ts);
        schedule.setNewSchedule(schedule.getDrone(), ts);
        assertEquals(1, ts.size());
    }

    @Test
    public void setChargingTimeSlotsTestWithTwoDeliveries1() throws IllegalAccessException {
        schedule.createDeliveryTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 0), delivery1);
        schedule.createDeliveryTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 15), delivery2);
        Set<TimeSlot> ts = schedule.getTimeSlotsWithOnlyDeliveries();
        assertEquals(2, ts.size());
        schedule.setChargingTimeSlots(ts);
        assertEquals(3, ts.size());
        schedule.setNewSchedule(schedule.getDrone(), ts);
        assertFalse(schedule.dateIsAvailable(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 30)));
        assertTrue(schedule.dateIsAvailable(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 45)));
    }

    @Test
    public void setChargingTimeSlotsTestWithTwoDeliveries2() throws IllegalAccessException {
        schedule.createDeliveryTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 0), delivery1);
        schedule.createDeliveryTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 30), delivery2);
        Set<TimeSlot> ts = schedule.getTimeSlotsWithOnlyDeliveries();
        assertEquals(2, ts.size());
        schedule.setChargingTimeSlots(ts);
        assertEquals(3, ts.size());
        schedule.setNewSchedule(schedule.getDrone(), ts);
        assertFalse(schedule.dateIsAvailable(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 45)));
        assertTrue(schedule.dateIsAvailable(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 15)));
    }

    /**
     * Tests getTimeSlotsWithOnlyDeliveries
     */
    @Test
    public void setUnavailableTimeSlotsTestsWithTwoDeliveries1() throws IllegalAccessException {
        schedule.createDeliveryTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 15), delivery1);
        schedule.createDeliveryTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 30), delivery2);
        Set<TimeSlot> ts = schedule.getTimeSlotsWithOnlyDeliveries();
        assertEquals(2, ts.size());
        schedule.setChargingTimeSlots(ts);
        schedule.setUnavailableTimeSlots(ts);
        assertEquals(4, ts.size());
        schedule.setNewSchedule(schedule.getDrone(), ts);
        assertFalse(schedule.dateIsAvailable(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 45)));
        assertFalse(schedule.dateIsAvailable(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 0)));

    }

    @Test
    public void setUnavailableTimeSlotsTestsWithTwoDeliveries2() throws IllegalAccessException {
        schedule.createDeliveryTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 15), delivery1);
        schedule.createDeliveryTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 45), delivery2);
        Set<TimeSlot> ts = schedule.getTimeSlotsWithOnlyDeliveries();
        assertEquals(2, ts.size());
        schedule.setChargingTimeSlots(ts);
        schedule.setUnavailableTimeSlots(ts);
        assertEquals(4, ts.size());
        schedule.setNewSchedule(schedule.getDrone(), ts);
        assertFalse(schedule.dateIsAvailable(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 30)));
        assertFalse(schedule.dateIsAvailable(new GregorianCalendar(2001, Calendar.JANUARY, 2, 9, 0)));

    }

}
