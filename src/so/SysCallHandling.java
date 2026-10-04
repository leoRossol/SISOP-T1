package so;

import hardware.HW;

// ------------------- C H A M A D A S   D E   S I S T E M A - rotinas de tratamento
public class SysCallHandling {
    private HW hw; // referencia ao hw se tiver que setar algo
    private GP gp;

    public SysCallHandling(HW _hw, GP _gp) {
        hw = _hw;
        gp = _gp;
    }

    public void stop() { // chamada de sistema indicando final de programa
        System.out.println("                                               SYSCALL STOP");
        gp.terminaProcesso(); // [T1C]
    }

    public void handle() { // chamada de sistema
        // suporta somente IO, com parametros
        // reg[8] = in ou out    e reg[9] endereco do inteiro
        System.out.println("SYSCALL pars:  " + hw.cpu.reg[8] + " / " + hw.cpu.reg[9]);

        if  (hw.cpu.reg[8]==1){
            // leitura ...

        } else if (hw.cpu.reg[8]==2){
            // escrita - escreve o conteuodo da memoria na posicao dada em reg[9]
            int fis = hw.cpu.traduz(hw.cpu.reg[9]);           // [T1A] reg[9] é endereço lógico
            if (fis >= 0) {                                    // [T1A]
                System.out.println("OUT:   "+ hw.mem.pos[fis].p);
            }

        } else {System.out.println("  PARAMETRO INVALIDO"); }
    }
}
