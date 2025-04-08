package hva.core;

interface Subject {
    void attach(Observer observer);
    void detach(Observer oserver);
    void notifyObservers();
}
