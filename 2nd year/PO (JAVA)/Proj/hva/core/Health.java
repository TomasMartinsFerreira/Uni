package hva.core;


public enum Health{
    NORMAL, CONFUSÃO, ACIDENTE, ERRO;

    static Health assignHealth(int damage){
        if(damage == 0) return CONFUSÃO;
        else if ( 4 >= damage) return ACIDENTE;
        else return ERRO;
    }
}
