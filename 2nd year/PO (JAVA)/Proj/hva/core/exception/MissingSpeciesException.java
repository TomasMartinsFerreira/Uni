package hva.core.exception;

public class MissingSpeciesException extends Exception{

    private String _speciesId;

    public MissingSpeciesException(String speciesId){
        _speciesId = speciesId;
    }

    public String getSpecies(){
        return _speciesId;
    }
}
