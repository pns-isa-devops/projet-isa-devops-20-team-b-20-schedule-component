package fr.polytech.schedule.components;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateful;

import fr.polytech.entities.Delivery;
import fr.polytech.entities.Drone;
import fr.polytech.entities.TimeSlot;
import fr.polytech.entities.TimeState;
import fr.polytech.warehouse.components.DeliveryModifier;

@LocalBean
@Stateful
public class ScheduleBean implements DeliveryOrganizer, DeliveryScheduler {

    @EJB
    private DeliveryModifier deliveryModifier;

    private Drone drone;
    public final static int DURING_15_MIN = 15 * 60 * 1000;

    @Override
    public Delivery getNextDelivery() {
        List<Delivery> deliveries = drone.getTimeSlots().stream()
                .filter(timeSlot -> timeSlot.getDate().after(new GregorianCalendar())).map(TimeSlot::getDelivery)
                .collect(Collectors.toList());
        if (!deliveries.isEmpty()) {
            return deliveries.get(0);
        } else {
            return null;
        }
    }

    @Override
    public boolean scheduleDelivery(GregorianCalendar date, Delivery delivery) {
        // Stage 1 : Check that the asked timeslot is available
        if (!dateIsAvailable(date))
            return false;

        // Stage 2 : Set the timeslot

        createDeliveryTimeSlot(date, delivery);

        // Stage 3 : Remove CHARGING and UNAVAILABLE slots in order to obtain only
        // DELIVERY timeslots

        Set<TimeSlot> timeslots = getTimeSlotsWithOnlyDeliveries();

        // Stage 3.1 : Set back the CHARGING time slots
        setChargingTimeSlots(timeslots);
        // Stage 3.2 : Set back UNAVAILABLE time slots
        setUnavailableTimeSlots(timeslots);

        setNewSchedule(drone, timeslots);

        delivery.setDrone(drone);

        return true;
    }

    /**
     * Return all deliveries timeslots
     *
     * @return Set
     */
    public Set<TimeSlot> getTimeSlotsWithOnlyDeliveries() {
        return new TreeSet<>(drone.getTimeSlots().stream().filter(ts -> ts.getState().equals(TimeState.DELIVERY))
                .collect(Collectors.toSet()));
    }

    /**
     * Check if the date can be use for a delivery
     *
     * @param date
     * @return boolean
     */
    public boolean dateIsAvailable(GregorianCalendar date) {
        for (TimeSlot ts : drone.getTimeSlots()) {
            if (ts.getDate().equals(date))
                return false;
        }
        return true;
    }

    /**
     * Create a time slot for delivery
     *
     * @param date
     * @param delivery
     */
    public void createDeliveryTimeSlot(GregorianCalendar date, Delivery delivery) {
        TimeSlot timeslot = new TimeSlot();
        timeslot.setDelivery(delivery);
        timeslot.setDate(date);
        timeslot.setState(TimeState.DELIVERY);
        drone.getTimeSlots().add(timeslot);
    }

    /**
     * Create a time slot
     */
    public void createTimeSlot(GregorianCalendar date, TimeState state) {
        TimeSlot timeslot = new TimeSlot();
        timeslot.setDate(date);
        timeslot.setState(state);
        drone.getTimeSlots().add(timeslot);
    }

    /**
     * Take a set of time slot and add CHARGING time slots where the drone needs
     * charge
     *
     * @param timeslots
     */
    public void setChargingTimeSlots(Set<TimeSlot> timeslots) {

        int count = 0;
        for (TimeSlot ts : timeslots) {
            if (ts.getState() == TimeState.DELIVERY) {
                count++;
                if (count % 2 == 0) {
                    TimeSlot chargingTs = new TimeSlot();
                    GregorianCalendar c = new GregorianCalendar();
                    c.setTimeInMillis(ts.getDate().getTimeInMillis() + DURING_15_MIN);
                    chargingTs.setDate(c);
                    chargingTs.setState(TimeState.CHARGING);
                    timeslots.add(chargingTs);
                }
            }
        }
    }

    /**
     * Take a set of time slot and add UNAVAILABLE time slots where it's impossible
     * to schedule a delivery
     *
     * @param timeslots
     */
    public void setUnavailableTimeSlots(Set<TimeSlot> timeslots) {
        List<TimeSlot> tss = new ArrayList<>(timeslots);
        TimeSlot first = tss.get(0);
        for (int i = 0; i < tss.size(); i++) {
            TimeSlot next;
            do {
                i++;
                if (i >= tss.size())
                    return;
                next = tss.get(i);
            } while (next.getState() != TimeState.DELIVERY);

            if (next.getDate().getTimeInMillis() - first.getDate().getTimeInMillis() < 2 * DURING_15_MIN) {
                TimeSlot ts = new TimeSlot();
                ts.setState(TimeState.UNAVAILABLE);
                GregorianCalendar c = new GregorianCalendar();
                c.setTimeInMillis(first.getDate().getTimeInMillis() - DURING_15_MIN);
                ts.setDate(c);
                timeslots.add(ts);
            } else if (next.getDate().getTimeInMillis() - first.getDate().getTimeInMillis() < 3 * DURING_15_MIN) {
                TimeSlot ts = new TimeSlot();
                ts.setState(TimeState.UNAVAILABLE);
                GregorianCalendar c = new GregorianCalendar();
                c.setTimeInMillis(first.getDate().getTimeInMillis() + DURING_15_MIN);
                ts.setDate(c);
                timeslots.add(ts);
            }
            first = next;
        }
    }

    @PostConstruct
    /**
     * Init the drone API on localhost
     */
    public void initDrone() {
        drone = new Drone();
    }

    public Drone getDrone() {
        return drone;
    }

    public void setNewSchedule(Drone drone, Set<TimeSlot> timeslots) {
        drone.setTimeSlots(timeslots);
    }
}
