package ru.fisunov.http.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Server {
    private final int port;

    public Server(int port) {
        this.port = port;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Сервер поднят, порт: " + port);

            Map<String, MyWebApplication> router = new HashMap<>();
            router.put("/calculator", new CalculatorWebApplication());
            router.put("/greetings", new GreetingsWebApplication());
            router.put("/items", new ItemsWebApplication());
            System.out.println("Построен маппинг для точек назначения:");
            router.entrySet().forEach(e -> System.out.println(e.getKey() + ": " + e.getValue().getClass().getSimpleName()));

            System.out.println("Сервер готов к работе");

            while (true) {
                try (Socket socket = serverSocket.accept()) {
                    System.out.println("Клиент подключился");
                    Request request = new Request(socket.getInputStream());

                    if (request.isBadRequest()) { // проявился баг в postman, только при обращении через локальный интерфейс открывает
                                                  // второе соединение и в нем уже посылает запрос, а первое соединение закрывает с FIN
                                                  // пришлось вот так обойти. Операционка ubuntu
                        System.out.println("request is bad, continue");
                        continue;
                    }
                    request.show();
                    boolean executed = false;
                    for (Map.Entry<String, MyWebApplication> e : router.entrySet()) {
                        if (request.getUri().startsWith(e.getKey())) {
                            e.getValue().execute(request, socket.getOutputStream());
                            executed = true;
                            break;
                        }
                    }
                    if (!executed) {
                        socket.getOutputStream().write(("HTTP/1.1 404 Not Found\r\nContent-Type: text/html\r\n\r\n<html><body><h1>Unknown application</h1></body></html>").getBytes(StandardCharsets.UTF_8));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
