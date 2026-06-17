# Banca de Coxinha — Caixa Eletrônico de Salgados

Back-end de demonstração para a disciplina de **Design Patterns**. O sistema simula um caixa
eletrônico que vende coxinhas, controlando saldo do cliente e o estoque físico de cédulas em
7 slots (R$ 2, 5, 10, 20, 50, 100 e 200). O foco é POO bem feita, princípios **SOLID** e a
aplicação de **5 padrões de projeto**.

## Stack

- Java 17+ / Spring Boot 3.5 (Spring Web, Spring Data JPA, Validation)
- Banco H2 em memória
- Documentação interativa via Swagger UI

## Como executar

```bash
./mvnw spring-boot:run
```

- API: `http://localhost:5141`
- Swagger UI: `http://localhost:5141/swagger-ui.html`
- Contrato OpenAPI (JSON, legível por máquina): `http://localhost:5141/v3/api-docs`
- Console H2: `http://localhost:5141/h2-console` (JDBC URL `jdbc:h2:mem:bancacoxinha`, usuário `sa`)

Ao subir, o `DataSeeder` cria os clientes `cliente` (saldo R$ 0) e `vip` (saldo R$ 50) e o
estoque inicial de cédulas.

Para integrar um front-end (inclusive gerado por IA), use [docs/API.md](docs/API.md) — referência
completa de endpoints, schemas e exemplos. O CORS está liberado para `/api/**`.

## Padrões de projeto e SOLID

| Padrão | Onde está | Responsabilidade | Princípio SOLID em destaque |
|--------|-----------|------------------|-----------------------------|
| Repository / DAO | `repository/*Repository` | Isola o acesso ao banco das entidades | DIP — serviços dependem de interfaces |
| Strategy | `strategy/CalculoPrecoStrategy` + `PrecoPadrao`, `PrecoPromocional` | Varia a regra de preço em tempo de execução | OCP — nova regra sem alterar o existente |
| Factory Method | `factory/CoxinhaFactory` + `Coxinha` e sabores | Cria o sabor correto a partir de uma String | OCP / SRP — criação isolada |
| Command | `command/TransacaoCommand` + `CompraCommand`, `TrocaCommand` | Encapsula a operação e seu desfazer | SRP — cada operação é um objeto |
| Facade | `facade/CaixaEletronicoFacade` | Unifica saldo, cédulas e movimentação | SRP — controllers ficam finos |

O **Command** é completado pelo Invoker `command/RegistroTransacoes`, que registra os comandos
executados num histórico e permite desfazer o último (`POST /api/caixa/desfazer`), gerando o
ESTORNO. O desfazer aceita uma lista de `clienteIds`: preenchida, desfaz a última transação
daqueles clientes; vazia (ou sem body), usa o histórico global. O `desfazer()` reverte por id da
movimentação, funcionando mesmo em requisições separadas. O receiver `service/CaixaOperacoes` concentra a mecânica de saldo/cédulas e é
acionado pelos Commands; `service/CalculadoraTroco` implementa o algoritmo de troco; e
`service/ClienteService` cuida do cadastro/consulta de clientes.

## Modelo de negócio

- **Inserir crédito (ENTRADA):** insere uma cédula de valor X → `saldo += X` e o slot da cédula
  aumenta em 1.
- **Comprar coxinha (SAÍDA):** o cliente paga **inserindo uma ou mais cédulas** (`notasPagas`, ex.:
  `[5, 2, 2]`) cuja soma cobre o preço (senão HTTP 400). As cédulas entram na máquina; o troco
  (`total pago − preço`) é devolvido em cédulas — o máximo possível. **O que não puder ser devolvido
  em cédulas vira saldo (crédito) do cliente.**
- **Troca de sabores:** estorna (ESTORNO) a compra original revertendo saldo e cédulas, e
  executa uma nova compra do novo sabor com a mesma cédula.

### Regra do troco

O algoritmo (`CalculadoraTroco`) compõe o troco preferindo as **maiores cédulas disponíveis
primeiro**, via backtracking — sempre encontra a combinação se ela existir (ex.: compõe R$ 100 só
com vinte cédulas de R$ 5). Como a menor cédula é R$ 2, um troco **ímpar exige uma cédula de R$ 5**.
Quando não há cédulas para o valor exato, o caixa devolve o **maior valor possível em cédulas** e
credita a diferença no `saldo` do cliente (campo `trocoEmCredito` na resposta da compra).

> Observação: o enunciado previa retornar `Transação impossível: falta de cédulas específicas`
> quando não houvesse troco exato. Por padrão o sistema faz a devolução parcial + crédito, mas a
> compra aceita a flag **`trocoExato: true`**, que exige troco exato e retorna exatamente aquele
> erro (revertendo a transação) — atendendo o requisito do enunciado sob demanda. Isso usa
> `CalculadoraTroco.calcularTroco` e `TransacaoImpossivelException`.

## Endpoints

| Método | Rota | Descrição |
|--------|------|-----------|
| POST | `/api/auth/login` | Carrega o cliente e o saldo |
| POST | `/api/clientes` | Cadastra um novo cliente |
| GET | `/api/clientes` | Lista os clientes |
| GET | `/api/clientes/{id}` | Perfil e saldo de um cliente |
| POST | `/api/caixa/credito` | Insere uma cédula (ENTRADA) |
| POST | `/api/caixa/compra` | Compra uma coxinha (SAÍDA) |
| POST | `/api/caixa/troca` | Troca o sabor (ESTORNO + nova SAÍDA) |
| POST | `/api/caixa/desfazer` | Desfaz a última transação (Command + ESTORNO). Body `{"clienteIds":[...]}` filtra por clientes; lista vazia ou sem body usa o histórico global |
| POST | `/api/caixa/abastecer` | Reabastece um slot de cédulas |
| GET | `/api/extrato/{clienteId}` | Extrato detalhado das movimentações |
| GET | `/api/caixa/slots` | Estoque atual de cédulas |
| GET | `/api/caixa/sabores` | Sabores e preços disponíveis |

## Roteiro sugerido para a apresentação

1. `POST /api/auth/login` `{"login":"cliente"}` → saldo R$ 0.
2. `POST /api/caixa/credito` `{"clienteId":1,"denominacao":20}` → saldo R$ 20, slot[20] +1.
3. `POST /api/caixa/compra` `{"clienteId":1,"sabor":"FRANGO","notasPagas":[20],"promocional":false}`
   → preço R$ 8, troco R$ 12 (uma de R$ 10 + uma de R$ 2).
4. `POST /api/caixa/compra` com `"sabor":"CARNE"` (R$ 9) e `"notasPagas":[20]` → troco R$ 11,
   demonstra o uso obrigatório da cédula de R$ 5. (Também aceita várias cédulas, ex.: `[5,2,2]`.)
5. Esgote um slot via `GET /api/caixa/slots` e force um troco impossível com `"trocoExato":true`
   → **HTTP 400** com a mensagem exata.
6. `POST /api/caixa/troca` para trocar o sabor de uma compra → veja o ESTORNO e a nova SAÍDA.
7. `GET /api/extrato/{clienteId}` → todas as movimentações com data/hora, tipo, valor da nota,
   sabor e troco.
