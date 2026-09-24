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

**Fase atual: T1A**

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
- [ ] Parametrizar `tamPg` (`Sistema`, `SO`) e criar o `GM` no `SO`
- [ ] Carga paginada em `Utilities.loadProgram`
- [ ] Tradução na `CPU`: método `traduz(endLogico)` usado no fetch, `LDD`, `STD`, `LDX`, `STX`, `JMPIM`, `JMPIGM`, `JMPILM`, `JMPIEM` e na syscall de escrita. O `pc` continua **lógico**. `setContext` passa a receber a tabela de páginas.
- [ ] Testes: vários programas carregados ao mesmo tempo, frames **não contíguos**, vários `tamPg` (4, 8, 10, 16)

#### Decisões e observações

- `PB` (tamanho 16, acessa endereço 50) e `PC` (tamanho 54, acessa 99) escrevem fora da própria área. Com proteção de memória, vão gerar endereço inválido. Isso está correto, mas **deve ser confirmado com o professor** (pendente).
- `JMPIM`, `JMPILM` e `JMPIEM` não validam endereço no código original. A tradução deve corrigir isso.
- Não criar `PCB`, gerente de processos nem escalonador nesta fase. A tabela de páginas pode ser passada direto para a CPU.
- `GM.aloca` usa os primeiros frames livres encontrados; devolve `null` sem alterar nada quando não há frames suficientes.
- O professor autorizou separar o código em vários arquivos/pacotes.

---

### T1B — (enunciado ainda não adicionado)

> Quando esta fase começar, peça o enunciado ao estudante e preencha: título, requisitos, checklist e "Decisões e observações", no mesmo formato do T1A.

---

### T1C — (enunciado ainda não adicionado)

> Quando esta fase começar, peça o enunciado ao estudante e preencha: título, requisitos, checklist e "Decisões e observações", no mesmo formato do T1A.

---

## 4. Convenções

- **Marque o que é da dupla.** Arquivos novos levam no topo `// [T1A] Criado pelo grupo - <descrição>`. Alterações em código do professor levam `// [T1A] <o que mudou>`. Nas próximas fases: `[T1B]`, `[T2]`...
- `git diff codigo-base` mostra tudo que a dupla mudou em relação ao código original.
- Siga o estilo do código existente: nomes em português, parâmetros de construtor com `_` (`_tamPg`), comentários curtos.
- Classes do SO ficam em `so/`, e as de hardware em `hardware/`.
