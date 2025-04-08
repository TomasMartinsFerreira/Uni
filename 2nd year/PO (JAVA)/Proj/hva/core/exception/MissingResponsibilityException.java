package hva.core.exception;

public class MissingResponsibilityException extends Exception {
    private String _speciesId;


    public MissingResponsibilityException(String speciesId){
        _speciesId = speciesId;
    }

    public String getSpecies(){
        return _speciesId;
    }
}
