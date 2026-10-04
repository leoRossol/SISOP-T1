package hardware;

public enum Interrupts {           // possiveis interrupcoes que esta CPU gera
    noInterrupt, intEnderecoInvalido, intInstrucaoInvalida, intOverflow, intTempo; // [T1C] intTempo: fim da fatia (relógio)
}
