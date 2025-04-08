package hva.core;

import hva.core.exception.*;
import java.util.*;

/** 
 * The 'Employee' class is abstract and represents 1 Employee
 */ 
abstract class Employee extends Identifier{
    protected final List<Responsibility> _responsibilities;
    protected SatisfactionStrategy _satisfactionStrategy;

    Employee(String employeeId, String name, SatisfactionStrategy satisfactionStrategy) {
        super(employeeId, name);
        _responsibilities = new ResponsibilityList<>(); 
        _satisfactionStrategy = satisfactionStrategy;
    }


    void addResponsibility(Responsibility responsibility) throws DuplicateResponsibilityException{
        try{
            _responsibilities.add(responsibility);
            responsibility.addWorker();
        } catch(DuplicateKeyException e){
            throw new DuplicateResponsibilityException();
        }
    }

    void removeResponsibility(String responsibilityId) throws MissingResponsibilityException{
        Responsibility responsibility = get_responsibility(responsibilityId);
        _responsibilities.remove(responsibility);
        responsibility.removeWorker();
    }

    // Abstract method that must be implemented by subclasses.
    // The method will return the type of employee, which depends on the specific subclass.
    abstract String get_type();

    Responsibility get_responsibility(String responsibilityId) throws MissingResponsibilityException {
        for (int i = 0; i < _responsibilities.size(); i++)
            if (_responsibilities.get(i).get_id().equals(responsibilityId))
                return _responsibilities.get(i);
        throw new MissingResponsibilityException(responsibilityId);
    }

    int getSatisfaction(){
        return _satisfactionStrategy.getSatisfaction(_responsibilities);
    }

    // Override the 'toString' method to provide a string representation of the Employee object.
    @Override
    public String toString() {
        if (_responsibilities.isEmpty()) {
            return get_type() + "|" + get_id() + "|" + get_name();
        }
        List<String> responsibilityIds = new ArrayList<>();
        for (Responsibility res : _responsibilities) {
            responsibilityIds.add(res.get_id()); 
        }
        Collections.sort(responsibilityIds);
        return get_type() + "|" + get_id() + "|" + get_name() + "|" + String.join(",", responsibilityIds);
    }
}
