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

            // [T2A] CPU ociosa: se um IO terminou enquanto ninguém rodava, ninguém viu a intIO.
            // Trata o aviso aqui (bloqueado -> pronto) antes de escalonar.
            if (hw.cpu.temSinalIO()) {
                gp.desbloqueiaProcesso(hw.cpu.pegaSinalIO());
            }

            gp.escalona();
            // [T1C] só roda a CPU se o escalona() pôs um processo nela; com a fila vazia,
            // o run() executaria o contexto velho (STOP de processo já liberado) em loop
            if (gp.temRodando()) {
                hw.cpu.run();
            }
        }
    }
}