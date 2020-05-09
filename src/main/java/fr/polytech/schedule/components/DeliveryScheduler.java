package fr.polytech.schedule.components;

import java.util.GregorianCalendar;

import javax.ejb.Local;
import java.util.List;

import fr.polytech.entities.Delivery;
import fr.polytech.entities.TimeState;
import fr.polytech.schedule.exception.DroneNotFoundException;
import fr.polytech.schedule.exception.OutsideOfDeliveryHoursException;
import fr.polytech.schedule.exception.TimeslotUnvailableException;
import fr.polytech.schedule.exception.ZeroDronesInWarehouseException;
import fr.polytech.schedule.exception.NoFreeDroneAtThisTimeSlotException;

@Local
public interface DeliveryScheduler {
        /**
         * ask and update <class>Schedule</class> if it can assign a new delivery to
         * schedule
         *
         * @param date     of delivery
         * @param delivery id
         * @return if scheduling a delivery to this hour is possible
         * @throws NoFreeDroneAtThisTimeSlotException
         * @throws ZeroDronesInWarehouseException
         * @throws DroneNotFoundException
         * @throws OutOfWorkingHourTimeSlotException
         * @throws OutsideOfDeliveryHoursException
         * @throws TimeslotUnvailableException
         */
        boolean scheduleDelivery(GregorianCalendar date, Delivery delivery)
                        throws ZeroDronesInWarehouseException, NoFreeDroneAtThisTimeSlotException,
                        OutsideOfDeliveryHoursException, TimeslotUnvailableException;

        public List<TimeState> getCurrentPlanning(String droneID)
                        throws DroneNotFoundException, ZeroDronesInWarehouseException;
}
