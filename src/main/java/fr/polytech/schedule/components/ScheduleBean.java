package fr.polytech.schedule.components;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateful;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import fr.polytech.entities.Delivery;
import fr.polytech.entities.Drone;
import fr.polytech.entities.TimeSlot;
import fr.polytech.entities.TimeState;

@Stateful
@LocalBean
@Named("schedule")
public class ScheduleBean implements DeliveryOrganizer, DeliveryScheduler {

    private static final Logger log = Logger.getLogger(Logger.class.getName());

//    @EJB
//    private DeliveryModifier deliveryModifier;

    private Drone drone; // todo delete that...
    public final static int DURING_15_MIN = 15 * 60 * 1000;

    @PersistenceContext
    private EntityManager entityManager;



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

    private Optional<Drone> findById(String id) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Drone> criteria = builder.createQuery(Drone.class);
        Root<Drone> root = criteria.from(Drone.class);
        criteria.select(root).where(builder.equal(root.get("deliveryId"), id));

        TypedQuery<Drone> query = entityManager.createQuery(criteria);
        try {
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            log.log(Level.FINEST, "No result for [" + id + "]", e);
            return Optional.empty();
        }
    }

    @Override
    public boolean scheduleDelivery(GregorianCalendar date, Delivery delivery) {
        // Stage 1 : Check that the asked timeslot is available
        if (!dateIsAvailable(date))
            return false;

        // Stage 2 : Set the timeslot

        try {
            createDeliveryTimeSlot(date, delivery);
        } catch (IllegalAccessException e) {
            return false;
        }

        // Stage 3 : Remove CHARGING and UNAVAILABLE slots in order to obtain only
        // DELIVERY timeslots

        Set<TimeSlot> timeslots = getTimeSlotsWithOnlyDeliveries();

        // Stage 3.1 : Set back the CHARGING time slots
        // TODO : Remove bug concurrent access
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
            if(ts.getDate().get(Calendar.HOUR_OF_DAY) == date.get(Calendar.HOUR_OF_DAY) && ts.getDate().get(Calendar.MINUTE) == date.get(Calendar.MINUTE) )
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
    public void createDeliveryTimeSlot(GregorianCalendar date, Delivery delivery) throws IllegalAccessException {
        if(!this.dateIsAvailable(date)){
            throw new IllegalAccessException("The time slot already has a delivery.");
        }
        TimeSlot timeSlot = new TimeSlot(date, TimeState.DELIVERY);
        timeSlot.setDelivery(delivery);
        //entityManager.merge(delivery);
        entityManager.persist(timeSlot);
        drone.getTimeSlots().add(timeSlot);
    }

    /**
     * Creates a charging time slot.
     */
    public void createChargingTimeSlot(GregorianCalendar date) {
        TimeSlot timeSlot = new TimeSlot();
        timeSlot.setDate(date);
        timeSlot.setState(TimeState.CHARGING);
        entityManager.persist(timeSlot);
        drone.getTimeSlots().add(timeSlot);
    }

    /**
     * Take a set of time slot and add CHARGING time slots where the drone needs
     * charge
     *
     * @param timeSlots
     */
    public void setChargingTimeSlots(Set<TimeSlot> timeSlots) {

        int count = 0;
        for (TimeSlot ts : timeSlots) {
            if (ts.getState() == TimeState.DELIVERY) {
                count++;
                if (count % 2 == 0) {
                    TimeSlot chargingTs = new TimeSlot();
                    GregorianCalendar c = new GregorianCalendar();
                    c.setTimeInMillis(ts.getDate().getTimeInMillis() + DURING_15_MIN);
                    chargingTs.setDate(c);
                    chargingTs.setState(TimeState.CHARGING);
                    timeSlots.add(chargingTs);
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
        drone = new Drone("000");
        entityManager.persist(drone);
    }

    public Drone getDrone() {
        return drone;
    }

    public void setNewSchedule(Drone drone, Set<TimeSlot> timeslots) {
        drone.setTimeSlots(timeslots);
    }
}
