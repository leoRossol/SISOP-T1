// [T1B] Criado pelo grupo - Shell: lê comandos do usuário e chama o GP, o GM e a CPU
// [T1C] + comando execAll
package so;

import hardware.HW;
import hardware.Word;
import programas.Programs;
import java.util.Scanner;

public class Shell {
    private SO so;
    private HW hw;
    private Programs progs;

    public Shell(SO _so, HW _hw, Programs _progs) {
        so = _so;
        hw = _hw;
        progs = _progs;
    }

    // loop principal: mostra o prompt, lê uma linha, executa o comando, até "exit"
    public void run() {
        Scanner teclado = new Scanner(System.in);
        System.out.println("Shell do SO. Digite 'help' para ver os comandos.");

        while (true) {
            System.out.print("> ");
            if (!teclado.hasNextLine()) { break; }       // fim da entrada 
            String linha = teclado.nextLine().trim();
            if (linha.isEmpty()) { continue; }

            String[] partes = linha.split("\\s+");     // separa por espaços: "exec 1" -> ["exec", "1"]
            String comando = partes[0];

            if (comando.equals("exit")) { break; }
            executaComando(comando, partes);
        }
        System.out.println("Fim do shell.");
    }

    private void executaComando(String comando, String[] partes) {
        switch (comando) {
            case "new":
                if (!confereArgs(partes, 1, "new <programa>")) { return; }
                // intern(): o retrieveProgram do professor compara nomes com ==, que só funciona
                // com o mesmo objeto String; intern() devolve o mesmo objeto do literal "fatorial" etc.
                Word[] programa = progs.retrieveProgram(partes[1].intern());
                if (programa == null) {
                    System.out.println("Programa '" + partes[1] + "' nao existe. Use 'progs' para ver a lista.");
                } else if (so.gp.criaProcesso(programa)) {
                    System.out.println("Processo criado com id " + so.gp.getUltimoId());
                } else {
                    System.out.println("Nao foi possivel criar o processo (sem memoria).");
                }
                break;

            case "rm":
                if (!confereArgs(partes, 1, "rm <id>")) { return; }
                Integer idRm = leNumero(partes[1]);
                if (idRm == null) { return; }
                if (so.gp.desalocaProcesso(idRm)) {
                    System.out.println("Processo " + idRm + " removido.");
                } else {
                    System.out.println("Processo com id: " + idRm + " nao encontrado");
                }
                break;

            case "ps":
                so.gp.mostraProcessos();
                break;

            case "dump":
                if (!confereArgs(partes, 1, "dump <id>")) { return; }
                Integer idDump = leNumero(partes[1]);
                if (idDump == null) { return; }
                so.gp.dumpProcesso(idDump);
                break;

            case "dumpM":
                if (!confereArgs(partes, 2, "dumpM <inicio> <fim>")) { return; }
                Integer ini = leNumero(partes[1]);
                Integer fim = leNumero(partes[2]);
                if (ini == null || fim == null) { return; }
                int tamMem = hw.mem.pos.length;
                if (ini < 0 || fim > tamMem || ini >= fim) {
                    System.out.println("Intervalo invalido. Use 0 <= inicio < fim <= " + tamMem);
                    return;
                }
                so.utils.dump(ini, fim);
                break;

            case "exec":
                if (!confereArgs(partes, 1, "exec <id>")) { return; }
                Integer idExec = leNumero(partes[1]);
                if (idExec == null) { return; }
                so.gp.executaProcesso(idExec);
                break;

            // [T1C] executa todos os processos em round robin até acabarem
            case "execAll":
                // sem processos, o escalona() só pararia a CPU, mas o run() desliga o
                // cpuStop ao começar e executaria o contexto velho que sobrou na CPU
                if (so.gp.getProcessos().isEmpty()) {
                    System.out.println("Nenhum processo para executar. Use 'new <programa>'.");
                    return;
                }
                System.out.println("execAll: " + so.gp.getProcessos().size() + " processo(s) na fila de prontos");
                so.gp.escalona();   // põe o primeiro da fila na CPU
                hw.cpu.run();       // roda até o escalonador parar a CPU (fila vazia)
                System.out.println("execAll: todos os processos terminaram");
                break;

            case "traceOn":
                hw.cpu.setDebug(true);
                System.out.println("Trace ligado.");
                break;

            case "traceOff":
                hw.cpu.setDebug(false);
                System.out.println("Trace desligado.");
                break;

            case "progs":
                System.out.print("Programas disponiveis:");
                for (programas.Program p : progs.progs) {
                    if (p != null) { System.out.print(" " + p.name); }
                }
                System.out.println();
                break;

            case "help":
                System.out.println("Comandos (troque <...> pelo valor, sem os sinais; ex.: new fatorial, exec 0):");
                System.out.println("  new <programa>        cria processo (ver 'progs')");
                System.out.println("  rm <id>               remove processo e libera a memoria");
                System.out.println("  ps                    lista os processos");
                System.out.println("  dump <id>             mostra PCB e memoria do processo");
                System.out.println("  dumpM <inicio> <fim>  mostra a memoria fisica [inicio, fim)");
                System.out.println("  exec <id>             executa so o processo <id> ate o fim (sem escalonar)"); // [T1C]
                System.out.println("  execAll               executa todos os processos em round robin"); // [T1C]
                System.out.println("  traceOn / traceOff    liga/desliga o trace da CPU");
                System.out.println("  progs                 lista os programas disponiveis");
                System.out.println("  exit                  sai");
                break;

            default:
                System.out.println("Comando desconhecido: " + comando + ". Digite 'help'.");
        }
    }

    // confere se o comando veio com a quantidade certa de argumentos
    private boolean confereArgs(String[] partes, int qtd, String uso) {
        if (partes.length != qtd + 1) {
            System.out.println("Uso: " + uso);
            return false;
        }
        return true;
    }

    // converte texto em número; devolve null (e avisa) se não for um número
    private Integer leNumero(String texto) {
        try {
            return Integer.parseInt(texto);
        } catch (NumberFormatException e) {
            System.out.println("'" + texto + "' nao e um numero.");
            return null;
        }
    }
}
