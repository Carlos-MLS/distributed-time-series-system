package server;
import common.User;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

public class UserManager {
    // Armazenamento de utilizadores: chave = username, valor = objeto User (username + password)
    // n podemos usar concurrent hashmaps, logo temos de usar locks
    private HashMap<String, User> users;

    // lock para garantir thread-safety em operações sobre o HashMap
    // necessário porque múltiplas ServerWorker threads acedem concorrentemente
    private ReentrantLock lock = new ReentrantLock();

    //Construtor vazio
    public UserManager() {
        this.users = new HashMap<>();
    }

    //Construtor com parametros
    public UserManager(HashMap<String, User> users) {
        this.users = users;
    }

    //Getters
    public HashMap<String, User> getUsers() {
        return this.users;
    }

    //Setters
    public void setUsers(HashMap<String, User> users) {
        this.users = users;
    }

    //toString
    public String toString() {
        return "UserManager{" +
               "users=" + users +
               '}';
    }

    //clone
    public UserManager clone() {
        return new UserManager((HashMap<String, User>) this.users.clone());
    }

    //equals
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        UserManager um = (UserManager) o;
        return this.users.equals(um.getUsers());
    }

    /*------------------------Métodos para a classe------------------------*/

    // Método auxiliar privado para verificar existência de username
    // NÃO usa lock porque é sempre chamado DENTRO de métodos que já adquiriram o lock
    private boolean userExists(String username) {
        return this.users.containsKey(username);
    } 

    // Registar novo utilizador no sistema
    public boolean registarUser(User user) {
        lock.lock();
        try {
            if(userExists(user.getUsername())) {
                return false; // Username já existe, registo falhou
            } 
            else {
                // Criar novo objeto User (cópia defensiva) e inserir no HashMap
                User novoUser = new User(user.getUsername(), user.getPassword());
                users.put(user.getUsername(), novoUser);
                return true; // Registo bem-sucedido
            }
        } finally {
            lock.unlock(); // SEMPRE libertar lock no finally (mesmo se exception)
        }
    }

    // Validar credenciais de login
    // Verifica se username existe E se password corresponde
    public boolean validarUser(String username, String password) { 
        lock.lock();
        try {
            // Tentar obter User do HashMap
            User user = users.get(username); // null se username não existir
            if(user == null) { 
                return false; // Username não encontrado
            }
            if(!user.getPassword().equals(password)) {
                return false; // Password incorreta
            }
            return true; // Credenciais válidas
        } finally {
            lock.unlock();
        }
    }

    // Método auxiliar para contar utilizadores registados (usado em testes/debug)
    public int getQtdUsers() {
        lock.lock();
        try {
            return users.size();
        } finally {
            lock.unlock();
        }
    }
}