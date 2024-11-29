package com.petcare;

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

import org.bson.Document;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.connection.ClusterConnectionMode;

public class AppointmentServer {
    private static final int PORT = 12345;
    private Set<Socket> connections;
    private MongoClient mongoClient;
    private MongoDatabase database;
    private Timer timer;
    private ServerSocket serverSocket;

    public AppointmentServer() {
        connections = new HashSet<>();
        initializeMongoConnection();
    }

    private void initializeMongoConnection() {
        String connectionString = "mongodb+srv://TesteUsuario:teste30112004@registros.p6qyb.mongodb.net/Registros?retryWrites=true&w=majority";
        
        MongoClientSettings settings = MongoClientSettings.builder()
            .applyConnectionString(new ConnectionString(connectionString))
            .applyToSslSettings(builder -> 
                builder.enabled(true)
                .invalidHostNameAllowed(true))
            .applyToSocketSettings(builder -> 
                builder.connectTimeout(30000, TimeUnit.MILLISECONDS)
                .readTimeout(30000, TimeUnit.MILLISECONDS))
            .applyToClusterSettings(builder -> 
                builder.serverSelectionTimeout(30000, TimeUnit.MILLISECONDS)
                .mode(ClusterConnectionMode.MULTIPLE))
            .retryWrites(true)
            .retryReads(true)
            .build();

        try {
            System.out.println("Iniciando conexão com MongoDB...");
            mongoClient = MongoClients.create(settings);
            database = mongoClient.getDatabase("Registros");
            
            boolean connected = false;
            int maxRetries = 3;
            int retryCount = 0;
            
            while (!connected && retryCount < maxRetries) {
                try {
                    database.runCommand(new Document("ping", 1));
                    connected = true;
                    System.out.println("Conexão com MongoDB estabelecida com sucesso!");
                    startAppointmentChecker();
                } catch (Exception e) {
                    retryCount++;
                    if (retryCount < maxRetries) {
                        System.out.println("Tentativa " + retryCount + " falhou. Tentando novamente em 5 segundos...");
                        Thread.sleep(5000);
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
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Servidor iniciado na porta " + PORT);
            startAppointmentChecker();

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    connections.add(clientSocket);
                    new ClientHandler(clientSocket, this).start();
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
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    System.out.println("\n=== Verificando agendamentos em: " + LocalDateTime.now() + " ===");
                    checkAppointments();
                } catch (Exception e) {
                    System.err.println("Erro ao verificar agendamentos: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }, 0, TimeUnit.MINUTES.toMillis(1));
    }

    private void checkAppointments() {
        try {
            MongoCollection<Document> appointments = database.getCollection("Agendamentos");
            LocalDateTime now = LocalDateTime.now();
            
            Document query = new Document();
            query.append("data", new Document("$exists", true));
            
            appointments.find(query).forEach(doc -> {
                try {
                    String dataStr = doc.getString("data");
                    String horaStr = doc.getString("horario");
                    
                    LocalDateTime appointmentDateTime = LocalDateTime.parse(
                        dataStr + "T" + horaStr
                    );
                    
                    long horasAte = ChronoUnit.HOURS.between(now, appointmentDateTime);
                    
                    if (horasAte == 24 || horasAte == 1) {
                        String message = createNotificationMessage(doc, horasAte);
                        broadcast(message);
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
        return String.format("Lembrete: Você tem um agendamento em %d horas", horasAte);
    }

    public void broadcast(String message) {
        Set<Socket> closedConnections = new HashSet<>();
        for (Socket socket : connections) {
            try {
                if (!socket.isClosed()) {
                    socket.getOutputStream().write(message.getBytes());
                    socket.getOutputStream().flush();
                } else {
                    closedConnections.add(socket);
                }
            } catch (IOException e) {
                System.err.println("Erro ao enviar mensagem para cliente: " + e.getMessage());
                closedConnections.add(socket);
            }
        }
        connections.removeAll(closedConnections);
    }

    public static void main(String[] args) {
        new AppointmentServer().start();
    }
}

class ClientHandler extends Thread {
    private Socket clientSocket;
    private AppointmentServer server;

    public ClientHandler(Socket socket, AppointmentServer server) {
        this.clientSocket = socket;
        this.server = server;
    }

    public void run() {
        try {
            // Aqui você pode implementar a lógica de tratamento das mensagens do cliente
            // Por exemplo, ler mensagens usando clientSocket.getInputStream()
            
            while (!clientSocket.isClosed()) {
                byte[] buffer = new byte[1024];
                int bytesRead = clientSocket.getInputStream().read(buffer);
                if (bytesRead == -1) break;
                
                String message = new String(buffer, 0, bytesRead);
                // Processa a mensagem recebida
                System.out.println("Mensagem recebida: " + message);
            }
        } catch (IOException e) {
            System.err.println("Erro na conexão com cliente: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Erro ao fechar socket: " + e.getMessage());
            }
        }
    }
}