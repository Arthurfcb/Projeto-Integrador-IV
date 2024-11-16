package com.petcare;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import com.mongodb.*;
import com.mongodb.client.*;
import org.bson.Document;
import java.net.InetSocketAddress;
import java.util.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class AppointmentServer extends WebSocketServer {
    private Set<WebSocket> connections;
    private MongoClient mongoClient;
    private MongoDatabase database;
    private Timer timer;

    public AppointmentServer(int port) {
        super(new InetSocketAddress(port));
        connections = new HashSet<>();
        
        String mongoUri = "mongodb+srv://TesteUsuario:teste30112004@registros.p6qyb.mongodb.net/Registros?retryWrites=true&w=majority&appName=Registros";
        mongoClient = MongoClients.create(mongoUri);
        database = mongoClient.getDatabase("Registros");
        timer = new Timer();
        
        System.out.println("Iniciando servidor com conexão MongoDB...");
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        connections.add(conn);
        System.out.println("New connection established: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        connections.remove(conn);
        System.out.println("Closed connection: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // Handle incoming messages if needed
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("Error occurred on connection " + conn.getRemoteSocketAddress());
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("Appointment notification server started on port " + getPort());
        startNotificationChecker();
    }

    private void startNotificationChecker() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkAppointments();
            }
        }, 0, 60000); // Verificar a cada 1 minuto
    }

    private void checkAppointments() {
        try {
            MongoCollection<Document> appointments = database.getCollection("Agendamentos");
            LocalDateTime now = LocalDateTime.now();

            System.out.println("\n=== Verificando agendamentos em: " + now + " ===");

            appointments.find().forEach(appointment -> {
                String dateStr = appointment.getString("data");
                String timeStr = appointment.getString("horario");
                
                System.out.println("\nAgendamento encontrado:");
                System.out.println("Data: " + dateStr);
                System.out.println("Hora: " + timeStr);
                
                try {
                    LocalDateTime appointmentDateTime = LocalDateTime.parse(dateStr + "T" + timeStr);
                    long hoursUntilAppointment = ChronoUnit.HOURS.between(now, appointmentDateTime);
                    
                    System.out.println("Horas até o agendamento: " + hoursUntilAppointment);
                    
                    if (hoursUntilAppointment == 24 || hoursUntilAppointment == 1) {
                        System.out.println(">>> Enviando notificação! <<<");
                        String notification = createNotificationMessage(appointment, hoursUntilAppointment);
                        broadcast(notification);
                    }
                } catch (Exception e) {
                    System.err.println("Erro ao processar agendamento: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            System.err.println("Erro ao verificar agendamentos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String createNotificationMessage(Document appointment, long hours) {
        return String.format("{\"type\":\"appointment_reminder\",\"hours\":%d,\"client\":\"%s\",\"pet\":\"%s\",\"date\":\"%s\",\"time\":\"%s\"}",
            hours,
            appointment.getString("cliente"),
            appointment.getString("nome_pet"),
            appointment.getString("data"),
            appointment.getString("horario")
        );
    }

    public static void main(String[] args) {
        int port = 8887; // WebSocket server port
        AppointmentServer server = new AppointmentServer(port);
        server.start();
    }
} 