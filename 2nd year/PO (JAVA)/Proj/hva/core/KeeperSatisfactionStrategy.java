package hva.core;

import java.util.*;

class KeeperSatisfactionStrategy implements SatisfactionStrategy {

    @Override
    public int getSatisfaction(List<Responsibility> responsibilities){
        float workload = 0;
        for(int i = 0; i< responsibilities.size(); i++)
            workload += responsibilities.get(i).getWorkload();
        return Math.round(300 - workload);
    }
}
