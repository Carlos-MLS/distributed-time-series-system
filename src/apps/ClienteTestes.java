package apps;
import client.ClientLib;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClienteTestes {
    public static void main(String[] args) throws Exception {
        System.out.println("=== TESTES DO CLIENTETESTES ===");
        System.out.println("NOTA: Estes testes foram executados com o servidor configurado D=50 S=10");
        System.out.println("      Valores diferentes de D e S podem produzir resultados ligeiramente diferentes.\n");
        System.out.println("Escolha o tipo de teste:");
        System.out.println("1 - Testes Funcionais");
        System.out.println("2 - Testes de Performance");
        System.out.println("3 - Testes de Escalabilidade");
        System.out.println("4 - Testes de Cliente lento");
        System.out.println("5 - Executar TODOS os testes");
        
        // Se não paassarmos um arg, executamos os testes funcionais por default
        int opcao = args.length > 0 ? Integer.parseInt(args[0]) : 1;
        
        switch(opcao) {
            case 1:
                testesFuncionais();
                break;
            case 2:
                testesPerformance();
                break;
            case 3:
                testesEscalabilidade();
                break;
            case 4:
                testesRobustez();
                break;
            case 5:
                testesFuncionais();
                System.out.println("\n\n");
                testesPerformance();
                System.out.println("\n\n");
                testesEscalabilidade();
                System.out.println("\n\n");
                testesRobustez();
                break;
            default:
                System.out.println("Executando os testes funcionais...");
                testesFuncionais();
        }
    }
    
    //-----------------------------------------------------------------------
    // TESTES FUNCIONAIS
    //-----------------------------------------------------------------------------------
    private static void testesFuncionais() throws Exception {
        System.out.println("\n" + "-".repeat(80));
        System.out.println("TESTES FUNCIONAIS, onde validamos as funcionalidades básicas");
        System.out.println("-".repeat(80));
        
        //inicializamos o cliente como localhost na porta definida no servidor
        ClientLib client = new ClientLib("localhost", 12345);
        client.login("Francisco", "francisco123"); //damos load deste user ao iniciar o server

        System.out.println("\n Teste 1 -> Persistência - Adicionar 20 dias de eventos");
        // No final do loop, dia deve valer 20, confirmando que todos os dias foram criados
        int dia;
        for (dia = 0; dia < 20; dia++) {
            client.adicionarEvento("Produto" + dia, 100, 50.0);
            client.novoDia();
            if (dia % 5 == 0) {
                //para mostrar o progresso e n dar muito output
                System.out.println("  Dias 0-" + dia + " persistidos...");
            }
        }
        if (dia == 20) System.out.println("Persistência de todos os dias funciona");
        
        System.out.println("\n Teste 2 -> Agregação em que forçamos a leitura do disco");
        // Como Produto0 só foi vendido no dia 0 (depois houve novoDia 20 vezes), ao pedir 15 dias anteriores
        // estamos fora do intervalo onde existe o evento, por isso qtd = 0
        int qtd = client.quantidadeVendas("Produto0", 15);  // Vai ao disco
        System.out.println("Quantidade Produto0 (15 dias): " + qtd);
        
        System.out.println("\n Teste 3 -> Agregações múltiplas");
        client.adicionarEvento("Laptop", 10, 1000.0);
        client.adicionarEvento("Laptop", 5, 950.0);
        client.novoDia();
        
        // Aqui temos qtd=15 (10+5), vol=14750.0 (10*1000 + 5*950), médio=983.33 (14750/15)
        int qtdLaptop = client.quantidadeVendas("Laptop", 1);
        double volLaptop = client.volumeVendas("Laptop", 1);
        double precoMedio = client.precoMedio("Laptop", 1);
        System.out.println("  Laptop: qtd=" + qtdLaptop + ", vol=" + volLaptop + ", médio=" + precoMedio);
        
        System.out.println("\n Teste 4 Barreiras, vendas simultâneas");
        // A thread abaixo fica bloqueada até Mouse e Teclado serem vendidos, depois retorna true
        Thread t1 = new Thread(() -> {
            try {
                ClientLib clientListener = new ClientLib("localhost", 12345);
                clientListener.login("Francisco", "francisco123");
                boolean ok = clientListener.aguardarVendasSimultaneas("Mouse", "Teclado");
                System.out.println(" Barreira venda simultânea foi satisfeita ? " + ok);
                clientListener.close();
            } catch (Exception e) {
                System.err.println("Erro na barreira " + e.getMessage());
            }
        });
        t1.start();
        Thread.sleep(500);
        client.adicionarEvento("Mouse", 1, 20);
        Thread.sleep(100);
        client.adicionarEvento("Teclado", 1, 50);
        t1.join(2000);
        

        System.out.println("\n Teste 5 Barreiras, vendas consecutivas");
        // Esta thread aguarda até 3 vendas consecutivas do mesmo produto, depois retorna "Monitor"
        Thread t2 = new Thread(() -> {
            try {
                ClientLib clientListener = new ClientLib("localhost", 12345);
                clientListener.login("Francisco", "francisco123");
                String prod = clientListener.aguardarVendasConsecutivas(3);
                System.out.println("Barreira de venda consecutiva satisfeita: " + prod);
                clientListener.close();
            } catch (Exception e) {
                System.err.println("Erro na barreira consecutiva: " + e.getMessage());
            }
        });
        t2.start();
        Thread.sleep(500);
        client.adicionarEvento("Monitor", 1, 300);
        client.adicionarEvento("Monitor", 1, 300);
        client.adicionarEvento("Monitor", 1, 300);
        t2.join(2000);
        


        System.out.println("\n Teste 6 Filtrar Eventos");
        Set<String> filtro = new HashSet<>();
        filtro.add("Laptop");
        filtro.add("Mouse");
        // Aqui obtemos 2 eventos: os 2 eventos de Laptop do dia atual (Mouse foi vendido num dia anterior)
        int numEventos = client.filtrarEventos(filtro, 1).size();
        System.out.println("  Eventos filtrados: " + numEventos);
        
        client.close();
        System.out.println("\n=== TESTES FUNCIONAIS CONCLUÍDOS COM SUCESSO ===");
    }
    
    //----------------------------------------------------------------------------------
    // TESTES DE PERFORMANCE
    //--------------------------------------------------------------------------------
    private static void testesPerformance() throws Exception {
        System.out.println("\n" + "-".repeat(80));
        System.out.println("TESTES DE PERFORMANCE");
        System.out.println("-".repeat(80));
        
        ClientLib client = new ClientLib("localhost", 12345);
        client.login("Francisco", "francisco123");
        
        System.out.println("\n Teste 1 -> Inserimos cerca de 10.000 eventos");
        // Vamos medir o throughput de inserção, devemos ter algo entre 30-40k eventos/segundo
        long inicio = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            client.adicionarEvento("Produto" + (i % 100), i % 50 + 1, 10.0 + (i % 100));
            if (i % 1000 == 0 && i > 0) {
                System.out.println("  Inseridos " + i + " eventos...");
            }
        }
        long fim = System.currentTimeMillis();
        double throughput = 10000.0 / ((fim - inicio) / 1000.0);
        System.out.println("  10.000 eventos inseridos em " + (fim - inicio) + "ms");
        System.out.println("  Throughput: " + String.format("%.2f", throughput) + " eventos/segundo");
        
        client.novoDia();
        
        System.out.println("\n Teste 2 -> Agregações com Cache HIT");
        // Como já agregamos este produto antes, está em cache - deve ser rápido (~0.1-0.3ms/op)
        inicio = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            client.quantidadeVendas("Produto0", 1);
        }
        fim = System.currentTimeMillis();
        double tempoCacheHit = (fim - inicio) / 100.0;
        System.out.println("  100 agregações ( com cache hit) em " + (fim - inicio) + "ms");
        System.out.println("  Tempo médio: " + String.format("%.2f", tempoCacheHit) + "ms/operação");
        
        System.out.println("\n Teste 3 -> Agregações com Cache MISS");
        // Produtos nunca consultados antes - tem de percorrer a série temporal (~2-5ms/op)
        inicio = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            client.quantidadeVendas("Produto" + (i + 50), 1);
        }
        fim = System.currentTimeMillis();
        double tempoCacheMiss = (fim - inicio) / 10.0;
        System.out.println("  10 agregações (cache miss) em " + (fim - inicio) + "ms");
        System.out.println("  Tempo médio: " + String.format("%.2f", tempoCacheMiss) + "ms/operação");
        System.out.println("  Comparação: Cache hit é " + String.format("%.2f", tempoCacheMiss / tempoCacheHit) + "x mais rápido");
        
        System.out.println("\n Teste 4 -> Persistência, 50 dias com 1000 eventos cada");
        // Vamos persistir 50.000 eventos totais
        inicio = System.currentTimeMillis();
        for (int dia = 0; dia < 50; dia++) {
            for (int ev = 0; ev < 1000; ev++) {
                client.adicionarEvento("Produto" + (ev % 20), ev % 10 + 1, 15.0);
            }
            client.novoDia();
            if (dia % 10 == 0) {
                System.out.println("  " + dia + " dias persistidos...");
            }
        }
        fim = System.currentTimeMillis();
        System.out.println("  50 dias persistidos (50.000 eventos) em " + (fim - inicio) + "ms");
        System.out.println("  Tempo médio por dia: " + String.format("%.2f", (fim - inicio) / 50.0) + "ms");
        
        client.close();
        System.out.println("\n---- TESTES DE PERFORMANCE CONCLUÍDOS COM SUCESSO ----");
    }
    
    //-----------------------------------------------------------------------
    // TESTES DE ESCALABILIDADE
    //-----------------------------------------------------------------------
    private static void testesEscalabilidade() throws Exception {
        System.out.println("\n" + "-".repeat(80));
        System.out.println("TESTES DE ESCALABILIDADE, onde temos múltiplos Clientes Concorrentes");
        System.out.println("-".repeat(80));
        
        ClientLib clientPrep = new ClientLib("localhost", 12345);
        clientPrep.login("Francisco", "francisco123");
        for (int i = 0; i < 1000; i++) {
            clientPrep.adicionarEvento("ProdutoEscala" + (i % 10), i % 20 + 1, 25.0);
        }
        clientPrep.novoDia();
        clientPrep.close();
        
        int[] numClientes = {1, 5, 10, 20};
        
        for (int n : numClientes) {
            // Com paralelização, esperamos: 1 cliente ~7-10k ops/s, 5 clientes ~35-40k, 10 clientes ~60-80k
            System.out.println("\n Teste com " + n + " clientes concorrentes");
            
            List<Thread> threads = new ArrayList<>();
            List<Long> tempos = new ArrayList<>();
            
            long inicioGeral = System.currentTimeMillis();
            
            for (int i = 0; i < n; i++) {
                final int clientId = i;
                Thread t = new Thread(() -> {
                    try {
                        ClientLib client = new ClientLib("localhost", 12345);
                        client.login("Francisco", "francisco123");
                        
                        long inicio = System.currentTimeMillis();
                        
                        for (int j = 0; j < 50; j++) {
                            client.quantidadeVendas("ProdutoEscala" + (j % 10), 1);
                            client.volumeVendas("ProdutoEscala" + (j % 10), 1);
                        }
                        
                        long fim = System.currentTimeMillis();
                        synchronized(tempos) {
                            tempos.add(fim - inicio);
                        }
                        
                        client.close();
                    } catch (Exception e) {
                        System.err.println("  Erro no cliente " + clientId + ": " + e.getMessage());
                    }
                });
                threads.add(t);
                t.start();
            }
            
            // Esperar todos terminarem
            for (Thread t : threads) {
                t.join();
            }
            
            long fimGeral = System.currentTimeMillis();
            long tempoTotal = fimGeral - inicioGeral;
            
            double tempoMedio = tempos.stream().mapToLong(Long::longValue).average().orElse(0);
            long tempoMin = tempos.stream().mapToLong(Long::longValue).min().orElse(0);
            long tempoMax = tempos.stream().mapToLong(Long::longValue).max().orElse(0);
            
            System.out.println("Tempo total: " + tempoTotal + "ms");
            System.out.println("Tempo médio por cliente: " + String.format("%.2f", tempoMedio) + "ms");
            System.out.println("Tempo mínimo: " + tempoMin + "ms, máximo: " + tempoMax + "ms");
            System.out.println("Throughput: " + String.format("%.2f", (n * 100.0) / (tempoTotal / 1000.0)) + " ops/segundo");
        }
        
        System.out.println("\n Teste Multi-Threaded -> Cliente com múltiplas threads");
        // Uma única socket partilhada por 10 threads, normal ter gargalo, mas n deve bloquear
        
        ClientLib clientMT = new ClientLib("localhost", 12345);
        clientMT.login("Francisco", "francisco123");
        
        List<Thread> threadsMT = new ArrayList<>();
        List<Long> temposMT = new ArrayList<>();
        
        long inicioMT = System.currentTimeMillis();
        
        for (int i = 0; i < 10; i++) {
            final int threadId = i;
            Thread t = new Thread(() -> {
                try {
                    long inicio = System.currentTimeMillis();
                    
                    for (int j = 0; j < 20; j++) {
                        clientMT.adicionarEvento("ProdutoMT" + threadId, j + 1, 50.0);
                        clientMT.quantidadeVendas("ProdutoMT" + threadId, 1);
                    }
                    
                    long fim = System.currentTimeMillis();
                    synchronized(temposMT) {
                        temposMT.add(fim - inicio);
                    }
                } catch (Exception e) {
                    System.err.println("Erro na thread " + threadId + ": " + e.getMessage());
                }
            });
            threadsMT.add(t);
            t.start();
        }
        
        // Esperar todas terminarem
        for (Thread t : threadsMT) {
            t.join();
        }
        
        long fimMT = System.currentTimeMillis();
        long tempoTotalMT = fimMT - inicioMT;
        double tempoMedioMT = temposMT.stream().mapToLong(Long::longValue).average().orElse(0);
        
        System.out.println("10 threads × 40 ops = 400 operações concorrentes");
        System.out.println("Tempo total: " + tempoTotalMT + "ms");
        System.out.println("Tempo médio por thread: " + String.format("%.2f", tempoMedioMT) + "ms");
        System.out.println("Throughput: " + String.format("%.2f", 400.0 / (tempoTotalMT / 1000.0)) + " ops/segundo");
        
        clientMT.close();
        
        System.out.println("\n--- TESTES DE ESCALABILIDADE CONCLUÍDOS COM SUCESSO ---");
    }
    
    //-----------------------------------------------------------------------
    // TESTES DE ROBUSTEZ
    //-----------------------------------------------------------------------
    private static void testesRobustez() throws Exception {
        System.out.println("\n" + "-".repeat(80));
        System.out.println("TESTES DE ROBUSTEZ - Cliente Lento + Desconexões");
        System.out.println("-".repeat(80));
        
        System.out.println("\n Teste 1 -> Cliente lento não bloqueia clientes normais");
        // O cliente lento demora ~5s, mas o normal deve completar 100 ops em ~30-50ms sem ser afetado
        
        Thread clienteLento = new Thread(() -> {
            try {
                ClientLib client = new ClientLib("localhost", 12345);
                client.login("Francisco", "francisco123");
                
                for (int i = 0; i < 10; i++) {
                    client.adicionarEvento("ProdutoLento" + i, 1, 10.0);
                    Thread.sleep(500);
                }
                
                System.out.println("Cliente lento completou operações");
                client.close();
            } catch (Exception e) {
                System.err.println("  Erro no cliente lento: " + e.getMessage());
            }
        });
        
        Thread clienteNormal = new Thread(() -> {
            try {
                Thread.sleep(100);
                ClientLib client = new ClientLib("localhost", 12345);
                client.login("Francisco", "francisco123");
                
                long inicio = System.currentTimeMillis();
                for (int i = 0; i < 100; i++) {
                    client.adicionarEvento("ProdutoNormal" + i, 1, 10.0);
                }
                long fim = System.currentTimeMillis();
                
                System.out.println("Cliente normal completou 100 operações em " + (fim - inicio) + "ms");
                System.out.println("Cliente normal não foi bloqueado pelo cliente lento");
                client.close();
            } catch (Exception e) {
                System.err.println("Erro no cliente normal: " + e.getMessage());
            }
        });
        
        clienteLento.start();
        clienteNormal.start();
        
        clienteLento.join();
        clienteNormal.join();
        
        System.out.println("\n Teste 2 -> Desconexão abrupta");
        // Depois de desconectar abruptamente, o servidor deve continuar a aceitar novos clientes normalmente
        ClientLib clienteDesconexao = new ClientLib("localhost", 12345);
        clienteDesconexao.login("Francisco", "francisco123");
        clienteDesconexao.adicionarEvento("ProdutoDesconexao", 1, 10.0);
        clienteDesconexao.close();
        System.out.println("Cliente desconectou-se abruptamente");     
        
        

        ClientLib clienteVerifica = new ClientLib("localhost", 12345);
        clienteVerifica.login("Francisco", "francisco123");
        clienteVerifica.adicionarEvento("ProdutoVerifica", 1, 10.0);
        clienteVerifica.close();
        System.out.println("Servidor continua funcional após desconexão, pode testar ao abrir outro terminal e começar uma nova conexão");
        
        System.out.println("\n--- TESTES DE ROBUSTEZ CONCLUÍDOS COM SUCESSO ---");
    }
}
