// [T1C] Criado pelo grupo - thread do escalonador
package so;

import hardware.HW;

public class EscalonadorThread extends Thread {
    private GP gp;
    private HW hw;
    private volatile boolean ativa;

    public EscalonadorThread(GP _gp, HW _hw) {
        gp = _gp;
        hw = _hw;
        ativa = true;
    }

    public void pararThread() {
        ativa = false;
        interrupt();
    }

    @Override
    public void run() {
        while (ativa) {
            if (!gp.esperaProcessoDisponivel()) {
                break;
            }

            if (!ativa) {
                break;
            }

            gp.escalona();
            hw.cpu.run();        
        }
    }
}