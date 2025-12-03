package client;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientLib {
    private Socket socket;
    private DataInputStream input;
    private DataOutputStream output;

    //Construtor vazio
    public ClientLib() {
        this.socket = null;
        this.input = null;
        this.output = null;
    }

    //Metodo construtor que cria a socket e os streams de input e output, é chamado no clientInterativo
    public ClientLib (String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.input = new DataInputStream(this.socket.getInputStream());
        this.output = new DataOutputStream(this.socket.getOutputStream());
    }

    //Envia para o servidor o pedido de registo de um novo user
    public boolean registar(String username, String password) throws IOException {
        this.output.writeByte(protocol.Message.REGISTAR);
        this.output.writeUTF(username);
        this.output.writeUTF(password);
        this.output.flush();

        byte status = this.input.readByte();
        String mensagem = this.input.readUTF();
        System.out.println(mensagem);

        return status == protocol.Message.STATUS_SUCCESSO;
    }

    //Envia para o servidor o pedido de login de um user
    public boolean login(String username, String password) throws IOException {
        this.output.writeByte(protocol.Message.LOGIN);
        this.output.writeUTF(username);
        this.output.writeUTF(password);
        this.output.flush();

        byte status = this.input.readByte();
        String mensagem = this.input.readUTF();
        System.out.println(mensagem);
        return status == protocol.Message.STATUS_SUCCESSO;
    }

    //Fecha a socket
    public void close() throws IOException {
        if (this.socket != null && !this.socket.isClosed()) {
            this.socket.close();    
        }
    }
    
    //
    public boolean adicionarEvento(String produto, int quantidade, double preco) throws IOException {
        this.output.writeByte(protocol.Message.ADICIONAR_EVENTO);
        this.output.writeUTF(produto);
        this.output.writeInt(quantidade);
        this.output.writeDouble(preco);
        this.output.flush();

        byte status = this.input.readByte();
        String mensagem = this.input.readUTF();
        System.out.println(mensagem);

        return status == protocol.Message.STATUS_SUCCESSO;
    }

    //Avança para o próximo dia, mesma cena do database mas temos aqui no client porque o client interativo chama este método
    public boolean novoDia() throws IOException {
        this.output.writeByte(protocol.Message.NOVO_DIA);
        this.output.flush();

        byte status = this.input.readByte();
        String mensagem = this.input.readUTF();
        System.out.println(mensagem);

        return status == protocol.Message.STATUS_SUCCESSO;
    }
}
