package apps;
import client.ClientLib;
import common.Evento;
import java.io.IOException;
import java.util.*;

public class ClienteInterativo {
    
    private static void menuOperacoes(ClientLib client, Scanner scanner) {
        try {
            while (true) {
                System.out.println("\n=== MENU DE OPERAÇÕES ===");
                System.out.println("1. Adicionar Evento");
                System.out.println("2. Novo Dia");
                System.out.println("3. Consultar Quantidade Vendida");
                System.out.println("4. Consultar Volume de Vendas");
                System.out.println("5. Consultar Preço Médio");
                System.out.println("6. Consultar Preço Máximo");
                System.out.println("7. Filtrar Eventos por Produto");
                System.out.println("8. Barreira - Vendas Simultâneas");
                System.out.println("0. Logout");
                System.out.print("Escolha opção: ");

                int opcao = scanner.nextInt();
                scanner.nextLine();

                if (opcao == 1) {
                    System.out.print("Nome do produto: ");
                    String produto = scanner.nextLine();
                    System.out.print("Quantidade: ");
                    int quantidade = scanner.nextInt();
                    System.out.print("Preço: ");
                    double preco = scanner.nextDouble();
                    scanner.nextLine();

                    boolean sucesso = client.adicionarEvento(produto, quantidade, preco);
                    System.out.println(sucesso ? "Evento adicionado com sucesso!" : "Falha ao adicionar evento.");

                } else if (opcao == 2) {
                    boolean sucesso = client.novoDia();
                    System.out.println(sucesso ? "Novo dia iniciado!" : "Falha ao iniciar novo dia.");

                } else if (opcao == 3) {
                    System.out.print("Nome do produto: ");
                    String produto = scanner.nextLine();
                    System.out.print("Número de dias: ");
                    int dias = scanner.nextInt();
                    scanner.nextLine();

                    int quantidade = client.quantidadeVendas(produto, dias);
                    System.out.printf("Quantidade vendida de '%s' nos últimos %d dias: %d\n", produto, dias, quantidade);

                } else if (opcao == 4) {
                    System.out.print("Nome do produto: ");
                    String produto = scanner.nextLine();
                    System.out.print("Número de dias: ");
                    int dias = scanner.nextInt();
                    scanner.nextLine();

                    double volume = client.volumeVendas(produto, dias);
                    System.out.printf("Volume de vendas de '%s' nos últimos %d dias: %.2f€\n", produto, dias, volume);

                } else if (opcao == 5) {
                    System.out.print("Nome do produto: ");
                    String produto = scanner.nextLine();
                    System.out.print("Número de dias: ");
                    int dias = scanner.nextInt();
                    scanner.nextLine();

                    double preco = client.precoMedio(produto, dias);
                    System.out.printf("Preço médio de '%s' nos últimos %d dias: %.2f€\n", produto, dias, preco);

                } else if (opcao == 6) {
                    System.out.print("Nome do produto: ");
                    String produto = scanner.nextLine();
                    System.out.print("Número de dias: ");
                    int dias = scanner.nextInt();
                    scanner.nextLine();

                    double preco = client.precoMaximo(produto, dias);
                    System.out.printf("Preço máximo de '%s' nos últimos %d dias: %.2f€\n", produto, dias, preco);

                } else if (opcao == 7) {
                    System.out.print("Produtos separados por vírgula (ex: Pao,Leite): ");
                    String input = scanner.nextLine();
                    Set<String> produtos = new HashSet<>(Arrays.asList(input.split(",")));
                    
                    System.out.print("Dia: ");
                    int dia = scanner.nextInt();
                    scanner.nextLine();

                    List<Evento> eventos = client.filtrarEventos(produtos, dia);
                    System.out.println("\n=== Eventos Encontrados: " + eventos.size() + " ===");
                    for (Evento e : eventos) {
                        System.out.printf("Produto: %s | Qtd: %d | Preço: %.2f€ | Timestamp: %d\n",
                            e.getProduto(), e.getQuantidade(), e.getPreco(), e.getTimestamp());
                    }

                } else if (opcao == 8) {
                    System.out.print("Produto 1: ");
                    String produto1 = scanner.nextLine();
                    System.out.print("Produto 2: ");
                    String produto2 = scanner.nextLine();

                    System.out.println("Aguardando vendas simultâneas de '" + produto1 + "' e '" + produto2 + "'...");
                    boolean sucesso = client.aguardarVendasSimultaneas(produto1, produto2);
                    System.out.println(sucesso ? "Barreira desbloqueada!" : "Falha na barreira.");

                } else if (opcao == 0) {
                    System.out.println("Logout efetuado. A sair...");
                    break;
                } else {
                    System.out.println("Opção inválida.");
                }
            }
        } catch (Exception e) {
            System.err.println("Erro durante operações: " + e.getMessage());
            e.printStackTrace();
        }
    }

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
                        menuOperacoes(client, scanner);
                        break; // Sai do loop de autenticação
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
