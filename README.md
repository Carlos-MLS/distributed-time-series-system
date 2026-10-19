# Sistema Distribuído de Séries Temporais

Projeto académico da unidade curricular de **Sistemas Distribuídos**,
implementado em Java, com arquitetura cliente-servidor e foco em concorrência,
comunicação em rede e processamento de eventos.

## Objetivos do projeto

- Implementar um servidor concorrente capaz de gerir multiplos clientes.
- Suportar operacoes sobre eventos (insercao, consulta e agregacoes).
- Garantir robustez de comunicacao com protocolo de mensagens sobre sockets.
- Avaliar comportamento funcional, performance e escalabilidade.

## Funcionalidades

- Autenticacao e gestao de utilizadores.
- Cliente interativo para operacoes manuais.
- Cliente de testes com varios cenarios automatizados.
- Persistencia de series/eventos em ficheiros de dados.
- Suporte a concorrencia no servidor para pedidos simultaneos.

## Estrutura do projeto

- `src/apps/` - Aplicacoes cliente (interativo e testes).
- `src/client/` - Biblioteca cliente.
- `src/common/` - Componentes partilhados (protocolos de comunicacao, framing, utilitarios).
- `src/protocol/` - Definicoes de mensagens.
- `src/server/` - Logica de servidor, base de dados e modelos de dominio.
- `data/series/` - Ficheiros de dados gerados na execucao.

## Como compilar

```bash
javac src/**/*.java
```

## Como executar

### Servidor

```bash
java -cp src server.Server [D] [S]
```

Exemplo:

```bash
java -cp src server.Server 50 10
```

### Cliente interativo

```bash
java -cp src apps.ClienteInterativo
```

### Cliente de testes

```bash
java -cp src apps.ClienteTestes [opcao]
```

Opcoes:

- `1` - Testes funcionais (Fases 1-7)
- `2` - Testes de performance (insercao massiva)
- `3` - Testes de escalabilidade (multiplos clientes + cliente multi-threaded)
- `4` - Testes de robustez (cliente lento)
- `5` - Executa todos os testes

## Limpeza de artefactos gerados

```bash
./clean.sh
```

O script remove ficheiros `.class` e `.dat` gerados durante compilacao/execucao.

## Tecnologias

- Java
- Programacao concorrente (threads, sincronizacao)
- Sockets TCP
- Serializacao/protocolo de mensagens customizado

## Contexto academico

Este projeto foi desenvolvido no ambito da unidade curricular de Sistemas Distribuidos.
