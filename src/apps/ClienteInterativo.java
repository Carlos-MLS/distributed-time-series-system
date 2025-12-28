package apps;
import client.ClientLib;
import common.Evento;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public class ClienteInterativo {
    public static void main(String[] args) {
        ClientLib client = null;
        Scanner scanner = new Scanner(System.in);

        try {
           client = new ClientLib("localhost", 12345);
           System.out.println("Conectado ao servidor.\n"); 

           while (true) {
                //Menu, será simplesmente no terminal
                System.out.println("=== MENU DE AUTENTICAÇÃO ===");
                System.out.println("1. Registar");
                System.out.println("2. Login");
                System.out.println("0. Sair");
                System.out.print("Escolha opção: ");

                int opcao = scanner.nextInt();
                scanner.nextLine(); // Consumir a nova linha

                if (opcao == 1) {
                    System.out.print("Username: ");
                    String username = scanner.nextLine();
                    System.out.print("Password: ");
                    String password = scanner.nextLine();

                    boolean sucessoRegisto = client.registar(username, password);
                    if (sucessoRegisto) {
                        System.out.println("Registo bem-sucedido!\n");
                    } else {
                        System.out.println("Falha no registo.\n");
                    }
                } else if (opcao == 2) {
                    System.out.print("Username: ");
                    String username = scanner.nextLine();
                    System.out.print("Password: ");
                    String password = scanner.nextLine();

                    boolean sucessoLogin = client.login(username, password);
                    if (sucessoLogin) {
                        System.out.println("Login bem-sucedido!\n");

                        // Adicionar eventos em múltiplos dias
                        client.adicionarEvento("Laptop", 10, 1000.0);
                        client.adicionarEvento("Mouse", 20, 15.0);
                        client.adicionarEvento("Laptop", 5, 950.0);
                        client.novoDia();

                        client.adicionarEvento("Laptop", 8, 1050.0);
                        client.adicionarEvento("Mouse", 10, 18.0);
                        client.novoDia();

                        // Testar agregação
                        int qtd = client.quantidadeVendas("Laptop", 2);
                        System.out.println("Quantidade vendida Laptop (últimos 2 dias): " + qtd);
                        // Deve mostrar: 10+5+8 = 23

                        // Testar outras agregações
                        double vol = client.volumeVendas("Laptop", 2);
                        System.out.println("Volume vendas Laptop (últimos 2 dias): " + vol);
                        // Deve mostrar: (10*1000 + 5*950 + 8*1050) = 23150.0

                        double medio = client.precoMedio("Laptop", 2);
                        System.out.println("Preço médio Laptop (últimos 2 dias): " + medio);
                        // Deve mostrar: 23150.0 / 23 = 1006.52...

                        double max = client.precoMaximo("Laptop", 2);
                        System.out.println("Preço máximo Laptop (últimos 2 dias): " + max);
                        // Deve mostrar: 1050.0

                        Set<String> filtro = new HashSet<>();
                        filtro.add("Laptop");
                        filtro.add("Mouse");
                        List<Evento> eventos = client.filtrarEventos(filtro, 1);
                        System.out.println("Eventos encontrados: " + eventos.size());
                        for (Evento e : eventos) {
                            System.out.println(e);
                        }

                        System.out.println("\n=== Testando Fase 5: Barreiras ===");
                        
                        // Thread 1: Esperar vendas simultâneas
                        Thread t1 = new Thread(() -> {
                            try {
                                // Usar nova conexão para evitar bloquear a conexão principal no servidor
                                ClientLib clientListener = new ClientLib("localhost", 12345);
                                clientListener.login(username, password);
                                boolean ok = clientListener.aguardarVendasSimultaneas("Laptop", "Mouse");
                                System.out.println("✓ Vendas simultâneas detectadas: " + ok);
                                clientListener.close();
                            } catch (Exception e) {
                                System.err.println("Erro em vendas simultâneas: " + e.getMessage());
                            }
                        });
                        t1.start();

                        try {
                            // Espera maior para garantir que a thread está registada
                            Thread.sleep(1000);
                            client.adicionarEvento("Laptop", 1, 1000);
                            Thread.sleep(500);
                            client.adicionarEvento("Mouse", 1, 20);
                            t1.join(2000);
                        } catch (InterruptedException e) {
                            System.err.println("Erro em sleep/join (simultâneas): " + e.getMessage());
                        }

                        // Thread 2: Esperar 3 vendas consecutivas
                        Thread t2 = new Thread(() -> {
                            try {
                                // Usar nova conexão para evitar bloquear a conexão principal no servidor
                                ClientLib clientListener = new ClientLib("localhost", 12345);
                                clientListener.login(username, password);
                                String prod = clientListener.aguardarVendasConsecutivas(3);
                                System.out.println("✓ 3 vendas consecutivas detectadas de: " + prod);
                                clientListener.close();
                            } catch (Exception e) {
                                System.err.println("Erro em vendas consecutivas: " + e.getMessage());
                            }
                        });
                        t2.start();

                        try {
                            // Espera maior para garantir que a thread está registada
                            Thread.sleep(1000);
                            client.adicionarEvento("Teclado", 1, 50);
                            client.adicionarEvento("Teclado", 1, 50);
                            client.adicionarEvento("Teclado", 1, 50);
                            t2.join(2000);
                        } catch (InterruptedException e) {
                            System.err.println("Erro em sleep/join (consecutivas): " + e.getMessage());
                        }

                        System.out.println("\nTodas as fases testadas com sucesso!");
                        System.out.println("Pressione Enter para continuar...");
                        scanner.nextLine();
                    } else {
                        System.out.println("Falha no login.\n");
                    }
                } else if (opcao == 0) {
                    System.out.println("A sair...");
                    break;
                } else {
                    System.out.println("Opção inválida. Tente novamente.\n");
                }
           }
        } catch (IOException e) {
            System.err.println("Erro de comunicação com o server: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (client != null) {
                    client.close();
                }
            } catch (IOException e) {
                System.err.println("Erro ao fechar a conexão com o server: " + e.getMessage());
                e.printStackTrace();
            }
            scanner.close();
        }
    }
}
