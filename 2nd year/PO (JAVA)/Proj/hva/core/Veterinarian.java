package hva.core;

/**
 * 'Veterinarian' class extends 'Employee' and represents a specific type of employee. 
 */
class Veterinarian extends Employee{

    // Constructor to initialize 'Veterinarian' using the 'Employee' constructor.
    Veterinarian(String employeeId, String name) {
        super(employeeId, name, new VeterinarianSatisfactionStrategy());
    }

    // Override 'get_type' to return the type "VET" for Veterinarian.
    @Override
    public String get_type() {
        return "VET";
    }
}

