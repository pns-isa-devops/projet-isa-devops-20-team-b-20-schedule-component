package fr.polytech.schedule.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;
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
import fr.polytech.schedule.exception.NoFreeDroneAtThisTimeSlotException;
import fr.polytech.schedule.exception.OutOfWorkingHourTimeSlotException;
import fr.polytech.schedule.exception.OutsideOfDeliveryHoursException;
import fr.polytech.schedule.exception.TimeslotUnvailableException;
import fr.polytech.schedule.exception.ZeroDronesInWarehouseException;

@Stateless
@LocalBean
@Named("schedule")
public class ScheduleBean implements DeliveryOrganizer, DeliveryScheduler {

    private static final Logger log = Logger.getLogger(ScheduleBean.class.getName());

    public static final int STARTING_HOUR = 8;
    public static final int CLOSING_HOUR = 18;
    public static final int NUMBER_OF_SLOT_PER_DAYS = 40; // end of days 18h

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Delivery getNextDelivery(GregorianCalendar date) throws ZeroDronesInWarehouseException {
        List<Drone> drones;
        drones = getAllDrones();
        List<TimeSlot> timeslots = new ArrayList<>();

        for (Drone drone : drones) {
            timeslots.addAll(drone.getTimeSlots());
        }

        List<Delivery> deliveries = timeslots.stream().filter(timeSlot -> timeSlot.getState() == TimeState.DELIVERY)
                .filter(timeSlot -> timeSlot.getDate().after(date)).map(TimeSlot::getDelivery)
                .collect(Collectors.toList());

        if (!deliveries.isEmpty()) {
            return deliveries.get(0);
        }
        return null;
    }

    public Drone getFreeDrone(GregorianCalendar date)
            throws ZeroDronesInWarehouseException, NoFreeDroneAtThisTimeSlotException {
        List<Drone> drones = this.getAllDrones();
        for (Drone d : drones) {
            if (dateIsAvailable(date, d) == TimeState.AVAILABLE) {
                return d;
            }
        }
        throw new NoFreeDroneAtThisTimeSlotException();
    }

    private List<Drone> getAllDrones() throws ZeroDronesInWarehouseException {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Drone> criteria = builder.createQuery(Drone.class);
        Root<Drone> root = criteria.from(Drone.class);
        criteria.select(root);

        TypedQuery<Drone> query = entityManager.createQuery(criteria);
        try {
            List<Drone> drones = query.getResultList();
            if (drones.isEmpty()) {
                throw new ZeroDronesInWarehouseException();
            }
            return drones;
        } catch (NoResultException e) {
            throw new ZeroDronesInWarehouseException();
        }
    }

    @Override

    public boolean scheduleDelivery(GregorianCalendar date, Delivery delivery) throws ZeroDronesInWarehouseException,
            NoFreeDroneAtThisTimeSlotException, OutsideOfDeliveryHoursException, TimeslotUnvailableException {

        delivery = entityManager.merge(delivery);
        Drone drone = getFreeDrone(date);

        // If not initialized
        if (drone.getTimeSlots().isEmpty()) {
            initDailyTimeSlots(drone);
        }

        if (date.get(GregorianCalendar.HOUR) < STARTING_HOUR || date.get(GregorianCalendar.HOUR) >= CLOSING_HOUR) {
            throw new OutsideOfDeliveryHoursException(STARTING_HOUR, CLOSING_HOUR);
        }

        // Stage 1 : Check that the asked timeslot is available
        checkAvailability(date, drone);

        // Stage 2 : Set the timeslot
        createDeliveryTimeSlot(date, delivery, drone);

        // UPDATE THE PLANNING - - - - - - - - - - - - - - - - - - -
        List<TimeState> timeStates = convertTimeSlotsToList(drone.getTimeSlots());

        int index = getIndexFromDate(date);

        for (int i = 0; i < timeStates.size(); i++) {
            if (i == index) {
                for (; i < timeStates.size() && timeStates.get(i) != TimeState.RESERVED_FOR_CHARGE
                        && timeStates.get(i) != TimeState.CHARGING; i++)
                    ;
                for (; i < timeStates.size() && timeStates.get(i) == TimeState.RESERVED_FOR_CHARGE; i++) {
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

    @Override
    public List<TimeState> getCurrentPlanning(String droneID)
            throws DroneNotFoundException, ZeroDronesInWarehouseException {
        for (Drone drone : this.getAllDrones()) {
            if (drone.getDroneId().equals(droneID)) {
                return convertTimeSlotsToList(drone.getTimeSlots()).stream()
                        .map(e -> e == null ? TimeState.AVAILABLE : e).collect(Collectors.toList());
            }
        }
        throw new DroneNotFoundException(droneID);

    }

    /**
     * Check if the date can be use for a delivery
     *
     * @param date
     * @return boolean
     */
    public TimeState dateIsAvailable(GregorianCalendar date, Drone drone) {
        for (TimeSlot ts : drone.getTimeSlots()) {
            if (ts.getDate().get(Calendar.HOUR_OF_DAY) == date.get(Calendar.HOUR_OF_DAY)
                    && ts.getDate().get(Calendar.MINUTE) == date.get(Calendar.MINUTE))
                return ts.getState();
        }
        return TimeState.AVAILABLE;
    }

    /**
     * Create a time slot for delivery
     *
     * @param date
     * @param delivery
     * @throws OutOfWorkingHourTimeSlotException
     */
    public void createDeliveryTimeSlot(GregorianCalendar date, Delivery delivery, Drone drone)
            throws TimeslotUnvailableException {
        checkAvailability(date, drone);
        delivery = entityManager.merge(delivery);
        drone = entityManager.merge(drone);
        TimeSlot timeSlot = new TimeSlot(date, TimeState.DELIVERY);
        // timeSlot.setDrone(drone);
        timeSlot.setDelivery(delivery);
        entityManager.persist(timeSlot);
        drone.add(timeSlot);
        entityManager.persist(drone);
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
    private void createTimeSlot(GregorianCalendar date, Drone drone, TimeState timeState) {
        drone = entityManager.merge(drone);
        TimeSlot timeSlot = new TimeSlot();
        timeSlot.setDate(date);
        timeSlot.setState(timeState);
        // timeSlot.setDrone(drone);
        entityManager.persist(timeSlot);
        drone.getTimeSlots().add(timeSlot);
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

    private void checkAvailability(GregorianCalendar date, Drone drone) throws TimeslotUnvailableException {
        TimeState ts = dateIsAvailable(date, drone);
        if (ts != TimeState.AVAILABLE) {
            String time = date.get(GregorianCalendar.HOUR) + ":" + date.get(GregorianCalendar.MINUTE);
            throw new TimeslotUnvailableException(time, ts.toString());
        }
    }

    private void initDailyTimeSlots(Drone drone) {
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
                    createTimeSlot(getDateFromIndex(i), drone, TimeState.RESERVED_FOR_CHARGE);
                }
                droneNeedsCharge = 2;
            } else {
                // It's available so probably a delivery
                droneNeedsCharge--;
                droneNeedsReview--;
            }
        }
    }

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
        long millisFromStartingDay = (long) index * 15 * 60 * 1000;
        long millisNow = startingDay.getTimeInMillis() + millisFromStartingDay;
        GregorianCalendar date = new GregorianCalendar();
        date.setTimeInMillis(millisNow);
        return date;
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

    /**
     * Convert list timeslots to list timestate to simply algo
     *
     * @param timeslots
     * @return list of timestate
     */
    private List<TimeState> convertTimeSlotsToList(List<TimeSlot> timeslots) {
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
     * Get delivery from a date (used by convertListToTimeSlots)
     *
     * @param timeslots
     * @param date
     * @return delivery
     */
    public TimeSlot findTimeSlotAtDate(List<TimeSlot> timeslots, GregorianCalendar date) {
        for (TimeSlot ts : timeslots) {
            if (ts.getDate().compareTo(date) == 0)
                return ts;
        }
        return null;
    }

}
