package hva.core;

enum Influence {
    NEU(0), POS(20), NEG(-20);
    final int _value;

    Influence(int value){
        _value = value;
    }

    int get_value() {
        return _value;
    }
}
