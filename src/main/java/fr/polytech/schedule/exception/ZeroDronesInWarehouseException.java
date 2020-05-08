package fr.polytech.schedule.exception;

public class ZeroDronesInWarehouseException extends Throwable {

    public ZeroDronesInWarehouseException() {
        super();
    }

    @Override
    public String getMessage() {
        return "There is no registered drone in the warehouse.";
    }

    @Override
    public String toString() {
        return "There is no registered drone in the warehouse.";
    }

}
