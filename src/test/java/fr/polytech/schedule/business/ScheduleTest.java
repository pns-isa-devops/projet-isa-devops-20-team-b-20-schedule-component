package fr.polytech.schedule.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Set;

import javax.ejb.EJB;
import javax.inject.Inject;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

import arquillian.AbstractScheduleTest;
import fr.polytech.entities.Delivery;
import fr.polytech.entities.TimeSlot;
import fr.polytech.entities.TimeState;
import fr.polytech.schedule.components.DeliveryOrganizer;
import fr.polytech.schedule.components.DeliveryScheduler;
import fr.polytech.schedule.components.ScheduleBean;

@RunWith(Arquillian.class)
public class ScheduleTest extends AbstractScheduleTest {

    @EJB(name = "schedule")
    private DeliveryScheduler deliveryScheduler;

    @EJB(name = "schedule")
    private DeliveryOrganizer deliveryOrganizer;

    @Inject
    private ScheduleBean schedule;

    @Test // Fonctionnel
    public void scheduleDeliveryTestWithNothing() {
        Delivery delivery = new Delivery();
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
    public void dateIsAvailableTestWithOneDeliveryBefore() {
        schedule.createDeliveryTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 0), new Delivery());
        assertTrue(schedule.dateIsAvailable(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 15)));
    }

    @Test
    public void dateIsAvailableTestWithOneDeliveryAtSameTime() {
        schedule.createDeliveryTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 15), new Delivery());
        assertFalse(schedule.dateIsAvailable(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 15)));
    }

    @Test
    public void dateIsAvailableTestWithOneTimeSlotBefore() {
        schedule.createTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 0), TimeState.CHARGING);
        assertTrue(schedule.dateIsAvailable(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 15)));
    }

    @Test
    public void dateIsAvailableTestWithOneTimeSlotAtSameTime() {
        schedule.createTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 15), TimeState.CHARGING);
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
    public void getTimeSlotsWithOnlyDeliveriesWithDeliveries() {
        schedule.createDeliveryTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 0), new Delivery());
        schedule.createDeliveryTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 15), new Delivery());
        schedule.createDeliveryTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 30), new Delivery());

        assertEquals(3, schedule.getTimeSlotsWithOnlyDeliveries().size());
    }

    @Test
    public void getNextDeliveriesTest() {

        assertNull(deliveryOrganizer.getNextDelivery());
        assertTrue(deliveryScheduler.scheduleDelivery(new GregorianCalendar(), new Delivery()));
        assertNull(deliveryOrganizer.getNextDelivery());

    }

    @Test
    public void getTimeSlotsWithOnlyDeliveriesWithDeliveryAndOther() {
        schedule.createTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 0), TimeState.REVIEW);
        schedule.createTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 30), TimeState.CHARGING);
        schedule.createDeliveryTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 15), new Delivery());

        assertEquals(1, schedule.getTimeSlotsWithOnlyDeliveries().size());
    }

    /**
     * Tests getTimeSlotsWithOnlyDeliveries
     */
    @Test
    public void setChargingTimeSlotsTestWithOneDeliveries() {
        schedule.createDeliveryTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 15), new Delivery());
        Set<TimeSlot> ts = schedule.getTimeSlotsWithOnlyDeliveries();
        assertEquals(1, ts.size());
        schedule.setChargingTimeSlots(ts);
        schedule.setNewSchedule(schedule.getDrone(), ts);
        assertEquals(1, ts.size());
    }

    @Test
    public void setChargingTimeSlotsTestWithTwoDeliveries1() {
        schedule.createDeliveryTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 0), new Delivery());
        schedule.createDeliveryTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 15), new Delivery());
        Set<TimeSlot> ts = schedule.getTimeSlotsWithOnlyDeliveries();
        assertEquals(2, ts.size());
        schedule.setChargingTimeSlots(ts);
        assertEquals(3, ts.size());
        schedule.setNewSchedule(schedule.getDrone(), ts);
        assertFalse(schedule.dateIsAvailable(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 30)));
        assertTrue(schedule.dateIsAvailable(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 45)));
    }

    @Test
    public void setChargingTimeSlotsTestWithTwoDeliveries2() {
        schedule.createDeliveryTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 0), new Delivery());
        schedule.createDeliveryTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 30), new Delivery());
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
    public void setUnavailableTimeSlotsTestsWithTwoDeliveries1() {
        schedule.createDeliveryTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 15), new Delivery());
        schedule.createDeliveryTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 30), new Delivery());
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
    public void setUnavailableTimeSlotsTestsWithTwoDeliveries2() {
        schedule.createDeliveryTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 15), new Delivery());
        schedule.createDeliveryTimeSlot(new GregorianCalendar(2001, Calendar.JANUARY, 2, 8, 45), new Delivery());
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
