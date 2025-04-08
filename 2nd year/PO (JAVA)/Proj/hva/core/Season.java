package hva.core;


// 'Season' enum defines four seasons and includes a method to advance to the next season.
public enum Season{
    SPRING, SUMMER, AUTUMN, WINTER;


    // Method to advance to the next season in the sequence.
    Season advanceSeason() {
        Season[] seasons = Season.values();
        return seasons[(this.ordinal() + 1) % seasons.length];
    }
}

