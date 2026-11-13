package hardware;

public enum Interrupts {           // possiveis interrupcoes que esta CPU gera
    noInterrupt, intEnderecoInvalido, intInstrucaoInvalida, intOverflow, intTempo, intIO, intPageFault, intIOVM; // [T1C] intTempo: fim da fatia (relógio), [T2A] intIO, [T2B] pagina ausente e fim de IO do disco
}
