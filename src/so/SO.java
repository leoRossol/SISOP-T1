package so;

import hardware.HW;

// ------------------- S O - reune as rotinas do sistema operacional
public class SO {
    public InterruptHandling ih;
    public SysCallHandling sc;
    public Utilities utils;
    public GM gm;                         // [T1A] gerente de memória
    public GP gp;

    public SO(HW hw, int tamPg) {         // [T1A] recebe tamanho de página
        ih = new InterruptHandling(hw); // rotinas de tratamento de int
        sc = new SysCallHandling(hw); // chamadas de sistema
        hw.cpu.setAddressOfHandlers(ih, sc);
        
        gm = new GM(hw.mem.pos.length, tamPg); // [T1A] cria o GM
        utils = new Utilities(hw, gm);         // [T1A] Utilities recebe o GM para fazer a carga
        gp = new GP(gm, utils);
    }
}
