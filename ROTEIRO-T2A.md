# Roteiro de apresentação — T2A (E/S com bloqueio)

Sequência de comandos para mostrar cada requisito do enunciado (`enunciados/T2-T2A.pdf`).
Tudo roda **numa sessão só**, na ordem abaixo (testado em 07/11: 7 partes, 11 processos, sem erro).

## Antes de começar

- Compilar pelo terminal: `rm -rf bin && javac -d bin $(find src -name '*.java') && java -cp bin Sistema`
  (o `rm -rf bin` evita o erro `UnsupportedClassVersionError` se o VSCode tiver compilado com outro Java).
- Configuração em `Sistema.main`: memória 1024, página 8, **delta 5**.
- Trace desligado (padrão). Só ligar (`traceOn`) se o professor pedir para ver instrução por instrução.
- O console imprime mensagens **no meio** do que você está digitando. É normal: são threads diferentes.
  Se o prompt `>` sumir, é só digitar o comando mesmo assim.
- Os ids crescem durante a sessão. Os ids abaixo são os que aparecem se a ordem for seguida.

---

## Parte 1 — SO no ar e escalonamento contínuo

**Requisito:** "o SO aceita continuamente a criação de novos processos enquanto executa os já submetidos" (seção 3).

```
start
new fibonacci10
new fatorial
ps
```

**O que mostrar:**
- `start` liga a thread da CPU (`EscalonadorThread`). O shell continua livre.
- Os processos começam a rodar **assim que são criados**, sem `exec`: aparecem `ESCALONADOR: entra processo` / `fim do slice` alternando 0 e 1.
- Cada um termina com dump automático: fibonacci (id 0) com 0, 1, 1, …, 34; fatorial (id 1) com 5040.
- `ps` vazio no fim: memória e PCB liberados.

**Falar:** "Desde a T1C o escalonador é uma thread. Na T2A a mesma thread faz o papel da *thread CPU* do esquema do enunciado."

---

## Parte 2 — OUT assíncrono: bloqueio, console e retorno

**Requisito:** IN/OUT gera pedido na fila do dispositivo, o processo **bloqueia**, outro processo usa a CPU (seção 2).

```
new fatorialV2
new fibonacci10
ps
```

**O que mostrar (na ordem em que aparece):**
```
SYSCALL pars:  2 / 18                              ← fatorialV2 (id 2) pede OUT do endereço 18
ESCALONADOR: bloqueio de IO no processo 2          ← salva contexto, BLOQUEADO, fila de bloqueados
ESCALONADOR: entra processo: 3                     ← outro processo usa a CPU
...
OUT:   120                                         ← a thread do console faz o IO (5! = 120)
ESCALONADOR: fim de IO, processo 2 volta para prontos
ESCALONADOR: entra processo: 2                     ← continua do pc 17 (instrução depois do SYSCALL)
ESCALONADOR: termino do processo: 2
```

**Falar:**
- "O OUT não acontece mais na CPU. A syscall cria um `PedidoIO` e coloca na fila do `Console`, que é outra thread (produtor/consumidor com dois semáforos)."
- "O pedido leva a **tabela de páginas do processo**: quando o console atende, a CPU já tem outro processo, então ele traduz com a tabela do pedido. Isso é o **DMA**."

---

## Parte 3 — IN pelo usuário, com outro processo rodando

**Requisito:** no IN, o valor lido é escrito por DMA na memória do processo; o processo continua de onde parou (seção 2). O usuário responde o IN pelo shell (esquema, "resposta de pedido de IN ao módulo de IO").

```
new fibonacciREAD
new fibonacci10
ps
in 10
ps
```

**O que mostrar:**
- `CONSOLE: processo 4 pede um valor (IN). Digite: in <valor>`.
- **Enquanto o console espera o usuário, o fibonacci10 (id 5) roda e termina.** O CPU não fica parada esperando o dispositivo lento: é o motivo de existir a T2A.
- Primeiro `ps`: `4  BLOQUEADO  3` (estado *blocked*, pc depois do `SYSCALL`).
- `in 10` → `CONSOLE: valor 10 gravado no endereco 55 do processo 4` → `fim de IO, processo 4 volta para prontos`.
- Dump final do processo 4: posição 55 = 10 e 10 termos da série (0 … 34) nas posições 41–50.

**Falar:** "O console escreve direto na memória do processo (DMA). Quando ele volta para a CPU, o `LDD` da posição 55 já encontra o valor."

---

## Parte 4 — CPU ociosa

**Requisito:** a CPU espera num semáforo quando não há processo e é liberada quando há (sugestão ii).

```
new fibonacciREAD
ps
in 7
in 3
ps
```

**O que mostrar:**
- `ps`: `6  BLOQUEADO  3`. É o **único** processo: a CPU está parada (thread dormindo no semáforo).
- `in 7` → valor gravado; `in 3` → `Nenhum processo esperando IN.` (o segundo é recusado).
- O processo termina mesmo com a CPU parada: série 0, 1, 1, 2, 3, 5, 8.

**Falar:** "Se o IO termina com ninguém na CPU, ninguém veria a interrupção. Então o console também **acorda a thread da CPU** (`acordaEscalonador`), e ela trata o aviso antes de escalonar. É como uma CPU real ociosa, acordada por interrupção."

---

## Parte 5 — Vários pedidos na fila e interrupção de IO durante outro processo

**Requisito:** o processo interrompido pelo dispositivo "retoma sua execução e acaba sua fatia de tempo" (seção 2).

```
new fatorialV2
new fibonacciREAD
new fatorialV2
ps
in 4
ps
```

**O que mostrar:**
- `ps`: os **três** `BLOQUEADO` ao mesmo tempo (7 e 9 com pc 17, 8 com pc 3). Três pedidos na fila do console.
- O console atende **um por vez, em ordem de chegada**: os dois OUT esperam o usuário responder o IN do processo 8.
- Depois do `in 4`: `OUT: 120` duas vezes, e linhas como
  ```
  Interrupcao intIO   pc: 4
  ESCALONADOR: fim de IO, processo 7 volta para prontos
  ```
  chegando **com o processo 8 na CPU**. Ele **não sai** da CPU: só a fila de prontos muda.
- Os três terminam.

**Falar:** "A `intIO` tem rotina própria: tira o processo de bloqueados e põe em prontos, **sem chamar o escalonador**. Quem estava rodando termina a fatia."

---

## Parte 6 — Encerrar

```
new fibonacciREAD
exit
```

**O que mostrar:** o `exit` funciona mesmo com um IN **esperando resposta**: para a thread da CPU, depois a do console (`pararThread` + `join`), e o programa termina.

---

## Se o professor pedir

| Pedido | Comando / resposta |
|---|---|
| Ver os frames de um processo | `dump <id>` enquanto ele está `BLOQUEADO` (ex.: na Parte 3, antes do `in`) |
| Memória física | `dumpM 0 64` |
| Remover processo | `rm <id>` (recusado se estiver `RODANDO`) |
| Parar o escalonamento | `stop`; voltar com `start` |
| `exec` / `execAll` | Funcionam como na T1C, **só com programas sem IO** (`fatorial`, `fibonacci10`, `progMinimo`). Não usar com o `start` ligado. Resposta: "são os modos sequenciais da T1C, em que o shell roda a CPU; com IO o shell precisa ficar livre para responder o IN, então o IO é feito no modo contínuo." |
| Trace | `traceOn` / `traceOff` |

## Perguntas prováveis (respostas completas no `AGENTS.md`, seção T2A)

1. **Por que o `pc++` vem antes do `sysCall.handle()`?** O bloqueio troca de processo dentro do `handle`; o pc salvo tem que ser o da instrução seguinte.
2. **Por que o pedido leva a tabela de páginas?** Quando o console atende, a CPU já tem a tabela de outro processo.
3. **Por que criar o pedido antes de bloquear e enfileirar depois?** Depois do bloqueio, os registradores são de outro processo; enfileirar depois garante que o processo já está em `bloqueados` quando o IO termina.
4. **Quantas fichas começa cada semáforo?** Mutex = 1; contadores (`pedidosDisponiveis`, `respostaIN`, `processosDisponiveis`) = 0; vaga do sinal de IO (`sinalLivre`) = 1.
5. **O que acontece se dois IO terminam juntos?** O `sinalIO` guarda um id só; o semáforo `sinalLivre` faz o console esperar a CPU ler o primeiro aviso.
6. **Por que o `limpaInterrupcao()`?** A `intIO` não troca de processo, então ninguém zeraria o `irpt`; sem isso a interrupção seria tratada em toda instrução e o relógio nunca mais tocaria.
7. **Quais são as threads?** Shell (`main`), CPU/escalonador (`EscalonadorThread`) e console (`Console`).
