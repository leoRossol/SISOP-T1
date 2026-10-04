# Instruções para agentes de IA — SISOP-T1

Este repositório é um **trabalho de faculdade** (Sistemas Operacionais), feito em dupla.
Os estudantes estão **aprendendo** Java e SO. Você é um **consultor/tutor**, não quem escreve o trabalho.
Responda sempre em **português**, com linguagem simples.

---

## 1. Modo de trabalho (o mais importante)

O objetivo é que o estudante **entenda e escreva o código**, e consiga explicá-lo ao professor na apresentação.

**Faça:**
- Divida o trabalho em **tarefas pequenas** (10–30 min cada), uma de cada vez.
- Para cada tarefa: explique o **conceito** necessário (de Java ou de SO) e entregue um **esqueleto com `TODO`s** e perguntas-guia, não a solução.
- Aponte **exemplos que já existem no código** do professor como modelo (ex.: "veja como `Memory` cria o array dela").
- Quando pedirem revisão: **leia o arquivo atual** e comece pelo que está **certo**, depois aponte os erros um por um, explicando o **porquê**.
- Para mostrar um bug, **simule a execução** com um exemplo pequeno e concreto (valores das variáveis a cada volta do `for`). Isso ensina mais do que dizer "está errado".
- Traduza mensagens de erro do compilador para português simples.
- Incentive testar **casos de borda** (divisão exata, sobra, zero, memória cheia).
- Sugira commits pequenos a cada parte que funcionar.

**Não faça:**
- Não escreva a solução completa de uma tarefa, a não ser que o estudante **peça explicitamente** ou esteja claramente travado depois de várias tentativas. Nesse caso, dê a resposta daquele trecho **explicando linha por linha**.
- Não refatore nem "melhore" o código do estudante por conta própria. Sugira, e o estudante decide.
- Não adiante partes futuras do trabalho (PCB, escalonador, threads) enquanto a fase atual não estiver pronta.
- Não diga que algo está certo sem ter lido o arquivo.

**Exceção:** tarefas mecânicas que não fazem parte do aprendizado (configuração do VSCode, git, reorganizar arquivos, scripts de teste) podem ser feitas pelo agente, **depois de confirmar** com o estudante.

### ⚠️ Mantenha o registro do trabalho atualizado (obrigatório)

A dupla trabalha em computadores e sessões diferentes, cada um com o seu agente. **A seção 3 deste arquivo é a única memória compartilhada** entre eles. Se ela ficar desatualizada, o próximo agente vai sugerir refazer algo pronto ou pular uma etapa.

- **No início de cada sessão:** leia a seção 3 e confira se ela bate com o código (abra os arquivos citados). Se não bater, avise o estudante antes de continuar.
- **Sempre que uma etapa terminar e funcionar:** marque `[x]` no checklist da fase, na mesma hora, sem esperar o fim da sessão.
- **Sempre que surgir algo importante:** uma decisão de projeto, um problema encontrado, uma dúvida para levar ao professor ou uma resposta dele. Anote em "Decisões e observações" da fase.
- **Quando uma fase nova começar:** peça ao estudante o enunciado (PDF ou texto), preencha os requisitos e o checklist daquela fase e mude o título "Fase atual".
- **Antes de encerrar a sessão:** lembre o estudante de commitar o `AGENTS.md` junto com o código, para que a outra pessoa da dupla receba as anotações, além de reforçar que o aluno revise o que foi adicionado ao arquivo.

---

## 2. O projeto

Um **simulador de computador** em Java: uma CPU e uma memória simuladas executam programas escritos numa linguagem de máquina própria (`LDI`, `ADD`, `JMP`, `STD`...). O trabalho do semestre é construir o **sistema operacional** dessa máquina, em fases.

### Estrutura

```
src/
├── Sistema.java        main; monta HW + SO e roda um programa
├── hardware/           a "máquina física" (código do professor)
│   ├── CPU.java        ciclo fetch → execute → checa interrupção
│   ├── Memory.java, Word.java, Opcode.java, Interrupts.java, HW.java
├── so/                 o sistema operacional (onde fica o trabalho)
│   ├── GM.java         Gerente de Memória (criado pela dupla)
│   ├── Utilities.java  carga de programa, dump de memória
│   ├── InterruptHandling.java, SysCallHandling.java, SO.java
└── programas/
    └── Programs.java   programas de teste (fatorial, fibonacci, bubble sort...)
```

### Compilar e rodar

```bash
javac -d bin $(find src -name '*.java') && java -cp bin Sistema
```
No VSCode: F5 (configuração em `.vscode/launch.json`). JDK 21.

---

## 3. Fases do trabalho e estado atual

O Trabalho 1 tem três partes, feitas em sequência: **T1A → T1B → T1C**. Cada uma se apoia na anterior.

**Fase atual: T1C** (iniciada em 03/10)

### Prazo e plano

- **Apresentação:** todas as fases (T1A+B+C) na quinta, 08/10/2026.
- **Plano:** T1A até sex 02/10 · T1B sáb–dom · T1C seg–ter · revisão para a apresentação na qua.
- **Divisão do trabalho (para caber no prazo):** o estudante escreve a lógica central (PCB, GP, troca de contexto, escalonador); o agente escreve as partes mecânicas (testes, parsing do shell, comandos de dump/ps, esqueleto de threads) e explica o que fez.

---

### T1A — Gerente de Memória com paginação

Requisitos do enunciado:
- Memória com `tamMem` palavras, páginas/frames de `tamPg` palavras. Deve funcionar com **diferentes valores** de `tamMem` e `tamPg`.
- **GM**: `aloca(nroPalavras)` devolve a tabela de páginas (`int[]`, onde `tabela[i]` = frame da página i) ou `null` se não couber; `desaloca(tabela)` libera os frames. Aloca código **e** dados do programa.
- **Carga**: cada página i do programa é copiada para o frame `tabela[i]`.
- **Tradução**: durante a execução, **todo** acesso à memória converte endereço lógico → físico: `pagina = end / tamPg`, `offset = end % tamPg`, `fisico = tabela[pagina] * tamPg + offset`. Acesso fora das páginas do processo → interrupção de endereço inválido.
- **Os programas em `programas/Programs.java` NÃO podem ser alterados.**

#### Checklist

- [x] Código do professor reorganizado em pacotes (tag git `codigo-base`)
- [x] `GM`: construtor, `aloca` `desaloca`
- [x] Parametrizar `tamPg` (`Sistema`, `SO`) e criar o `GM` no `SO`
- [x] Carga paginada em `Utilities.loadProgram` (devolve a tabela de páginas; `loadAndExec` guarda ela, mas a CPU ainda não usa)
- [x] `CPU` recebe `tamPg` no construtor e a tabela de páginas no `setContext(pc, tabela)`; método `traduz(endLogico)` criado (commit `71a74db`)
- [x] Tradução na `CPU`: `traduz` usado no fetch, `LDD`, `STD`, `LDX`, `STX`, `JMPIM`, `JMPIGM`, `JMPILM`, `JMPIEM`. O `pc` continua **lógico** (jumps que não leem memória não mudam).
- [x] Tradução na syscall de escrita (`traduz` virou `public` para a `SysCallHandling` usar)
- [x] Remover `legal()` da `CPU` (ficou sem uso)
- [x] `loadAndExec`: se `loadProgram` devolver `null` (não coube), não executa
- [x] Testes: vários programas carregados ao mesmo tempo, frames **não contíguos**, vários `tamPg` (4, 8, 10, 16), memória pequena, `tamMem` não divisível. Verificação feita pelo agente fora do repositório em 02/10 (299 checagens ok); nenhum bug no código do grupo.

#### Decisões e observações

**Organização do código**
- O professor autorizou separar o código em vários arquivos/pacotes.
- Não criar `PCB`, gerente de processos nem escalonador nesta fase. A tabela de páginas é passada direto para a CPU.
- O código do grupo na `CPU` fica num bloco no topo da classe (`tabelaPaginas`, `tamPg`, `setContext`, `traduz`), para facilitar a apresentação.

**Gerente de Memória (`GM`)**
- `aloca` usa os primeiros frames livres encontrados; devolve `null` sem alterar nada quando não há frames suficientes.
- `aloca` devolve `int[]` (ou `null`) em vez do `boolean aloca(int, OUT int[])` sugerido no enunciado, porque Java não tem parâmetro de saída.

**Como o `tamPg` e a tabela chegam onde são usados**
- `tamPg` desce pelos construtores até o `GM`: `main` → `Sistema(tamMem, tamPg)` → `SO(hw, tamPg)` → `GM`. A `Utilities` recebe o `GM` e lê o tamanho com `gm.getTamPg()`.
- `tamPg` chega à CPU também pelo construtor: `Sistema` → `HW(tamMem, tamPg)` → `CPU(mem, tamPg, debug)`. Foi escolhido o construtor (e não o `setContext`) porque o tamanho da página é do hardware, igual para todos os processos.
- A tabela de páginas chega à CPU pelo `setContext(pc, tabela)`, chamado no `loadAndExec`. O `setContext(int)` antigo foi removido.
- Se o programa não couber, `loadProgram` devolve `null` e o `loadAndExec` não executa.

**Tradução na CPU**
- `traduz` devolve o endereço físico, ou liga `intEnderecoInvalido` e devolve `-1` (endereço negativo ou `pagina >= tabelaPaginas.length`). É `public` porque a syscall de escrita (`SysCallHandling`) também usa.
- Cada acesso à memória segue o padrão `fis = traduz(end); if (fis >= 0) { ... m[fis] ... }`. A variável `fis` é declarada no fetch e reaproveitada nos `case`.
- Só os jumps que **leem a memória** (`JMPIM`, `JMPIGM`, `JMPILM`, `JMPIEM`) traduzem; o valor lido vai para o `pc` sem traduzir, porque o `pc` é lógico. `JMPIM`, `JMPILM` e `JMPIEM` não validavam endereço no código original e agora validam.
- `legal()` foi removido: o `traduz` faz a mesma checagem, só que mais restrita (páginas do processo, e não a memória inteira).
- A proteção é por página: acessos às sobras da última página (ex.: endereço 21 no fatorial de 20 palavras com `tamPg` 8) são aceitos. É fragmentação interna, comportamento normal da paginação.

**Programas do professor que acessam fora da própria área** (⏳ confirmar com o professor)
- `PB` (16 palavras, acessa o endereço 50) e `PC` (54 palavras, acessa 96–99) geram `intEnderecoInvalido`. O sistema está certo ao barrar.
- `fibonacci10` tem 30 palavras (0..29), mas o último `STX` escreve no endereço 30. Com `tamPg` 4, 8 ou 16 isso cai na sobra da última página e passa; com `tamPg` 10 (3 páginas exatas) gera `intEnderecoInvalido` depois de gravar a série. Os resultados (posições 20..29) ficam certos.

**Perguntas prováveis na apresentação**
- Por que `aloca` devolve `int[]` e não `boolean`? → Java não tem parâmetro de saída; `null` faz o papel do `false`.
- Por que o `tamPg` vem pelo construtor da CPU? → é característica do hardware, não do processo.
- Por que não reaproveitar o `legal()`? → um método devolve uma coisa só; precisamos de "é válido?" **e** do endereço físico. O `traduz` responde as duas (`-1` = inválido).
- Por que só os jumps `...M` mudaram? → só eles leem a memória; os outros põem no `pc` um número que já é lógico.
- Por que o endereço 21 do fatorial é aceito? → proteção por página (fragmentação interna).

---

### T1B — Gerente de Processos (concluída)

Enunciado: `enunciados/T1-T1B.pdf`. Requisitos:
- **GP** (módulo do SO):
  - `criaProcesso(programa)`: verifica o tamanho do programa, pede memória ao GM (se não houver, retorna falso), cria o **PCB**, guarda a tabela de páginas no PCB, carrega o programa, seta id, `pc = 0` etc., coloca o PCB na fila de **prontos** e retorna verdadeiro.
  - `desalocaProcesso(id)`: desaloca toda a memória do processo, retira-o de qualquer fila e desaloca o PCB.
- **Estruturas:** PCB (um por processo); variável `rodando` (running) apontando para o PCB em execução; lista de **prontos** (ready) com PCBs.
- **Sistema interativo** (shell que espera comandos em loop): `new <programa>` (cria processo e retorna id único), `rm <id>`, `ps`, `dump <id>` (PCB + memória do processo), `dumpM <inicio> <fim>` (memória física), `exec <id>`, `traceOn`, `traceOff`, `exit`. Os nomes podem mudar, desde que façam o descrito.

#### Checklist

- [x] Criar `PCB` com id, PC, registradores, tabela de páginas, tamanho e estado
- [x] Criar `EstadoProcesso` com `PRONTO`, `RODANDO` e `BLOQUEADO`
- [x] Tornar `Utilities.loadProgram` público para permitir carga sem execução
- [x] Criar estrutura inicial do `GP` com processos, prontos, rodando e contador de IDs
- [x] Implementar `GP.criaProcesso(programa)`
- [x] Implementar busca de processo por ID
- [x] Implementar `GP.desalocaProcesso(id)` com liberação de memória
- [x] Implementar listagem básica de processos (`mostraProcessos`)
- [x] Testar criação, busca, listagem e remoção de processos
- [x] `GP.getUltimoId()` para o comando `new` mostrar o id criado
- [x] Integrar execução de processo por ID (`GP.executaProcesso(id)`)
- [x] `Shell` com `new`, `rm`, `ps`, `dump`, `dumpM`, `exec`, `traceOn`, `traceOff`, `exit` (+ `help`, `progs`); `CPU.setDebug`
- [x] `GP.dumpProcesso(id)`: dados do PCB (`Arrays.toString` para tabela e registradores) + memória página por página (`frame = tabela[i]`, `utils.dump(frame*tamPg, frame*tamPg + tamPg)`)
- [x] Testar memória insuficiente, múltiplos processos e reutilização de frames (agente, fora do repo, 03/10)

#### Decisões e observações

- O `PCB` guarda o contexto necessário para o processo: id, PC, registradores, tabela de páginas, tamanho do programa e estado.
- O estado `BLOQUEADO` foi incluído para representar processos que aguardam E/S ou outro evento; na T1B, os estados usados no fluxo básico são `PRONTO` e `RODANDO`.
- `GP.criaProcesso` carrega o programa pela `Utilities`, cria registradores zerados, cria o PCB como `PRONTO` e coloca o PCB nas listas de processos e prontos. A CPU não é executada nesse momento.
- `GP.desalocaProcesso` remove o PCB das listas e chama `GM.desaloca` para liberar os frames. IDs removidos não são reutilizados, pois `proximoId` apenas cresce.
- O método de listagem atual mostra id, estado e PC; ainda não existe shell interativo.
- `criaProcesso` continua devolvendo `boolean` (como no enunciado). Para o `new` mostrar o id, foi criado `getUltimoId()` (`proximoId - 1`; `-1` se nenhum processo foi criado). O shell só deve chamá-lo quando `criaProcesso` devolver `true`. Getter em vez de `proximoId` público para ninguém de fora alterar o contador.
- Marcações `[T1B]` adicionadas em `GP`, `PCB`, `EstadoProcesso`, `SO` e `Utilities` (03/10).
- A `CPU` não tem `setDebug`: `debug` é `private` e só é definido no construtor (`HW` passa `true`). Os comandos `traceOn`/`traceOff` vão precisar de um `setDebug(boolean)`.
- A criação, busca, listagem e remoção foram testadas em `Sistema`, com compilação via `javac` sem erros.
- Commit de referência: `573245b` (`T1B iniciado e testado`), enviado ao remoto.
- `GP` recebe o `HW` pelo construtor (`GP(gm, utils, hw)`), como `InterruptHandling`/`SysCallHandling`, e não por parâmetro do `executaProcesso`: o hardware não muda, o shell só conhece o id, e na T1C outros pontos (relógio, `STOP`) vão precisar da CPU.
- `executaProcesso(id)`: busca o PCB (avisa e devolve `false` se não existe ou se está `TERMINADO`) → `rodando = pcb`, estado `RODANDO`, sai de `prontos` → restaura contexto (`setContext(pcb.getPc(), tabela)` + cópia dos registradores) → `cpu.run()` → salva contexto (`setPc(cpu.pc)` + cópia dos registradores) → `rodando = null`, estado `TERMINADO`.
- Registradores são copiados **posição por posição** (PCB → CPU e CPU → PCB), nunca `cpu.reg = pcb.getReg()`: arrays em Java são referências, e na T1C dois PCBs acabariam dividindo o mesmo array.
- Estado `TERMINADO` adicionado ao `EstadoProcesso`. O processo terminado continua na lista `processos` (com a memória) até o `rm`, para o `dump <id>` mostrar o resultado. `exec` num processo terminado é recusado (senão a CPU continuaria depois do `STOP`).
- Teste (03/10, memória 1024, `tamPg` 8): fatorialV2 (id 0) e fibonacci10 (id 1); `exec 1` → `TERMINADO`, PC 16, série 0..55 gravada nos frames 3–6; `exec 1` de novo e `exec 7` recusados; `exec 0` → `TERMINADO`, PC 17. O `pc` salvo é o endereço do `STOP`/syscall onde a CPU parou.
- **Shell** (`so/Shell.java`, escrito pelo agente): `Sistema.run()` só faz `new Shell(so, hw, progs).run()`. O shell lê linhas com `Scanner`, separa com `split("\\s+")` e cada comando só chama métodos do `GP`/`Utilities`/`CPU`; não tem lógica de SO. Valida número de argumentos, número inválido (`exec abc`) e intervalo do `dumpM` (`0 <= ini < fim <= tamMem`, fim exclusivo).
- **Pegadinha do professor:** `Programs.retrieveProgram` compara nomes com `==` (referência). Com texto digitado no teclado isso sempre dá `null`. Como `Programs.java` não pode ser alterado, o shell usa `partes[1].intern()`, que devolve o mesmo objeto `String` do literal. (Pergunta provável: por que `intern()`?)
- `CPU.setDebug(boolean)` criado; o `HW` agora cria a CPU com `debug = false` (antes `true`), e o trace só aparece com `traceOn`.
- Teste do shell (03/10): `new`/`ps`/`exec`/`dump`/`dumpM`/`rm`/`traceOn`/`traceOff`, programa inexistente, falta de argumento, id inexistente e comando desconhecido ok. O `new progMinimo` depois de `rm 1` reaproveitou os frames liberados (o trace mostra escrita no físico 40).
- `dumpProcesso` testado no shell (03/10): `new fatorial` + `exec 0` + `dump 0` mostra 5040 (7!) no endereço 10; `dump` do fibonacci10 mostra a série nos frames certos. Os programas não imprimem nada: o resultado fica na memória, e por isso o `exec` só mostra `SYSCALL STOP` (com trace desligado). Na apresentação: `exec` → `dump`.
- Teste de memória cheia (03/10, `Sistema(64, 8)` = 8 frames, numa cópia fora do repo): 2× `new fibonacci10` ocupa os 8 frames; `new fatorial` falha ("sem memoria") e **não gasta id**; `rm 0` libera os frames 0–3 e dois `new fatorial` reaproveitam os frames 0–1 e 2–3; `exec` e `dump` corretos nos frames reaproveitados (5040 e a série). `PB` termina com `intEnderecoInvalido` (pc 1), fica `TERMINADO` e o shell continua; `PC` (54 palavras) não cabe junto com outro processo. Nenhum bug no código do grupo.
- Detalhe cosmético: quando não há memória aparecem duas mensagens (a da `Utilities.loadProgram` e a do shell).
- **T1B concluída em 03/10.** Próximo: manual de apresentação (estudante) e depois a T1C.

---

### T1C — Escalonamento (em andamento)

Enunciado: `enunciados/T1-T1C.pdf`. Requisitos:
- **Troca de contexto:** salvar o contexto da CPU no PCB quando o processo sai da CPU e restaurar quando ele volta.
- **Relógio:** a cada *delta* instruções (contador de ciclos na CPU, a forma sugerida por ser mais simples), gerar uma interrupção de tempo. A rotina de tratamento salva o contexto, coloca o processo em prontos, escolhe o próximo e restaura o contexto dele (round robin).
- **Fim de processo:** `STOP` é chamada de sistema; a rotina libera a memória, desaloca o PCB e escalona outro processo.
- **Comandos:** manter todos do T1B e adicionar `execAll` (executa de forma escalonada todos os processos em memória até acabarem; deve dar para acompanhar o escalonamento e ver os resultados na memória).
- **Funcionamento contínuo:** o escalonamento roda sozinho enquanto o usuário digita comandos. **No mínimo duas threads**: uma atende o usuário, outra escalona.
- O diagrama `enunciados/Esquema.pdf` mostra a arquitetura (versões sequencial, multithreaded e com E/S).

#### Checklist

Parte 1 — versão sequencial (`execAll`), seguindo a página "Sequencial" do `Esquema.pdf`:
- [x] **Relógio na CPU**: `intTempo` no `Interrupts`; `delta` e contador de instruções na `CPU`; a cada `delta` instruções liga `intTempo`; contador zera no `setContext`
- [x] **CPU não para em toda interrupção**: quem decide é a rotina do SO; `CPU` ganha um jeito de parar quando não há mais processos (ex.: `cpu.para()`); `case STOP` deixa de ligar `cpuStop` sozinho
- [x] **Troca de contexto no GP**: separar `salvaContexto(pcb)` e `restauraContexto(pcb)` do código que já existe no `executaProcesso`
- [x] **Escalonador**: `GP.escalona()` tira o primeiro de `prontos`, marca `RODANDO` e restaura o contexto; se `prontos` estiver vazio, `rodando = null` e a CPU para
- [x] **Rotina do timer**: `InterruptHandling` recebe o `GP`; em `intTempo` salva o contexto do `rodando`, põe no fim de `prontos` (`PRONTO`) e chama `escalona()`
- [x] **Fim de processo**: `STOP` (em `SysCallHandling.stop`) e interrupções de erro (endereço inválido, overflow, instrução inválida) finalizam o `rodando` (libera memória e PCB) e chamam `escalona()`
- [x] **Adaptar `exec <id>`**: passos 5 e 6 do `executaProcesso` **comentados** (o estudante ainda vai decidir se apaga)
- [x] **Modo `exec <id>` da T1B**: `boolean execUnico` no GP; ligado pelo `executaProcesso` antes do `run()` e desligado depois; com ele ligado, `trocaPorTempo` faz `salvaContexto` + `restauraContexto` do mesmo processo (fatia nova, zera `irpt`/contador) e `escalona` para a CPU em vez de pegar o próximo
- [x] **Comando `execAll`**: chama `escalona()` + `cpu.run()`; mensagens do escalonador mostram quem sai e quem entra
- [x] Testes do `execAll`: vários processos, `delta` pequeno e grande, processo que termina com erro, resultados na memória

Parte 2 — funcionamento contínuo com threads (página "multithreaded" do `Esquema.pdf`):
- [ ] (detalhar quando a parte 1 estiver pronta)

#### Decisões e observações

- Ordem escolhida (03/10): primeiro a versão sequencial com `execAll`, depois as threads. A versão sequencial já tem toda a lógica de escalonamento; as threads só mudam **quem** chama a CPU e o escalonador.
- **`delta`** chega pelo construtor, igual ao `tamPg`: `main` → `Sistema(tamMem, tamPg, delta)` → `HW(tamMem, tamPg, delta)` → `CPU(mem, tamPg, delta, debug)`. Valor usado: **5** (exemplo do próprio `Esquema.pdf`). Delta pequeno = muitas trocas (overhead); grande demais = vira FIFO e o escalonamento não aparece.
- **Relógio** no `run()`, depois do `switch` e antes do "VERIFICA INTERRUPÇÃO": `contadorCiclos++`; se `contadorCiclos >= delta && irpt == noInterrupt`, liga `intTempo`. O `== noInterrupt` impede que o relógio apague uma interrupção de erro da mesma instrução (primeira versão só testava `intEnderecoInvalido` e apagaria `intOverflow`/`intInstrucaoInvalida`). `>=` em vez de `==` cobre `delta <= 0`. O `setContext` zera o contador (processo novo = fatia nova).
- Teste (03/10, fora do repo): fatorial com delta 5 para com `intTempo` no pc 5 depois de exatamente 5 instruções; PB com delta 2 (erro e fim de fatia na mesma instrução) mantém `intEnderecoInvalido`; fatorial com delta 1000 chega ao `STOP` com 5040.
- **Quem para a CPU é o SO** (Tarefa 2, 03/10): `CPU.para()` (público, faz `cpuStop = true`). Saíram os dois `cpuStop = true` da CPU (no `case STOP` e depois do `ih.handle`). Por enquanto `InterruptHandling.handle` e `SysCallHandling.stop` sempre chamam `hw.cpu.para()`, então o comportamento é igual ao da Tarefa 1 (testado). Nas Tarefas 5 e 6 as rotinas passam a decidir: timer troca de processo sem parar; `STOP`/erro só param se não houver outro processo.
- Pegadinha da Tarefa 2: comentar o `case STOP` inteiro faz o `STOP` cair no `default` e virar `intInstrucaoInvalida`. O `case` tem que continuar existindo (é ele que desvia para `sysCall.stop()`).
- Efeito temporário: como o `executaProcesso` (T1B) marca `TERMINADO` sempre que o `run()` volta, um processo interrompido pelo relógio aparece como `TERMINADO` no `ps`. Resolvido na Tarefa 6 (passos 5 e 6 do `executaProcesso` comentados).
- **Troca de contexto** (Tarefa 3, 04/10): `GP.restauraContexto(pcb)` (`setContext` + cópia PCB → CPU) e `GP.salvaContexto(pcb)` (`pc` + cópia CPU → PCB), `private`, extraídos dos passos 3 e 5 do `executaProcesso`, que agora só chama os dois. Ficam no GP e não na CPU porque a CPU não conhece processos/PCB (pergunta provável). Teste: fatorial com delta 5 → PCB guarda pc 5 e regs `[7, 1, 0, 0, 0, 0, 1, 8, 0, 0]`.
- `GP.java` dividido em seções `[T1B]` e `[T1C]` (agente, 04/10); métodos da T1C ficam no fim do arquivo.
- **Escalonador** (Tarefa 4, 04/10): `GP.escalona()` **sem parâmetro**: se `prontos` vazia → `rodando = null`, `cpu.para()`, `return`; senão tira o primeiro (`get(0)` + `remove(0)`), marca `RODANDO`, `restauraContexto` e imprime `ESCALONADOR: entra processo X`. Primeira versão recebia o `PCB` por parâmetro: errado, porque quem decide o próximo é o escalonador (a rotina do timer só conhece quem sai). Pergunta provável: `remove(Object)` devolve `boolean`, `remove(int)` devolve o elemento. Ainda não testado em execução (sem chamador até a Tarefa 5).
- **Rotina do timer** (Tarefa 5, 04/10): `GP.trocaPorTempo()` (público) faz `salvaContexto(rodando)` → `PRONTO` → fim de `prontos` → mensagem → `escalona()`. `InterruptHandling` recebe o `GP` pelo construtor e, se `irpt == Interrupts.intTempo`, chama `gp.trocaPorTempo()`; senão `para()`. Escolhido um método no GP (e não deixar `salvaContexto`/`rodando` públicos) para a fila e a troca de contexto continuarem só no GP. No `SO`, `ih`/`sc`/`setAddressOfHandlers` passaram para **depois** do `gp` (o `ih` precisa dele; ninguém antes depende do `ih`). Pegadinhas: a primeira versão não chamava `salvaContexto` (processo recomeçaria do pc 0 a cada fatia, loop infinito); `if (Interrupts.intTempo)` não compila (falta o `irpt ==`).
- Teste (04/10, delta 5): `new fatorial`, `new fibonacci10`, `exec 0` → os dois se revezam a cada 5 instruções; fatorial chega a 5040 depois de ~7 trocas (prova de que a troca de contexto está certa). Final errado, **esperado até a Tarefa 6**: o `STOP` do P0 caiu na 5ª instrução da fatia → `stop()` chama `para()` **e** o relógio liga `intTempo` → `trocaPorTempo` devolve o P0 terminado à fila e põe o P1 na CPU → `run()` sai → `executaProcesso` salva o contexto do P1 no PCB do P0.
- **Fim de processo** (Tarefa 6, 04/10): `GP.terminaProcesso()` (público): `pcb = rodando` → mensagem → `salvaContexto` (para o dump mostrar pc/regs finais e não os da última troca) → `TERMINADO` → `dumpProcesso` → `desalocaProcesso` (já libera frames, tira das listas e zera `rodando`) → `escalona()`. Chamado por `SysCallHandling.stop()` (que agora recebe o `GP`) e pelo `else` do `InterruptHandling` (erros). O `TERMINADO` dura só até o `desaloca`: estado de transição, como o *zombie* do Linux (pergunta provável).
- **Bug encontrado e corrigido (Tarefa 6):** `STOP` do **último** processo na última instrução da fatia → `escalona()` para a CPU sem `setContext` (contador não zera) → relógio liga `intTempo` na mesma instrução → `trocaPorTempo` com `rodando == null` → `NullPointerException` (acontecia com `fatorial` e `fatorialV2` sozinhos, delta 5). Correção no SO (não na CPU, que é hardware): cláusula de guarda `if (rodando == null) return;` no começo do `trocaPorTempo`.
- Testes (04/10, delta 5, com `exec 0` disparando): cada programa sozinho (`fatorial`, `fatorialV2`, `progMinimo`, `fibonacci10`, `PB`, `PC`) termina com dump e sem exceção; `PB`/`PC` morrem por `intEnderecoInvalido` e liberam; fatorial + fibonacci10 + PB juntos → PB morre, os outros revezam até 5040 e 55; `ps` vazio no fim; `new` depois reaproveita a memória (id 3).
- **`execAll`** (Tarefa 7, agente, 04/10): no `Shell`, se `getProcessos()` estiver vazia avisa e não roda (senão o `run()`, que desliga o `cpuStop` ao começar, executaria o contexto velho que sobrou na CPU); senão `so.gp.escalona()` + `hw.cpu.run()`, entre as mensagens `execAll: N processo(s)...` e `execAll: todos os processos terminaram`. `help` atualizado (`exec` agora diz "a partir do processo <id>").
- Testes do `execAll` (Tarefa 8, agente, 04/10, cópia fora do repo): vazio → aviso; fatorial + fibonacci10 + PB + progMinimo (delta 5) → PB morre por `intEnderecoInvalido`, os outros terminam com 999, 5040 e 55, `ps` vazio, segundo `execAll` → aviso, `new` + `execAll` de novo funciona (processo sozinho "sai e entra" nele mesmo a cada fatia). fatorial + fibonacci10 + fatorialV2 com delta 1/2/3/1000 → 166/83/54/0 trocas, resultados certos em todos (com delta 1 o caso "STOP do último na última instrução da fatia" acontece sempre e não quebra). Com delta 1000 vira FIFO (termina na ordem 0, 1, 2): bom exemplo para a pergunta "e se o delta for grande?". `exec 1` com 3 processos: começa pelo 1 e depois revezam todos.
- **Objetivo para a apresentação (estudante, 04/10):** mostrar **os dois modos**: sequencial (`execAll`, Parte 1) e contínuo (thread escalonando enquanto o usuário digita, Parte 2), que o estudante acha que é o foco do professor. Plano: tag `t1c-sequencial` ao fechar a Parte 1; na Parte 2, manter o `execAll` funcionando (ex.: modo contínuo liga/desliga por comando).
- ✅ (Tarefa 5) se a rotina do timer **não** chamar `para()`, o `irpt` continua ligado e a CPU trataria a mesma interrupção de novo na volta seguinte. Quem zera o `irpt` é o `setContext` (chamado pelo escalonador ao restaurar o próximo processo).
- ✅ (Tarefa 6) o `case STOP` não faz `pc++`; tudo bem, porque depois do `STOP` o escalonador troca o contexto (o `pc` do processo terminado não é mais usado).
- ✅ (Tarefa 6, conferido: gerava o NullPointerException corrigido com a guarda no `trocaPorTempo`) se o `STOP` cair na última instrução da fatia, hoje aparecem `SYSCALL STOP` e depois `intTempo`. Quando o `STOP` passar a chamar o escalonador (que faz `setContext` e zera o contador), conferir esse caso.
- **Decidido (04/10):** no fim do processo (`STOP` ou erro), o SO mostra o **dump do processo antes de liberar** memória e PCB (segue o enunciado e o resultado aparece no `execAll`). O comando **`exec <id>` fica** (o enunciado manda manter os comandos da T1B). **Revisto (04/10):** chegou a ser planejado um modo "só aquele processo, sem preempção" (flag `execUnico` no GP), mas foi **descartado**: o enunciado não define o comportamento do `exec` na T1C, a flag é complexidade que ninguém pediu, e na Parte 2 (threads) os processos rodam sozinhos e o modo perderia o sentido. Ficou: `exec <id>` põe aquele processo na CPU primeiro e o escalonamento normal segue (os outros também rodam). Os passos 5 e 6 do `executaProcesso` saem (o `terminaProcesso` cuida do fim).
- **Revisto de novo (04/10, decisão final do estudante):** o `exec <id>` **mantém o comportamento da T1B** (roda só aquele processo até o fim, sem escalonar; os outros ficam `PRONTO`) e o `execAll` é o modo novo, escalonado. Era isso que o estudante entendeu do enunciado ("mantenha todos os comandos existentes"). A flag `execUnico` **volta** (Tarefa 6b). Justificativa para a apresentação: o relógio é hardware e sempre toca; o enunciado diz que a rotina do relógio "avalia se o processo que está rodando deve ser trocado", e no modo `exec` a avaliação é "não troca". Diferença em relação à T1B: depois do `exec` o `dump <id>` não acha mais o processo (liberado no `STOP`, como o enunciado manda); o resultado aparece no dump automático do `terminaProcesso`. ⏳ Na Parte 2 (contínuo), rever o que o `exec` significa.
- **Tarefa 6b feita pelo agente (04/10)**, a pedido explícito do estudante (sem tempo; precisava passar a Parte 2 para a dupla). **A dupla precisa ler e entender antes da apresentação.** Mudanças no `GP`: `execUnico = false` no construtor; `executaProcesso` faz `execUnico = true` → `run()` → `execUnico = false` (desliga para o próximo `execAll` não herdar o modo); `escalona()` passo 1 virou `if (prontos.isEmpty() || execUnico)` (no `exec`, quando o processo acaba a CPU para e os outros ficam `PRONTO`); `trocaPorTempo()`, depois da guarda do `null`, se `execUnico` faz `salvaContexto(rodando)` + `restauraContexto(rodando)` e `return` (fatia nova para o mesmo processo; só `return` deixaria o `irpt` em `intTempo` e a interrupção seria tratada em **toda** instrução, porque a CPU não zera o `irpt`, só o `setContext`). `help` do shell: `exec <id>` agora diz "executa so o processo <id> ate o fim (sem escalonar)". No `exec` o `Interrupcao intTempo` continua aparecendo a cada `delta`: o relógio toca, mas o SO decide não trocar (bom para mostrar na apresentação).
- Testes da 6b (agente, 04/10, fora do repo): delta 5, fatorial + fibonacci10 + progMinimo, `exec 1` → só o fibonacci roda (nenhum "entra/sai"), termina com 55, `ps` mostra 0 e 2 `PRONTO` com pc 0; `execAll` depois → 0 e 2 em round robin (999 e 5040), `ps` vazio. `exec` num PB → morre por endereço inválido, fatorial continua `PRONTO`; `exec 0` → 5040; `exec 0` de novo → "nao encontrado". Delta 1 (relógio em toda instrução): `exec 0` e `exec 1` → 5040 e 55 sem exceção; `execAll` sem processos → aviso.

---

## 4. Convenções

- **Marque o que é da dupla.** Arquivos novos levam no topo `// [T1A] Criado pelo grupo - <descrição>`. Alterações em código do professor levam `// [T1A] <o que mudou>`. Nas próximas fases: `[T1B]`, `[T2]`...
- `git diff codigo-base` mostra tudo que a dupla mudou em relação ao código original.
- Siga o estilo do código existente: nomes em português, parâmetros de construtor com `_` (`_tamPg`), comentários curtos.
- Classes do SO ficam em `so/`, e as de hardware em `hardware/`.
