# Design Patterns — Banca Coxinha

Documento de referência dos padrões de projeto usados no sistema de caixa
eletrônico de coxinhas (Spring Boot). Para cada padrão: **o que é
conceitualmente**, **onde está no código**, **por que foi usado** e **como foi
inserido**.

---

## Visão geral

```
Controller
   └─> CaixaEletronicoFacade            [Facade]
         ├─ resolverEstrategia()        [Strategy de preço]
         ├─ coxinhaFactory.criar()      [Factory]
         ├─ new CompraCommand(...)      [Command]
         └─ registroTransacoes.executar()           [Command invoker]
               ├─ observadores.aoExecutar(...)       [Observer]
               └─ caixaOperacoes.registrarCompra()
                     ├─ estrategia.calcular()        [Strategy de preço]
                     ├─ calculadoraTroco.devolver()  [Strategy de troco]
                     └─ repositories.save()          [Repository]
```

Padrões GoF: **Command, Strategy, Factory Method, Facade, Observer**.
Padrões de apoio (Spring/arquiteturais): **Repository, Dependency Injection,
Singleton, DTO + Layered Architecture**.

---

## 1. Command

**Pacote:** `command/`
**Arquivos:** `TransacaoCommand`, `CompraCommand`, `TrocaCommand`, `RegistroTransacoes`

### O que é, conceitualmente
O padrão **Command** encapsula uma requisição (uma ação) como um objeto. Em vez
de chamar diretamente um método, você cria um objeto que representa "a operação
a ser feita", contendo todos os dados necessários. Isso permite enfileirar,
registrar em histórico e — principalmente — **desfazer** operações, já que cada
objeto-comando sabe tanto se executar quanto se reverter.

Participantes clássicos: `Command` (interface), `ConcreteCommand`, `Invoker`
(quem dispara) e `Receiver` (quem realmente faz o trabalho).

### Onde está
- `TransacaoCommand` — a interface `Command`, com `executar()`, `desfazer()`,
  `getResultado()` e `getClienteId()`.
- `CompraCommand` e `TrocaCommand` — os *ConcreteCommands*. Guardam no construtor
  todos os parâmetros (cliente, itens, estratégia, notas) e delegam o trabalho ao
  *Receiver* `CaixaOperacoes`.
- `RegistroTransacoes` — o *Invoker*. Mantém uma pilha (`Deque`) de comandos
  executados (o histórico) e expõe `executar()` e `desfazerUltima()`.

### Por que foi usado
A funcionalidade de **desfazer a última transação** exige guardar o que foi feito
e como reverter. Modelar cada operação como um comando reversível torna isso
natural: a pilha de histórico é literalmente a lista de comandos executados.

### Como foi inserido
Cada operação de negócio (compra, troca) virou uma classe que implementa
`TransacaoCommand`. O `RegistroTransacoes.executar()` chama `comando.executar()`
e empilha; `desfazerUltima()` desempilha (global ou filtrando por cliente) e
chama `comando.desfazer()`. Os métodos são `synchronized` para manter o histórico
consistente sob concorrência.

---

## 2. Strategy (preço)

**Pacote:** `strategy/`
**Arquivos:** `CalculoPrecoStrategy`, `PrecoPadrao`, `PrecoPromocional`

### O que é, conceitualmente
**Strategy** define uma família de algoritmos intercambiáveis, encapsula cada um
em sua própria classe e os torna substituíveis em tempo de execução. O código
cliente trabalha contra a interface e não precisa saber qual variante concreta
está em uso.

### Onde está
- `CalculoPrecoStrategy` — interface com `Double calcular(Double base)`.
- `PrecoPadrao` — retorna o preço cheio.
- `PrecoPromocional` — aplica desconto de R$ 2,00 com piso de R$ 2,00.
- Escolha em runtime: `CaixaEletronicoFacade.resolverEstrategia(boolean)`.
- Uso: `CaixaOperacoes.registrarCompra(...)` chama `estrategia.calcular(precoBase)`.

### Por que foi usado
A regra de precificação varia (normal vs. promocional) e tende a crescer. Strategy
permite trocar o cálculo sem mexer no fluxo de compra e adicionar novas políticas
de preço criando apenas uma nova classe.

### Como foi inserido
O `Facade` decide a estratégia a partir do flag `promocional` do request e a
injeta no `CompraCommand`/`CaixaOperacoes`. O cálculo do preço unitário é
delegado à estratégia, isolando o "como precificar" do "como registrar a compra".

---

## 3. Strategy (troco) / Backtracking

**Pacote:** `service/`
**Arquivo:** `CalculadoraTroco`

### O que é, conceitualmente
Mesma ideia de Strategy: dois algoritmos diferentes para devolver troco,
escolhidos conforme a necessidade do cliente. Internamente, o cálculo usa
**backtracking** (busca recursiva com retrocesso) para compor o troco com as
cédulas disponíveis no caixa.

### Onde está
- `calcularTroco(...)` — exige troco **exato**; se não for possível compor,
  lança `TransacaoImpossivelException`.
- `devolverTroco(...)` — devolve o **máximo possível** em cédulas e transforma o
  restante em crédito do cliente.
- O método privado `compor(...)` é o backtracking sobre as denominações.
- Seleção: o flag `exigirTrocoExato` em `CaixaOperacoes.registrarCompra(...)`.

### Por que foi usado
Há duas políticas de troco com comportamentos distintos. Separá-las em métodos/
algoritmos dedicados mantém cada regra clara e testável (ver `CalculadoraTrocoTest`).

---

## 4. Factory Method

**Pacote:** `factory/`
**Arquivos:** `CoxinhaFactory`, `Coxinha`, `CoxinhaFrango`, `CoxinhaCatupiry`, `CoxinhaCarne`, `CoxinhaQueijo`

### O que é, conceitualmente
**Factory** centraliza a criação de objetos, escondendo do cliente as classes
concretas instanciadas. O cliente pede um produto por um identificador e recebe a
implementação correta, sem acoplar-se aos `new` das classes específicas.

### Onde está
- `Coxinha` — interface do produto (`getSabor()`, `getPrecoBase()`).
- `CoxinhaFrango`, `CoxinhaCatupiry`, `CoxinhaCarne`, `CoxinhaQueijo` — produtos concretos.
- `CoxinhaFactory.criar(String sabor)` — mapeia a string para a instância via
  `switch`, lançando `RegraNegocioException` para sabor inválido.
  `saboresDisponiveis()` lista todos os produtos.

### Por que foi usado
Os sabores chegam como texto no request da API. A factory traduz esse texto em
objetos de domínio e concentra num único lugar a validação de sabor e a lista de
produtos disponíveis — adicionar um sabor novo é criar uma classe e um `case`.

### Como foi inserido
O `Facade` nunca dá `new` em sabor: chama `coxinhaFactory.criar(item.sabor())` e
recebe um `Coxinha` pronto, já com preço base encapsulado em cada produto.

---

## 5. Facade

**Pacote:** `facade/`
**Arquivo:** `CaixaEletronicoFacade`

### O que é, conceitualmente
**Facade** oferece uma interface única e simplificada para um subsistema
complexo. Os clientes (controllers) falam só com a fachada, sem conhecer as peças
internas nem como elas se coordenam.

### Onde está
`CaixaEletronicoFacade` orquestra `ClienteService`, `CaixaOperacoes`,
`CoxinhaFactory`, `RegistroTransacoes`, repositories e as estratégias. Métodos
como `comprar()`, `trocar()`, `inserirCredito()`, `desfazerUltimaTransacao()`
executam o caso de uso inteiro e devolvem DTOs prontos.

### Por que foi usado
Sem a fachada, cada controller teria que conhecer Command, Strategy, Factory e
repositories e coordená-los. A fachada esconde essa complexidade e centraliza o
controle transacional (`@Transactional`) e a conversão entidade↔DTO.

### Como foi inserido
Os controllers chamam apenas `facade.comprar(request)` etc. Toda a montagem
(buscar cliente → resolver estratégia → criar itens via factory → criar e
registrar o command → montar a resposta) acontece dentro da fachada.

---

## 6. Observer  *(adicionado por último)*

**Pacote:** `observer/`
**Arquivos:** `TransacaoObserver`, `LogTransacaoObserver` + integração em `RegistroTransacoes`

### O que é, conceitualmente
**Observer** define uma dependência um-para-muitos: quando um objeto (o *subject*)
muda de estado ou dispara um evento, todos os seus *observers* registrados são
notificados automaticamente. O subject não conhece as classes concretas dos
observers — fala apenas com a interface — o que permite **adicionar novos
reativos sem alterar o subject** (princípio aberto/fechado).

### Onde está
- `TransacaoObserver` — interface *Observer*, com `aoExecutar(TransacaoCommand)`
  e `aoDesfazer(TransacaoCommand)`. Ambos `default` vazios, para que cada
  observer implemente só o evento que lhe interessa.
- `LogTransacaoObserver` — *ConcreteObserver* (`@Component`) que registra em log
  cada transação executada e desfeita.
- `RegistroTransacoes` — o *Subject*. Recebe `List<TransacaoObserver>` por
  injeção e notifica todos após executar/desfazer.

### Por que foi usado
Era preciso adicionar um padrão novo de forma **segura e aditiva**, sem alterar
cálculos nem quebrar nada. O `RegistroTransacoes` já é o ponto central por onde
toda transação passa, sendo o lugar natural para emitir eventos. Reações a
transações (log, métricas, alertas) são efeitos colaterais que não devem poluir a
lógica de negócio — exatamente o caso de uso do Observer.

### Como foi inserido
1. Criada a interface `TransacaoObserver` com métodos `default` vazios.
2. Criado o `LogTransacaoObserver` como `@Component`.
3. O `RegistroTransacoes` passou a receber `List<TransacaoObserver>` no construtor
   — o Spring injeta automaticamente **todos** os beans dessa interface.
4. Em `executar(...)` (após empilhar) e em `desfazerUltima(...)` (após desfazer),
   ele percorre os observers chamando `aoExecutar`/`aoDesfazer`.

Como as assinaturas públicas não mudaram e o observer só gera log, nenhum cálculo,
saldo ou resposta foi afetado e todos os 25 testes continuam passando. Para
adicionar uma nova reação (ex.: contador de transações, alerta de caixa baixo),
basta criar outra classe `@Component implements TransacaoObserver` — sem tocar no
`RegistroTransacoes`.

---

## Padrões de apoio (Spring / arquitetura)

### Repository
**Pacote:** `repository/` — `ClienteRepository`, `MovimentacaoRepository`,
`SlotNotaRepository`. Abstraem o acesso a dados via `JpaRepository`; métodos como
`findByLogin` e `findByDenominacao` são gerados pelo Spring Data a partir do nome.
A lógica de negócio nunca escreve SQL.

### Dependency Injection (Inversão de Controle)
Todas as colaborações são injetadas por construtor (`@Component`, `@Service`). O
contêiner Spring monta o grafo de objetos, desacoplando as classes de suas
dependências concretas. É também o mecanismo que torna o Observer plugável (a
`List<TransacaoObserver>` é montada pelo Spring).

### Singleton
Beans Spring como `RegistroTransacoes` e `CoxinhaFactory` têm instância única no
contêiner. Relevante para o histórico de undo, que precisa ser compartilhado entre
todas as requisições.

### DTO + Layered Architecture
Camadas `controller → facade → service → repository`. O pacote `dto/` (records
como `CompraRequest`/`CompraResponse`) isola o contrato da API das entidades JPA
do pacote `model/`; a conversão entidade↔DTO ocorre nos métodos `paraXResponse(...)`
do facade.

---

## Resumo

| Padrão | Tipo | Local | Função |
|---|---|---|---|
| Command | Comportamental | `command/` | Encapsula compra/troca como objeto reversível → undo |
| Strategy (preço) | Comportamental | `strategy/` | Troca a política de precificação em runtime |
| Strategy (troco) | Comportamental | `service/CalculadoraTroco` | Dois algoritmos de troco (exato vs. máximo+crédito) |
| Factory | Criacional | `factory/` | Cria o sabor de coxinha a partir de texto |
| Facade | Estrutural | `facade/` | Interface única que orquestra todo o subsistema |
| Observer | Comportamental | `observer/` | Notifica reativos (log) a cada transação |
| Repository | Arquitetural | `repository/` | Abstrai persistência |
| Dependency Injection | Arquitetural | todo o sistema | Desacopla e monta o grafo de objetos |
| Singleton | Criacional | beans Spring | Instância única compartilhada |
| DTO + Layered | Arquitetural | `dto/` + camadas | Separa contrato da API do modelo de domínio |
