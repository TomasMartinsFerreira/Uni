package hva.core;

// 'Keeper' class extends 'Employee' and represents a specific type of employee.
class Keeper extends Employee {

    // Constructor to initialize 'Keeper' using 'Employee' constructor.
    Keeper(String employeeId, String name) {
        super(employeeId, name, new KeeperSatisfactionStrategy());
    }

    // Override 'get_type' to return the type "TRT" for Keeper.
    @Override
    public String get_type() {
        return "TRT";
    }
}

