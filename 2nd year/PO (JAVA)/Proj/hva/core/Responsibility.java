package hva.core; /* esta classe deve ser uma abstract class */

interface Responsibility /*Template pattern*/ {

    void addWorker();

    void removeWorker();
    
    default int getWorkload(){
        return getTotalWorkload() / getWorkerAmount();
    }

    int getTotalWorkload();

    int getWorkerAmount();

    String get_id();
}
