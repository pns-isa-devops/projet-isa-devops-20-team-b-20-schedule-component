package fr.polytech.schedule.exception;

import java.io.Serializable;

public class ZeroDronesInWarehouseException extends Exception implements Serializable {

    public ZeroDronesInWarehouseException() {
        super();
    }

    @Override
    public String getMessage() {
        return "There is no registered drone in the warehouse.";
    }

}
