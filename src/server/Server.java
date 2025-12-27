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
    private Socket socket;
    private UserManager userManager;
    private boolean autenticado = false; //temos este bool já que o cliente só pode fazer operações se tiver autenticado
    private String userAtual = null; //
    private Database database;


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
                Frame frame = taggedConn.receive();  // Recebe pedido com tag
                int tag = frame.tag;
                
                // Deserializar o pedido
                ByteArrayInputStream bais = new ByteArrayInputStream(frame.data);
                DataInputStream in = new DataInputStream(bais);
                byte tipoOperacao = in.readByte();
    
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
                    out.writeUTF("Evento adicionado com sucesso");
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
                    out.writeUTF("Avançou para o próximo dia com sucesso");
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
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    DataOutputStream out = new DataOutputStream(byteArrayOutputStream);

                    if (!autenticado) {
                        out.writeByte(protocol.Message.STATUS_NAO_AUTENTICADO);
                        out.writeUTF("Não autenticado");
                        out.flush();
                        taggedConn.send(tag, byteArrayOutputStream.toByteArray());
                        continue;
                    }
                    
                    int numProdutos = in.readInt();
                    Set<String> produtos= new HashSet<>();
                    for (int i = 0; i < numProdutos; i++) {
                        produtos.add(in.readUTF());
                    }
                    int dia = in.readInt();
                    
                    List<Evento> eventosFiltrados = database.filtrarEventos(produtos, dia);
                    

                    Map<String, Integer> mapProdutosInd = new HashMap<>(); //mapa para guardar o índice de cada produto na lista que vamos enviar
                    List<String> produtosUnicos = new ArrayList<>(); //lista para guardar os produtos únicos
                    for (Evento e : eventosFiltrados) {
                        if (!mapProdutosInd.containsKey(e.getProduto())) {
                            mapProdutosInd.put(e.getProduto(), produtosUnicos.size());
                            produtosUnicos.add(e.getProduto());
                        }
                    }

                    out.writeByte(Message.STATUS_SUCCESSO);
                    out.writeInt(produtosUnicos.size()); //primeiro enviamos o número de produtos únicos
                    for (String produto : produtosUnicos) {
                        out.writeUTF(produto); //depois enviamos cada produto único
                    }

                    out.writeInt(eventosFiltrados.size()); //enviamos eventos com o índice do produto na lista de prods
                    for (Evento e : eventosFiltrados) {
                        out.writeInt(mapProdutosInd.get(e.getProduto()));
                        out.writeInt(e.getQuantidade());
                        out.writeDouble(e.getPreco());
                        out.writeLong(e.getTimestamp());
                    }
                    out.flush();
                    taggedConn.send(tag, byteArrayOutputStream.toByteArray());
                }
                
                //fase 5, de vendas simultaneas e consecutivas
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
                    String produto1 = in.readUTF();
                    String produto2 = in.readUTF();
                    boolean result = false;
                    try {
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

                    int n = in.readInt(); //número de vendas consecutivas a aguardar
                    String produtoVendido = null;
                    try {
                        produtoVendido = database.vendasConsecutivas(n);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    out.writeByte(protocol.Message.STATUS_SUCCESSO);
                    if (produtoVendido != null) {
                        out.writeBoolean(true);
                        out.writeUTF(produtoVendido);
                    } else {
                        out.writeBoolean(false);
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
        ServerSocket serverSocket = new ServerSocket(12345);
        //print de debug da porta onde o servidor está a correr
        System.out.println("Server está na porta " + serverSocket.getLocalPort());

        //popular com alguns users para iniciais
        UserManager userManager = new UserManager();
        userManager.registarUser(new User("Francisco","francisco123"));
        userManager.registarUser(new User("Nuno","nuno123"));
        userManager.registarUser(new User("Carlos","carlos123"));

        //print para verificar se os users forma todos registados com sucesso e carregados
        System.out.println("Foram carregados " + userManager.getQtdUsers() + " users.");

        //criar a base de dados partilhada por todos os workers
        Database database = new Database();
        while (true) { 
            Socket clientSocket = serverSocket.accept();
            //print para verificar que houve uma nova conexão
            System.out.println("Nova conexão de " + clientSocket.getRemoteSocketAddress());

            Thread worker = new Thread(new ServerWorker(clientSocket, userManager, database));           
            worker.start();
        }
    }
    


}