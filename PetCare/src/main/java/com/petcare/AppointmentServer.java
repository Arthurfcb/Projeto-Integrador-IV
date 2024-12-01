package com.petcare;
// Define o pacote do código como "com.petcare", útil para organização e modularização.

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
// Importa bibliotecas essenciais para operações de rede, controle de tempo e manipulação de data/hora.

import org.bson.Document;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.connection.ClusterConnectionMode;
// Importa bibliotecas relacionadas ao MongoDB para conexão e manipulação do banco de dados.

public class AppointmentServer {
// Define a classe principal "AppointmentServer", que gerencia o servidor de agendamentos.

    private static final int PORT = 12345;
    // Define a porta padrão para o servidor.
    
    private Set<Socket> connections;
    // Um conjunto para armazenar conexões ativas com os clientes.
    
    private MongoClient mongoClient;
    private MongoDatabase database;
    // Objetos para gerenciar a conexão e o banco de dados MongoDB.
    
    private Timer timer;
    // Usado para executar tarefas repetitivas, como verificações de agendamento.
    
    private ServerSocket serverSocket;
    // Representa o socket do servidor para aceitar conexões dos clientes.

    public AppointmentServer() {
        connections = new HashSet<>();
        // Inicializa o conjunto de conexões.
        
        initializeMongoConnection();
        // Configura a conexão com o banco de dados MongoDB.
    }

    private void initializeMongoConnection() {
        // Método responsável por inicializar a conexão com o MongoDB.
        String connectionString = "mongodb+srv://TesteUsuario:teste30112004@registros.p6qyb.mongodb.net/Registros?retryWrites=true&w=majority";
        // URL de conexão com o banco MongoDB.

        MongoClientSettings settings = MongoClientSettings.builder()
            // Configurações detalhadas para a conexão com o MongoDB.
            .applyConnectionString(new ConnectionString(connectionString))
            .applyToSslSettings(builder -> 
                builder.enabled(true)
                .invalidHostNameAllowed(true))
            // Habilita SSL e permite nomes de host inválidos.
            .applyToSocketSettings(builder -> 
                builder.connectTimeout(30000, TimeUnit.MILLISECONDS)
                .readTimeout(30000, TimeUnit.MILLISECONDS))
            // Configura timeout de conexão e leitura.
            .applyToClusterSettings(builder -> 
                builder.serverSelectionTimeout(30000, TimeUnit.MILLISECONDS)
                .mode(ClusterConnectionMode.MULTIPLE))
            // Configura o modo do cluster para permitir múltiplas conexões.
            .retryWrites(true)
            .retryReads(true)
            .build();

        try {
            System.out.println("Iniciando conexão com MongoDB...");
            mongoClient = MongoClients.create(settings);
            // Cria o cliente MongoDB com as configurações especificadas.
            
            database = mongoClient.getDatabase("Registros");
            // Obtém a referência ao banco de dados "Registros".

            boolean connected = false;
            int maxRetries = 3;
            int retryCount = 0;
            // Variáveis para gerenciar tentativas de reconexão.

            while (!connected && retryCount < maxRetries) {
                try {
                    database.runCommand(new Document("ping", 1));
                    // Testa a conexão com o banco de dados enviando um comando "ping".
                    connected = true;
                    System.out.println("Conexão com MongoDB estabelecida com sucesso!");
                    startAppointmentChecker();
                    // Inicia o verificador de agendamentos após conexão bem-sucedida.
                } catch (Exception e) {
                    retryCount++;
                    if (retryCount < maxRetries) {
                        System.out.println("Tentativa " + retryCount + " falhou. Tentando novamente em 5 segundos...");
                        Thread.sleep(5000);
                        // Espera 5 segundos antes de tentar novamente.
                    } else {
                        throw new RuntimeException("Falha ao conectar ao MongoDB após " + maxRetries + " tentativas", e);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erro fatal ao conectar ao MongoDB: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Falha ao inicializar o servidor", e);
        }
    }

    public void start() {
        // Método principal para iniciar o servidor.
        try {
            serverSocket = new ServerSocket(PORT);
            // Cria um socket do servidor na porta especificada.
            System.out.println("Servidor iniciado na porta " + PORT);
            startAppointmentChecker();
            // Inicia o verificador de agendamentos.

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    // Aceita conexões de clientes.
                    connections.add(clientSocket);
                    // Adiciona a conexão ao conjunto de conexões ativas.
                    new ClientHandler(clientSocket, this).start();
                    // Inicia uma nova thread para gerenciar a comunicação com o cliente.
                } catch (IOException e) {
                    System.out.println("Erro ao aceitar conexão: " + e.getMessage());
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao iniciar o servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void startAppointmentChecker() {
        // Configura uma tarefa para verificar agendamentos periodicamente.
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    System.out.println("\n=== Verificando agendamentos em: " + LocalDateTime.now() + " ===");
                    checkAppointments();
                    // Executa a verificação de agendamentos.
                } catch (Exception e) {
                    System.err.println("Erro ao verificar agendamentos: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }, 0, TimeUnit.MINUTES.toMillis(1));
        // Configura a tarefa para rodar a cada 1 minuto.
    }

    private void checkAppointments() {
        // Verifica os agendamentos no banco de dados.
        try {
            MongoCollection<Document> appointments = database.getCollection("Agendamentos");
            // Obtém a coleção "Agendamentos" do banco de dados.

            LocalDateTime now = LocalDateTime.now();
            // Obtém a data e hora atual.

            Document query = new Document();
            query.append("data", new Document("$exists", true));
            // Cria uma consulta para buscar documentos com o campo "data".

            appointments.find(query).forEach(doc -> {
                try {
                    String dataStr = doc.getString("data");
                    String horaStr = doc.getString("horario");
                    // Obtém as informações de data e horário do documento.

                    LocalDateTime appointmentDateTime = LocalDateTime.parse(
                        dataStr + "T" + horaStr
                    );
                    // Converte os valores para um objeto LocalDateTime.

                    long horasAte = ChronoUnit.HOURS.between(now, appointmentDateTime);
                    // Calcula o número de horas até o agendamento.

                    if (horasAte == 24 || horasAte == 1) {
                        String message = createNotificationMessage(doc, horasAte);
                        broadcast(message);
                        // Envia uma notificação se o agendamento estiver próximo.
                    }
                } catch (Exception e) {
                    System.err.println("Erro ao processar agendamento: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            System.err.println("Erro ao verificar agendamentos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String createNotificationMessage(Document appointment, long horasAte) {
        // Cria a mensagem de notificação para o cliente.
        return String.format("Lembrete: Você tem um agendamento em %d horas", horasAte);
    }

    public void broadcast(String message) {
        // Envia uma mensagem para todos os clientes conectados.
        Set<Socket> closedConnections = new HashSet<>();
        for (Socket socket : connections) {
            try {
                if (!socket.isClosed()) {
                    socket.getOutputStream().write(message.getBytes());
                    socket.getOutputStream().flush();
                    // Envia a mensagem via socket.
                } else {
                    closedConnections.add(socket);
                }
            } catch (IOException e) {
                System.err.println("Erro ao enviar mensagem para cliente: " + e.getMessage());
                closedConnections.add(socket);
            }
        }
        connections.removeAll(closedConnections);
        // Remove conexões fechadas do conjunto.
    }

    public static void main(String[] args) {
        new AppointmentServer().start();
        // Inicia o servidor.
    }
}

class ClientHandler extends Thread {
    // Classe para gerenciar a comunicação com cada cliente.
    private Socket clientSocket;
    private AppointmentServer server;

    public ClientHandler(Socket socket, AppointmentServer server) {
        this.clientSocket = socket;
        this.server = server;
    }

    public void run() {
        try {
            // Loop para processar mensagens do cliente.
            while (!clientSocket.isClosed()) {
                byte[] buffer = new byte[1024];
                int bytesRead = clientSocket.getInputStream().read(buffer);
               
