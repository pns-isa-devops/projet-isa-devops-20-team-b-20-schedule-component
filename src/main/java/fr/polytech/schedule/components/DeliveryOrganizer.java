package fr.polytech.schedule.components;

import javax.ejb.Local;

import fr.polytech.entities.Delivery;
import fr.polytech.schedule.exception.DroneNotFoundException;
import fr.polytech.schedule.exception.ZeroDronesInWarehouseException;

import java.util.GregorianCalendar;

@Local
public interface DeliveryOrganizer {

    /**
     * Returns the next closest delivery given a date
     *
     * @param date
     * @return
     * @throws DroneNotFoundException
     * @throws ZeroDronesInWarehouseException
     */
    Delivery getNextDelivery(GregorianCalendar date) throws ZeroDronesInWarehouseException;

}
