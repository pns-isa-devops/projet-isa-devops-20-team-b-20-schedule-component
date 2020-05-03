package fr.polytech.schedule.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.ejb.LocalBean;
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
import fr.polytech.schedule.exception.DroneNotFoundException;
import fr.polytech.schedule.exception.TimeslotUnvailableException;

@Stateless
@LocalBean
@Named("schedule")
public class ScheduleBean implements DeliveryOrganizer, DeliveryScheduler {

    public final static int FIFTEEN_MINUTES_DURATION = 15 * 60 * 1000;

    private static final Logger log = Logger.getLogger(Logger.class.getName());
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Delivery getNextDelivery() throws DroneNotFoundException {
        Drone drone;
        Optional<Drone> d = this.findById("000");
        if (d.isPresent()) {
            drone = d.get();
        } else {
            throw new DroneNotFoundException("000");
        }

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
    public boolean scheduleDelivery(GregorianCalendar date, Delivery delivery)
            throws DroneNotFoundException, TimeslotUnvailableException {
        delivery = entityManager.merge(delivery);
        Drone drone;
        Optional<Drone> d = this.findById("000");
        if (d.isPresent()) {
            drone = d.get();
        } else {
            throw new DroneNotFoundException("000");
        }

        // If not initialized
        if (drone.getTimeSlots().size() == 0) {
            initDailyTimeSlots(drone);
        }

        // Stage 1 : Check that the asked timeslot is available
        if (!dateIsAvailable(date, drone))
            throw new TimeslotUnvailableException(date.toString());

        // Stage 2 : Set the timeslot

        createDeliveryTimeSlot(date, delivery, drone);

        // UPDATE THE PLANNING - - - - - - - - - - - - - - - - - - -
        List<TimeState> timeStates = convertTimeSlotsToList(drone.getTimeSlots());

        int index = getIndexFromDate(date);
        for (int i = 0; i < timeStates.size(); i++) {
            if (i == index) {
                for (; i < timeStates.size() && timeStates.get(i) != TimeState.UNAVAILABLE; i++)
                    ;
                for (; i < timeStates.size() && timeStates.get(i) == TimeState.UNAVAILABLE; i++) {
                    TimeSlot ts = findTimeSlotAtDate(drone.getTimeSlots(), getDateFromIndex(i));
                    ts = entityManager.merge(ts);
                    ts.setState(TimeState.CHARGING);
                }
                break;
            }

        }
        // END UPDATE THE PLANNING - - - - - - - - - - - - - - - - - - -
        drone = entityManager.merge(drone);
        delivery.setDrone(drone);
        return true;
    }

    /**
     * Return all deliveries timeslots
     *
     * @return Set
     */
    public Set<TimeSlot> getTimeSlotsWithOnlyDeliveries(Drone drone) {
        return new TreeSet<>(drone.getTimeSlots().stream().filter(ts -> ts.getState().equals(TimeState.DELIVERY))
                .collect(Collectors.toSet()));
    }

    /**
     * Check if the date can be use for a delivery TODO refactor pour que la méthode
     * cherche dans TOUS les drones.
     *
     * @param date
     * @return boolean
     */
    public boolean dateIsAvailable(GregorianCalendar date, Drone drone) {
        for (TimeSlot ts : drone.getTimeSlots()) {
            if (ts.getDate().get(Calendar.HOUR_OF_DAY) == date.get(Calendar.HOUR_OF_DAY)
                    && ts.getDate().get(Calendar.MINUTE) == date.get(Calendar.MINUTE))
                return false;
        }
        return true;
    }

    /**
     * Create a time slot for delivery
     *
     * @param date
     * @param delivery
     * @throws TimeslotUnvailableException
     */
    public void createDeliveryTimeSlot(GregorianCalendar date, Delivery delivery, Drone drone)
            throws TimeslotUnvailableException {
        if (!this.dateIsAvailable(date, drone)) {
            throw new TimeslotUnvailableException(date.toString());
        }
        delivery = entityManager.merge(delivery);
        drone = entityManager.merge(drone);
        TimeSlot timeSlot = new TimeSlot(date, TimeState.DELIVERY);
        timeSlot.setDrone(drone);
        timeSlot.setDelivery(delivery);
        entityManager.persist(timeSlot);
        drone.add(timeSlot);
    }

    /**
     * Creates a charging time slot.
     */
    public void createChargingTimeSlot(GregorianCalendar date, Drone drone) {
        createTimeSlot(date, drone, TimeState.CHARGING);
    }

    /**
     * Creates a charging time slot.
     */
    public void createTimeSlot(GregorianCalendar date, Drone drone, TimeState timeState) {
        drone = entityManager.merge(drone);
        TimeSlot timeSlot = new TimeSlot();
        timeSlot.setDate(date);
        timeSlot.setState(timeState);
        timeSlot.setDrone(drone);
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
            ts = entityManager.merge(ts);
            if (ts.getState() == TimeState.DELIVERY) {
                count++;
                if (count % 2 == 0) {
                    TimeSlot chargingTs = new TimeSlot();
                    GregorianCalendar c = new GregorianCalendar();
                    c.setTimeInMillis(ts.getDate().getTimeInMillis() + FIFTEEN_MINUTES_DURATION);
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

            if (next.getDate().getTimeInMillis() - first.getDate().getTimeInMillis() < 2 * FIFTEEN_MINUTES_DURATION) {
                TimeSlot ts = new TimeSlot();
                ts.setState(TimeState.UNAVAILABLE);
                GregorianCalendar c = new GregorianCalendar();
                c.setTimeInMillis(first.getDate().getTimeInMillis() - FIFTEEN_MINUTES_DURATION);
                ts.setDate(c);
                timeslots.add(ts);
            } else if (next.getDate().getTimeInMillis() - first.getDate().getTimeInMillis() < 3
                    * FIFTEEN_MINUTES_DURATION) {
                TimeSlot ts = new TimeSlot();
                ts.setState(TimeState.UNAVAILABLE);
                GregorianCalendar c = new GregorianCalendar();
                c.setTimeInMillis(first.getDate().getTimeInMillis() + FIFTEEN_MINUTES_DURATION);
                ts.setDate(c);
                timeslots.add(ts);
            }
            first = next;
        }
    }

    public Optional<Drone> findById(String id) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Drone> criteria = builder.createQuery(Drone.class);
        Root<Drone> root = criteria.from(Drone.class);
        criteria.select(root).where(builder.equal(root.get("droneId"), id));

        TypedQuery<Drone> query = entityManager.createQuery(criteria);
        try {
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            log.log(Level.FINEST, "No result for [" + id + "]", e);
            return Optional.empty();
        }
    }

    public void setNewSchedule(Drone drone, Set<TimeSlot> timeslots) {
        drone = entityManager.merge(drone);
        for (TimeSlot timeSlot : timeslots) {
            timeSlot = entityManager.merge(timeSlot);
            drone.add(timeSlot);
        }
    }

    public void initDailyTimeSlots(Drone drone) {
        // Check if a review is required
        int droneNeedsReview = 80 - drone.getFlightTime();
        boolean reviewScheduled = false;
        int droneNeedsCharge = 3;

        // 3h review = 12 timeslots
        for (int i = 0; i < NUMBER_OF_SLOT_PER_DAYS; i++) {
            if (droneNeedsReview == 0 && !reviewScheduled) {
                for (int j = 0; j < 12 && i < NUMBER_OF_SLOT_PER_DAYS; j++, i++) {
                    createTimeSlot(getDateFromIndex(i), drone, TimeState.REVIEW);
                }
                reviewScheduled = true;
            } else if (droneNeedsCharge == 0) {
                for (int k = 0; k < 4 && i < NUMBER_OF_SLOT_PER_DAYS; k++, i++) {
                    createTimeSlot(getDateFromIndex(i), drone, TimeState.UNAVAILABLE);
                    System.out.println(i);
                }
                droneNeedsCharge = 2;
            } else {
                // It's available so probably a delivery
                droneNeedsCharge--;
                droneNeedsReview--;
            }
        }
    }

    /** NEW ALGO */

    public static final int STARTING_HOUR = 8;
    public static final int NUMBER_OF_SLOT_PER_DAYS = 40; // end of days 18h

    /**
     * Get date from slot's index, it's today date
     *
     * @param index
     * @return date
     */
    public GregorianCalendar getDateFromIndex(int index) {
        GregorianCalendar now = new GregorianCalendar();
        GregorianCalendar startingDay = new GregorianCalendar(now.get(GregorianCalendar.YEAR),
                now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH), STARTING_HOUR, 0);
        long millisFromStartingDay = index * 15 * 60 * 1000;
        long millisNow = startingDay.getTimeInMillis() + millisFromStartingDay;
        GregorianCalendar date = new GregorianCalendar();
        date.setTimeInMillis(millisNow);
        return date;
    }

    /**
     * Convert list timeslots to list timestate to simply algo
     *
     * @param timeslots
     * @return list of timestate
     */
    public List<TimeState> convertTimeSlotsToList(Set<TimeSlot> timeslots) {
        List<TimeSlot> timeslots2 = new ArrayList<>(timeslots);
        TimeState[] schedule = new TimeState[NUMBER_OF_SLOT_PER_DAYS];
        Arrays.fill(schedule, null);
        for (int i = 0; i < timeslots2.size(); i++) {
            int index = getIndexFromDate(timeslots2.get(i).getDate());
            schedule[index] = timeslots2.get(i).getState();
        }
        return Arrays.asList(schedule);
    }

    /**
     * Convert list of timestate to list of timeslots timeslotsOriginal is the
     * timeslot list with deliveries to get it to insert it Can use
     * getTimeSlotsWithOnlyDeliveries
     *
     * @param schedule
     * @param timeslotsOriginal
     * @return list of timeslots
     */
    public Set<TimeSlot> convertListToTimeSlots(List<TimeState> schedule, Set<TimeSlot> timeslotsOriginal) {
        Set<TimeSlot> timeSlots = new HashSet<>();
        for (int i = 0; i < schedule.size(); i++) {
            if (schedule.get(i) != null) {
                TimeSlot timeSlot = new TimeSlot(getDateFromIndex(i), schedule.get(i));
                if (schedule.get(i) == TimeState.DELIVERY) {
                    Delivery delivery = findTimeSlotAtDate(timeslotsOriginal, timeSlot.getDate()).getDelivery();
                    timeSlot.setDelivery(delivery);
                }
                timeSlots.add(timeSlot);
            }
        }
        return timeSlots;
    }

    /**
     * Get delivery from a date (used by convertListToTimeSlots)
     *
     * @param timeslots
     * @param date
     * @return delivery
     */
    public TimeSlot findTimeSlotAtDate(Set<TimeSlot> timeslots, GregorianCalendar date) {
        for (TimeSlot ts : timeslots) {
            if (ts.getDate().compareTo(date) == 0)
                return ts;
        }
        return null;
    }

    /**
     * Get slot's index of Date
     *
     * @param date
     * @return index
     */
    public int getIndexFromDate(GregorianCalendar date) {
        GregorianCalendar startingDay = new GregorianCalendar(date.get(GregorianCalendar.YEAR),
                date.get(GregorianCalendar.MONTH), date.get(GregorianCalendar.DAY_OF_MONTH), STARTING_HOUR, 0);

        long startingMillis = startingDay.getTimeInMillis();
        long dateMillis = date.getTimeInMillis();

        long index = (dateMillis - startingMillis) / 1000 / 60 / 15;
        return (int) index;
    }

}
