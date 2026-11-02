package hardware;

public enum Interrupts {           // possiveis interrupcoes que esta CPU gera
    noInterrupt, intEnderecoInvalido, intInstrucaoInvalida, intOverflow, intTempo, intIO; // [T1C] intTempo: fim da fatia (relógio), [T2A] intIO
}
