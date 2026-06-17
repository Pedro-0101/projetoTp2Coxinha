Atue como um Arquiteto de Software especialista em Java e Spring Boot. Implemente o Back-End completo para um sistema de "Banca de Coxinha (Caixa Eletrônico de Salgados)" utilizando Java 17+ e Spring Boot (Spring Web, Spring Data JPA). O banco de dados pode ser em memória (H2) para fins de teste.

O projeto deve seguir estritamente o padrão de arquitetura MVC e atender a todos os requisitos de negócio e padrões de projeto listados abaixo.

### REQUISITOS TÉCNICOS OBRIGATÓRIOS
1. Comunicação Restrita em JSON: Todos os endpoints nos Controllers devem consumir e produzir 'application/json'.
2. Complexidade de Movimentação Real: O sistema não é um CRUD simples. Cada compra, inserção de nota ou troca de sabor deve gerar registros reais na tabela de Movimentação, alterando o saldo do cliente e o estoque físico de cédulas no caixa.
3. Ausência de Emojis e Comentários Supérfluos: O código gerado deve ser limpo, profissional, bem estruturado e sem comentários redundantes ou emojis.

### REQUISITOS DE NEGÓCIO (CAIXA ELETRÔNICO)
1. Login de Cliente: Autenticação simples que carrega o perfil e saldo do consumidor.
2. Inserir Crédito (Nota): O usuário insere uma cédula física. O saldo dele aumenta e o slot correspondente daquela nota aumenta em +1.
3. Comprar Coxinha: O usuário escolhe o sabor (Frango, Catupiry, etc.). O sistema calcula o preço da coxinha, valida o saldo, calcula se há troco exato disponível nas cédulas físicas e realiza a movimentação.
4. Troca de Sabores: Permite transferir o valor de uma compra/reserva de um sabor para outro, gerando um estorno da movimentação anterior e um novo registro.
5. Extrato de Glutonaria: Listagem detalhada de todas as transações, contendo ID, data/hora exata (LocalDateTime), valor da nota inserida, tipo de sabor e tipo de movimentação (ENTRADA, SAÍDA, ESTORNO).

### REGRA CRÍTICA DE TRANSAÇÃO DE NOTAS (SLOTNOTAS)
O sistema deve simular o estoque físico de dinheiro em um caixa eletrônico com 7 slots de notas: R$ 2, 5, 10, 20, 50, 100 e 200.
- Lógica do Troco: Se o usuário paga uma coxinha de R$ 8 com uma nota de R$ 10, o sistema precisa verificar se há pelo menos uma nota de R$ 2 disponível no slot correspondente.
- Algoritmo de Troco Impossível: O algoritmo deve tentar compor o valor do troco usando as maiores notas disponíveis primeiro. Como a menor nota é R$ 2, valores de troco ímpares só são possíveis se houver notas de R$ 5 disponíveis para ajustar a paridade.
- Se não houver a combinação exata de cédulas disponíveis para fornecer o troco completo, a transação inteira deve sofrer rollback e retornar um erro HTTP 400 com a mensagem exata: "Transação impossível: falta de cédulas específicas".
- Se houver troco, o estoque de cédulas deve ser atualizado instantaneamente (adicionando a nota que o cliente pagou e removendo as notas usadas no troco).

### PADRÕES DE PROJETO OBRIGATÓRIOS (Implementar os 5 abaixo)
1. DAO / Repository: Utilizar as interfaces do Spring Data JPA para isolar completamente o acesso ao banco de dados das entidades Cliente, Movimentacao e SlotNotas.
2. Strategy (Cálculo de Preço): Criar a interface 'CalculoPrecoStrategy' com o método 'calcular(Double base)'. Implementar as classes 'PrecoPadrao' e 'PrecoPromocional' para variar a lógica de preço dinamicamente.
3. Factory Method (Criação de Sabores): Criar a classe 'CoxinhaFactory' com um método que receba uma String com o nome do sabor (ex: "FRANGO", "CATUPIRY") e retorne a instância correta de classes que implementem a interface 'Coxinha'.
4. Command (Operações e Estorno): Encapsular as ações de Compra e Troca em classes que implementem uma interface 'TransacaoCommand' contendo os métodos 'executar()' e 'desfazer()', permitindo reverter transações e ajustar os slots de notas e saldos caso uma operação falhe ou seja cancelada.
5. Facade (Fachada do Caixa): Criar a classe 'CaixaEletronicoFacade' para unificar a coordenação entre a verificação de saldo do Cliente, validação de cédulas no SlotNotas e criação da Movimentacao. O Controller deve interagir apenas com esta Facade para manter a arquitetura limpa.

Forneça o código estruturado em pacotes claros (model, repository, service, controller, strategy, factory, command, facade).