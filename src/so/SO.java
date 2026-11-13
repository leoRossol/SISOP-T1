package so;

import hardware.HW;

// ------------------- S O - reune as rotinas do sistema operacional
public class SO {
    public InterruptHandling ih;
    public SysCallHandling sc;
    public Utilities utils;
    public GM gm;                         // [T1A] gerente de memória
    public Disco disco;                   // [T2B] armazenamento das paginas fora da RAM
    public GP gp;                         // [T1B] gerente de processos
    public Console console;               // [T2A]

    public SO(HW hw, int tamPg) {         // [T1A] recebe tamanho de página
        gm = new GM(hw.mem.pos.length, tamPg); // [T1A] cria o GM
        disco = new Disco(hw, hw.mem.pos.length * 10, tamPg); // [T2B] disco com 10 vezes a RAM
        utils = new Utilities(hw, gm);         // [T1A] Utilities recebe o GM para fazer a carga
        gp = new GP(gm, disco, utils, hw);         // [T2B] GP tambem gerencia paginas no disco
        disco.setGP(gp);
        disco.start();                           // [T2B] o disco atende pedidos em outra thread
        console = new Console(hw, tamPg, gp);            // [T2A] recebe o gp para acordar a CPU ociosa
        console.start();                         // [T2A] o console (dispositivo) liga junto com o SO
        ih = new InterruptHandling(hw, gp); // rotinas de tratamento de int
        sc = new SysCallHandling(hw, gp, console); // chamadas de sistema
        hw.cpu.setAddressOfHandlers(ih, sc);
    }
}
