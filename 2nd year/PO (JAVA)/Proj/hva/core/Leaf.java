package hva.core;

//better solution than states, if its necessary to add functionality that depends on type and season it can be done trough the leaf enum, not the tree itself
enum Leaf {
    CADUCA, PERENE;
    
    private static final Cycle[][] _CYCLES = {
        {Cycle.GERARFOLHAS, Cycle.COMFOLHAS, Cycle.LARGARFOLHAS, Cycle.SEMFOLHAS},
        {Cycle.GERARFOLHAS, Cycle.COMFOLHAS, Cycle.COMFOLHAS, Cycle.LARGARFOLHAS}
    };
    private static final int[][] _EFFORT = {
        {1,2,5,0},
        {1,1,1,2}
    };

    int getSeasonalEffort(Season season){
        return _EFFORT[this.ordinal()][season.ordinal()];
    }

    Cycle getCycle(Season season){
        return _CYCLES[this.ordinal()][season.ordinal()];
    }
}
