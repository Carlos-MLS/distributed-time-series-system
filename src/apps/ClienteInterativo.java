package apps;
import client.ClientLib;
import java.io.IOException;
import java.util.Scanner;

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
                        System.out.println("Cliente autenticado. Agora pode usar o menu de operações.");
                        System.out.println("(Para testes automáticos, use: java -cp src apps.ClienteTestes)");
                        System.out.println("\nPressione Enter para continuar...");
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
