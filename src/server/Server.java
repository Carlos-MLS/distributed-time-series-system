package server;
import common.Evento;
import common.TaggedConnection;
import common.TaggedConnection.Frame;
import common.User;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import protocol.Message;



class ServerWorker implements Runnable {
    private Socket socket; // Socket da conexão TCP com este cliente específico
    private UserManager userManager; // referência partilhada para a gestão de utilizadores
    private boolean autenticado = false; // bool para saber se o user está autenticado
    private String userAtual = null; // username  do user autenticado nesta sessão (null se não autenticado)
    private Database database; // Referência partilhada para a base de dados de séries temporais


    public ServerWorker(Socket socket, UserManager userManager, Database database) {
        this.socket = socket;
        this.userManager = userManager;
        this.database = database;
    }

    @Override
    public void run() { 
        try {
            TaggedConnection taggedConn = new TaggedConnection(socket);

            
            while(true) {
                // Recebe frame do cliente contendo tag (para associar resposta) + dados do pedido
                Frame frame = taggedConn.receive();
                int tag = frame.tag; // Tag que identifica este pedido, o servidor responde com a mesma tag
                
                // Deserializar os dados binários recebidos do cliente
                ByteArrayInputStream bais = new ByteArrayInputStream(frame.data);
                DataInputStream in = new DataInputStream(bais);
                byte tipoOperacao = in.readByte(); // Primeiro byte identifica o tipo de operação (1=REGISTAR, 2=LOGIN, etc)
    
                if (tipoOperacao == protocol.Message.REGISTAR) { //podia dizer diretamente == 1 mas se tiver uma constante melhor já que é mais legível
                    String username = in.readUTF();
                    String password = in.readUTF(); //readUTF lê uma string que foi escrita com writeUTF

                    User novoUser = new User(username, password);
                    boolean registouComSucesso = userManager.registarUser(novoUser);

                    //temos que serializar a resposta agora
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    DataOutputStream out = new DataOutputStream(byteArrayOutputStream);

                    if(registouComSucesso) {
                        out.writeByte(protocol.Message.STATUS_SUCCESSO);
                        out.writeUTF("Registo efetuado com sucesso"); 
                    } else {
                        out.writeByte(protocol.Message.STATUS_FALHOU);
                        out.writeUTF("Username já existe"); 

                    }
                    out.flush();

                    taggedConn.send(tag, byteArrayOutputStream.toByteArray());
                }
                else if (tipoOperacao == protocol.Message.LOGIN) { // ou seja return ==2
                    String username = in.readUTF();
                    String password = in.readUTF(); //readUTF lê uma string que foi escrita com writeUTF

                    boolean loginComSucesso = userManager.validarUser(username, password);

                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    DataOutputStream out = new DataOutputStream(byteArrayOutputStream);

                    if(loginComSucesso) {
                        autenticado = true;
                        userAtual = username;
                        out.writeByte(protocol.Message.STATUS_SUCCESSO);
                        out.writeUTF("Login efetuado com sucesso"); 
                    } else {
                        out.writeByte(protocol.Message.STATUS_FALHOU);
                        out.writeUTF("Username ou Password errados"); 

                    }
                    out.flush();
                    taggedConn.send(tag, byteArrayOutputStream.toByteArray());
                }

                else if (tipoOperacao == protocol.Message.ADICIONAR_EVENTO) { //
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    DataOutputStream out = new DataOutputStream(byteArrayOutputStream);

                    if(!autenticado) {
                        out.writeByte(protocol.Message.STATUS_NAO_AUTENTICADO);
                        out.writeUTF("Erro: Não autenticado. Por favor faça login ou registe-se primeiro.");
                        out.flush();
                        taggedConn.send(tag, byteArrayOutputStream.toByteArray());
                        continue;
                    }
                    String produto = in.readUTF();
                    int quantidade = in.readInt();
                    double preco = in.readDouble();

                    database.adicionarEvento(produto, quantidade, preco);

                    out.writeByte(protocol.Message.STATUS_SUCCESSO);
                    out.flush();
                    taggedConn.send(tag, byteArrayOutputStream.toByteArray());
                }
                else if (tipoOperacao == protocol.Message.NOVO_DIA) { //
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    DataOutputStream out = new DataOutputStream(byteArrayOutputStream);

                    if(!autenticado) {
                        out.writeByte(protocol.Message.STATUS_NAO_AUTENTICADO);
                        out.writeUTF("Erro: Não autenticado. Por favor faça login ou registe-se primeiro.");
                        out.flush();
                        taggedConn.send(tag, byteArrayOutputStream.toByteArray());
                        continue;
                    }

                    database.novoDia();
                    out.writeByte(protocol.Message.STATUS_SUCCESSO);
                    out.flush();
                    taggedConn.send(tag, byteArrayOutputStream.toByteArray());
                }
                else if (tipoOperacao == protocol.Message.QUANTIDADE_VENDAS) {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    DataOutputStream out = new DataOutputStream(byteArrayOutputStream);

                    if (!autenticado) {
                        out.writeByte(protocol.Message.STATUS_NAO_AUTENTICADO);
                        out.writeUTF("Não autenticado");
                        out.flush();
                        taggedConn.send(tag, byteArrayOutputStream.toByteArray());
                        continue;
                    }
                    
                    String produto = in.readUTF();
                    int dias = in.readInt();
                    
                    int resultado = database.quantidadeVendas(produto, dias);
                    
                    out.writeByte(protocol.Message.STATUS_SUCCESSO);
                    out.writeInt(resultado);
                    out.flush();
                    taggedConn.send(tag, byteArrayOutputStream.toByteArray());
                }
                else if (tipoOperacao == protocol.Message.VOLUME_VENDAS) {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    DataOutputStream out = new DataOutputStream(byteArrayOutputStream);

                    if (!autenticado) {
                        out.writeByte(protocol.Message.STATUS_NAO_AUTENTICADO);
                        out.writeUTF("Não autenticado");
                        out.flush();
                        taggedConn.send(tag, byteArrayOutputStream.toByteArray());
                        continue;
                    }
                    
                    String produto = in.readUTF();
                    int dias = in.readInt();
                    
                    double resultado = database.volumeVendas(produto, dias);
                    
                    out.writeByte(protocol.Message.STATUS_SUCCESSO);
                    out.writeDouble(resultado);
                    out.flush();
                    taggedConn.send(tag, byteArrayOutputStream.toByteArray());
                }
                else if (tipoOperacao == protocol.Message.PRECO_MEDIO) {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    DataOutputStream out = new DataOutputStream(byteArrayOutputStream);

                    if (!autenticado) {
                        out.writeByte(protocol.Message.STATUS_NAO_AUTENTICADO);
                        out.writeUTF("Não autenticado");
                        out.flush();
                        taggedConn.send(tag, byteArrayOutputStream.toByteArray());
                        continue;
                    }
                    
                    String produto = in.readUTF();
                    int dias = in.readInt();
                    
                    double resultado = database.precoMedio(produto, dias);
                    
                    out.writeByte(protocol.Message.STATUS_SUCCESSO);
                    out.writeDouble(resultado);
                    out.flush();
                    taggedConn.send(tag, byteArrayOutputStream.toByteArray());
                }
                else if (tipoOperacao == protocol.Message.PRECO_MAXIMO) {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    DataOutputStream out = new DataOutputStream(byteArrayOutputStream);

                    if (!autenticado) {
                        out.writeByte(protocol.Message.STATUS_NAO_AUTENTICADO);
                        out.writeUTF("Não autenticado");
                        out.flush();
                        taggedConn.send(tag, byteArrayOutputStream.toByteArray());
                        continue;
                    }
                    
                    String produto = in.readUTF();
                    int dias = in.readInt();
                    
                    double resultado = database.precoMaximo(produto, dias);
                    
                    out.writeByte(protocol.Message.STATUS_SUCCESSO);
                    out.writeDouble(resultado);
                    out.flush();
                    taggedConn.send(tag, byteArrayOutputStream.toByteArray());
                }

                else if (tipoOperacao == protocol.Message.FILTRAR_EVENTOS) {
                    // Preparar stream para serializar a resposta
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    DataOutputStream out = new DataOutputStream(byteArrayOutputStream);

                    // Verificar autenticação antes de processar pedido
                    if (!autenticado) {
                        out.writeByte(protocol.Message.STATUS_NAO_AUTENTICADO);
                        out.writeUTF("Não autenticado");
                        out.flush();
                        taggedConn.send(tag, byteArrayOutputStream.toByteArray());
                        continue;
                    }
                    
                    // Ler parâmetros do pedido enviados pelo cliente:
                    int numProdutos = in.readInt(); // Número de produtos no filtro (enviado primeiro pelo cliente)
                    Set<String> produtos= new HashSet<>(); // Conjunto de produtos para filtrar
                    for (int i = 0; i < numProdutos; i++) {
                        produtos.add(in.readUTF()); // Ler cada nome de produto enviado pelo cliente
                    }
                    int dia = in.readInt(); // Dia para filtrar eventos
                    
                    // Obter eventos filtrados da base de dados
                    List<Evento> eventosFiltrados = database.filtrarEventos(produtos, dia);
                    
                    // Criar um dicionário de produtos para evitar repetição de strings
                    //por ex: 100 eventos de "Laptop" -> enviamos "Laptop" 1 vez + índice 0 nos 100 eventos
                    Map<String, Integer> mapProdutosInd = new HashMap<>(); // Mapa produto -> índice no dicionário
                    List<String> produtosUnicos = new ArrayList<>(); // Lista ordenada de produtos únicos (o dicionário)
                    
                    // Identificar produtos únicos e atribuimos índices
                    for (Evento e : eventosFiltrados) {
                        if (!mapProdutosInd.containsKey(e.getProduto())) {
                            mapProdutosInd.put(e.getProduto(), produtosUnicos.size()); // Índice = posição na lista
                            produtosUnicos.add(e.getProduto());
                        }
                    }

                    // Protocolo de resposta em 3 partes:
                    out.writeByte(Message.STATUS_SUCCESSO);
                    
                    // 1 Enviamos o dicionário de produtos
                    out.writeInt(produtosUnicos.size()); // Número de produtos únicos
                    for (String produto : produtosUnicos) {
                        out.writeUTF(produto); // Cada produto enviado apenas UMA vez
                    }

                    // 2 Enviamos eventos com índices
                    out.writeInt(eventosFiltrados.size()); // Número total de eventos
                    for (Evento e : eventosFiltrados) {
                        out.writeInt(mapProdutosInd.get(e.getProduto())); // Índice do produto no dicionário 
                        out.writeInt(e.getQuantidade()); // Quantidade vendida
                        out.writeDouble(e.getPreco()); // Preço unitário
                        out.writeLong(e.getTimestamp()); // Timestamp da venda
                    }
                    out.flush();
                    taggedConn.send(tag, byteArrayOutputStream.toByteArray());
                }
                
                // Fase 5 de  Barreiras ->>> Vendas Simultâneas e Vendas Consecutivas
                else if (tipoOperacao == protocol.Message.VENDAS_SIMULTANEAS) {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    DataOutputStream out = new DataOutputStream(byteArrayOutputStream);

                    if (!autenticado) {
                        out.writeByte(protocol.Message.STATUS_NAO_AUTENTICADO);
                        out.writeUTF("Não autenticado");
                        out.flush();
                        taggedConn.send(tag, byteArrayOutputStream.toByteArray());
                        continue;
                    }
                    
                    // Ler os dois produtos a aguardar do pedido do cliente
                    String produto1 = in.readUTF(); // Primeiro produto
                    String produto2 = in.readUTF(); // Segundo produto
                    boolean result = false; // Resultado: true se venderam ambos, false se dia terminou antes
                    try {
                        // BLOQUEIA até ambos serem vendidos OU novoDia() cancelar
                        result = database.vendasSimultaneas(produto1, produto2);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    out.writeByte(protocol.Message.STATUS_SUCCESSO);
                    out.writeBoolean(result);
                    out.flush();
                    taggedConn.send(tag, byteArrayOutputStream.toByteArray());
                }
                else if (tipoOperacao == protocol.Message.VENDAS_CONSECUTIVAS) {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    DataOutputStream out = new DataOutputStream(byteArrayOutputStream);

                    if (!autenticado) {
                        out.writeByte(protocol.Message.STATUS_NAO_AUTENTICADO);
                        out.writeUTF("Não autenticado");
                        out.flush();
                        taggedConn.send(tag, byteArrayOutputStream.toByteArray());
                        continue;
                    }

                    // Ler quantas vendas consecutivas aguardar (enviado pelo cliente)
                    int n = in.readInt(); // Ex: n=3 → aguarda 3 vendas seguidas do mesmo produto
                    String produtoVendido = null; // Resultado: nome do produto vendido n vezes, ou null se dia terminou
                    try {
                        // BLOQUEIA até n vendas consecutivas OU novoDia() cancelar
                        produtoVendido = database.vendasConsecutivas(n);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    
                    // Protocolo de resposta: boolean (hasResult) + UTF (produto se hasResult=true)
                    out.writeByte(protocol.Message.STATUS_SUCCESSO);
                    if (produtoVendido != null) {
                        out.writeBoolean(true); // Sinaliza que há resultado
                        out.writeUTF(produtoVendido); // Nome do produto vendido n vezes consecutivas
                    } else {
                        out.writeBoolean(false); // Sinaliza que dia terminou sem satisfazer condição
                    }
                    out.flush();
                    taggedConn.send(tag, byteArrayOutputStream.toByteArray());
                }
            }
        } catch (EOFException e) {
            System.out.println("Cliente terminou a conexão: " + socket.getRemoteSocketAddress());
        } catch (IOException e) {
            System.err.println("Ocorreu algum erro com o cliente: " + socket.getRemoteSocketAddress());
            e.printStackTrace();
        }
        finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Erro ao fechar a socket: " + socket.getRemoteSocketAddress());
                e.printStackTrace();
            }
        }
    }
}



public class Server {
    public static void main(String[] args) throws IOException {
        // Parâmetros de configuração do servidor:
        // D = número máximo de dias mantidos no sistema, mais antigos descartamos
        // S = número máximo de séries temporais em memória simultâneas (restantes em disco)
        int D = 30; // Default: manter 30 dias de histórico
        int S = 5;  // Default: máximo 5 séries em memória


        // Ler parâmetros da linha de comando: java Server
        if (args.length >= 1) {
            D = Integer.parseInt(args[0]); //primeiro arg D
        }
        if (args.length >= 2) {
            S = Integer.parseInt(args[1]); // segundo argumento = S
        }
        System.err.println("Server iniciado com D=" + D + " e S=" + S); //para garantir que leu bem os argumentos
    
        // Criar socket servidor na porta 12345
        ServerSocket serverSocket = new ServerSocket(12345);
        System.out.println("Server está na porta " + serverSocket.getLocalPort());

        // Inicializar UserManager com alguns utilizadores de teste
        // É uma instância única partilhada por todos os ServerWorkers
        UserManager userManager = new UserManager();
        userManager.registarUser(new User("Francisco","francisco123"));
        userManager.registarUser(new User("Nuno","nuno123"));
        userManager.registarUser(new User("Carlos","carlos123"));
        System.out.println("Foram carregados " + userManager.getQtdUsers() + " users.");

        // Criar Database com parâmetros D e S
        // É uma instância única partilhada por todos os ServerWorkers
        Database database = new Database(D, S);
        
        //aceitar conexões e criar thread por cliente
        while (true) { 
            Socket clientSocket = serverSocket.accept(); // Bloqueia até novo cliente conectar
            System.out.println("Nova conexão de " + clientSocket.getRemoteSocketAddress());

            // Criar e iniciar thread dedicada para este cliente
            Thread worker = new Thread(new ServerWorker(clientSocket, userManager, database));           
            worker.start();
        }
    }
    


}