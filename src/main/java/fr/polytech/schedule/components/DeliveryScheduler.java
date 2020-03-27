package fr.polytech.schedule.components;

import java.util.GregorianCalendar;

import javax.ejb.Local;

import fr.polytech.entities.Delivery;

@Local
public interface DeliveryScheduler {
    /**
     * ask and update <class>Schedule</class> if it can assign a new delivery to
     * schedule
     *
     * @param date     of delivery
     * @param delivery id
     * @return if scheduling a delivery to this hour is possible
     */
    boolean scheduleDelivery(GregorianCalendar date, Delivery delivery);
}
