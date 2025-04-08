package hva.core;

import java.io.*;
import java.util.*;

interface SatisfactionStrategy extends Serializable {
    int getSatisfaction(List<Responsibility> responsibilities );
}
