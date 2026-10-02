// PUCRS - Escola Politécnica - Sistemas Operacionais
// Prof. Fernando Dotti
// Código fornecido como parte da solução do projeto de Sistemas Operacionais
//
// Estrutura deste código (organizado em pacotes):
//    hardware/  definição de HW:
//           Memory, Word,
//           CPU tem Opcodes (codigos de operacoes suportadas na cpu),
//               e Interrupcoes possíveis, define o que executa para cada instrucao
//           HW - a máquina virtual é uma instanciação de CPU e Memória
//    so/        definições de SW (sistema operacional):
//           InterruptHandling e SysCallHandling (rotinas de tratamento),
//           Utilities (carga, início de execução e dump de memória),
//           GM (gerente de memória),
//           SO reúne as rotinas acima
//    programas/ os programas existentes, que podem ser copiados em memória.
//           Isto representa programas armazenados.
//    Sistema (este arquivo) instancia o Sistema com os elementos mencionados acima.
//           em seguida solicita a execução de algum programa com  loadAndExec

import hardware.HW;
import programas.Programs;
import so.SO;

public class Sistema {

    // ------------------- S I S T E M A
    // --------------------------------------------------------------------

    public HW hw;
    public SO so;
    public Programs progs;

    public Sistema(int tamMem, int tamPg) { // [T1A] recebe tamanho de página
        hw = new HW(tamMem, tamPg);    // memoria do HW tem tamMem palavras  // [T1A] repassa tamPg para o HW
        so = new SO(hw, tamPg);        // [T1A] repassa tamanho de página para o SO
        hw.cpu.setUtilities(so.utils); // permite cpu fazer dump de memoria ao avancar
        progs = new Programs();
    }

    public void run() {

        so.utils.loadAndExec(progs.retrieveProgram("fatorialV2"));

        // so.utils.loadAndExec(progs.retrieveProgram("fatorial"));
        // fibonacci10,
        // fibonacci10v2,
        // progMinimo,
        // fatorialWRITE, // saida
        // fibonacciREAD, // entrada
        // PB
        // PC, // bubble sort
    }
    // ------------------- S I S T E M A - fim
    // --------------------------------------------------------------

    // -------------------------------------------------------------------------------------------------------
    // ------------------- instancia e testa sistema
    public static void main(String args[]) {
        Sistema s = new Sistema(1024, 8); // [T1A] memória de 1024 palavras, página de 8
        s.run();
    }
}
