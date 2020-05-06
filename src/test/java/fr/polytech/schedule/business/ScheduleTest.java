package fr.polytech.schedule.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.GregorianCalendar;

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
import fr.polytech.schedule.exception.DroneNotFoundException;
import fr.polytech.schedule.exception.TimeslotUnvailableException;

@RunWith(Arquillian.class)
@Transactional(TransactionMode.DISABLED)
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
        public void init() throws Exception {
                utx.begin();
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
                utx.commit();
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

        // Fonctionnel
        @Test
        public void scheduleDeliveryTest() throws DroneNotFoundException, TimeslotUnvailableException,
                        Exception {
                utx.begin();
                GregorianCalendar tomorrow = new GregorianCalendar();
                tomorrow.setTimeInMillis(now.getTimeInMillis() + 24l*60l*60l*1000l);
                GregorianCalendar c = new GregorianCalendar(tomorrow.get(GregorianCalendar.YEAR),
                tomorrow.get(GregorianCalendar.MONTH), tomorrow.get(GregorianCalendar.DAY_OF_MONTH), 8, 0);
                deliveryScheduler.scheduleDelivery(c, delivery1);
                utx.commit();
                utx.begin();
                Delivery next = deliveryOrganizer.getNextDelivery();
                utx.commit();
                assertEquals(delivery1, next);
        }

        @Test
        public void getNextDeliveriesTest() throws DroneNotFoundException, TimeslotUnvailableException, Exception {
                utx.begin();
                assertNull(deliveryOrganizer.getNextDelivery());
                assertTrue(deliveryScheduler.scheduleDelivery(new GregorianCalendar(), delivery1));
                assertNull(deliveryOrganizer.getNextDelivery());
                utx.commit();
        }

        /**
         * Tests DateIsAvailable
         */
        @Test
        public void dateIsAvailableTestWithNothing() throws Exception {
                utx.begin();
                assertTrue(schedule.dateIsAvailable(new GregorianCalendar(now.get(GregorianCalendar.YEAR),
                                now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH), 8, 15),
                                drone));
                utx.commit();
        }

        @Test
        public void dateIsAvailableTestWithOneDeliveryBefore()
                        throws IllegalAccessException, TimeslotUnvailableException, Exception {
                                utx.begin();
                schedule.createDeliveryTimeSlot(new GregorianCalendar(now.get(GregorianCalendar.YEAR),
                                now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH), 8, 0),
                                delivery1, drone);
                assertTrue(schedule.dateIsAvailable(new GregorianCalendar(now.get(GregorianCalendar.YEAR),
                                now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH), 8, 15),
                                drone));
                                utx.commit();
        }

        @Test
        public void dateIsAvailableTestWithOneDeliveryAtSameTime()
                        throws IllegalAccessException, TimeslotUnvailableException, Exception {
                                utx.begin();
                schedule.createDeliveryTimeSlot(new GregorianCalendar(now.get(GregorianCalendar.YEAR),
                                now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH), 8, 15),
                                delivery1, drone);
                assertFalse(schedule.dateIsAvailable(new GregorianCalendar(now.get(GregorianCalendar.YEAR),
                                now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH), 8, 15),
                                drone));
                                utx.commit();
        }

        @Test
        public void dateIsAvailableTestWithOneTimeSlotBefore() throws Exception {
                utx.begin();
                schedule.createChargingTimeSlot(new GregorianCalendar(now.get(GregorianCalendar.YEAR),
                                now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH), 8, 0),
                                drone);
                assertTrue(schedule.dateIsAvailable(new GregorianCalendar(now.get(GregorianCalendar.YEAR),
                                now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH), 8, 15),
                                drone));
                                utx.commit();
        }

        @Test
        public void dateIsAvailableTestWithOneTimeSlotAtSameTime() throws Exception {
                schedule.createChargingTimeSlot(new GregorianCalendar(now.get(GregorianCalendar.YEAR),
                                now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH), 8, 15),
                                drone);
                assertFalse(schedule.dateIsAvailable(new GregorianCalendar(now.get(GregorianCalendar.YEAR),
                                now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH), 8, 15),
                                drone));
                                utx.commit();
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
                        throws IllegalAccessException, DroneNotFoundException, TimeslotUnvailableException, Exception {
                                utx.begin();
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
                System.out.println(schedule.findTimeSlotAtDate(drone.getTimeSlots(),
                                new GregorianCalendar(now.get(GregorianCalendar.YEAR), now.get(GregorianCalendar.MONTH),
                                                now.get(GregorianCalendar.DAY_OF_MONTH), 8, 45)));
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
                                utx.commit();

        }

        @Test
        public void setReviewTimeSlotsAndScheduleDeliveryTestWith()
                        throws IllegalAccessException, DroneNotFoundException, TimeslotUnvailableException, Exception {
                                utx.begin();
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
                                utx.commit();

        }

        @Test
        public void getIndexFromDateTest() throws Exception {
                utx.begin();
                GregorianCalendar date = new GregorianCalendar(now.get(GregorianCalendar.YEAR),
                                now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH),
                                ScheduleBean.STARTING_HOUR, 45);
                assertEquals(3, schedule.getIndexFromDate(date));
                utx.commit();

        }

        @Test
        public void getDateFromIndexTest() throws Exception {
                utx.begin();
                GregorianCalendar date = new GregorianCalendar(now.get(GregorianCalendar.YEAR),
                                now.get(GregorianCalendar.MONTH), now.get(GregorianCalendar.DAY_OF_MONTH),
                                ScheduleBean.STARTING_HOUR, 45);
                assertEquals(date.get(GregorianCalendar.HOUR),
                                schedule.getDateFromIndex(3).get(GregorianCalendar.HOUR));
                assertEquals(date.get(GregorianCalendar.MINUTE),
                                schedule.getDateFromIndex(3).get(GregorianCalendar.MINUTE));
                                utx.commit();

        }

}
