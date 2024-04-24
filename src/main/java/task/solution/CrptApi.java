package task.solution;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

// Класс для работы с API Честного знака
public class CrptApi {
    // Блокировка для обеспечения потокобезопасности
    private final Lock lock = new ReentrantLock();

    // Параметры для ограничения количества запросов к API
    private final int requestLimit; // Максимальное количество запросов
    private final long requestIntervalInMillis; // Промежуток времени между запросами
    private long lastRequestTime; // Время последнего запроса
    private int requestCounter; // Счетчик запросов

    // Конструктор класса
    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.requestLimit = requestLimit;
        this.requestIntervalInMillis = timeUnit.toMillis(1);
        this.lastRequestTime = System.currentTimeMillis();
        this.requestCounter = 0;
    }

    // Метод для создания документа в API
    public void createDocument(Document document, String signature) {
        try {
            lock.lock(); // Получаем блокировку

            // Проверяем превышение лимита запросов в заданном промежутке времени
            long currentTime = System.currentTimeMillis();
            long elapsedTimeSinceLastRequest = currentTime - lastRequestTime;
            if (elapsedTimeSinceLastRequest < requestIntervalInMillis && requestCounter >= requestLimit) {
                // Если лимит превышен, блокируем выполнение до истечения промежутка времени
                try {
                    Thread.sleep(requestIntervalInMillis - elapsedTimeSinceLastRequest);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // Обновляем время последнего запроса и счетчик запросов
            lastRequestTime = System.currentTimeMillis();
            requestCounter++;

            // Отправляем запрос к API для создания документа
            System.out.println("Sending request to API...");

            // Создаем HTTP клиент
            HttpClient httpClient = HttpClient.newHttpClient();

            // Формируем тело запроса
            String requestBody = convertToJsonString(document);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                    .header("Content-Type", "application/json")
                    .header("Signature", signature)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            // Отправляем запрос и получаем ответ
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                System.out.println("Response status code: " + response.statusCode());
                System.out.println("Response body: " + response.body());
            } catch (Exception e) {
                System.err.println("Failed to send request: " + e.getMessage());
            }
        } finally {
            lock.unlock(); // Освобождаем блокировку
        }
    }

    // Метод для чтения объекта документа из JSON файла
    public static Document readFromJsonFile(String filePath) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(new File(filePath), Document.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Метод для преобразования объекта документа в строку JSON
    public static String convertToJsonString(Document document) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(document);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Пример использования класса
    public static void main(String[] args) {
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 5); // Создаем экземпляр класса
        // Загружаем данные из файла JSON
        Document document = readFromJsonFile("document.json");
        crptApi.createDocument(document, "109");
    }
}

// Классы, описывающие структуру документа и его составляющих

@Data
class Document {
    private Description description;
    @JsonProperty("doc_id")
    private String docId;
    @JsonProperty("doc_status")
    private String docStatus;
    @JsonProperty("doc_type")
    private String docType;
    @JsonProperty("importRequest")
    private boolean importRequest;
    @JsonProperty("owner_inn")
    private String ownerInn;
    @JsonProperty("participant_inn")
    private String participantInn;
    @JsonProperty("producer_inn")
    private String producerInn;
    @JsonProperty("production_date")
    private String productionDate;
    @JsonProperty("production_type")
    private String productionType;
    private List<Product> products;
    @JsonProperty("reg_date")
    private String regDate;
    @JsonProperty("reg_number")
    private String regNumber;
}

@Data
class Description {
    private String participantInn;
}

@Data
class Product {
    @JsonProperty("certificate_document")
    private String certificateDocument;
    @JsonProperty("certificate_document_date")
    private String certificateDocumentDate;
    @JsonProperty("certificate_document_number")
    private String certificateDocumentNumber;
    @JsonProperty("owner_inn")
    private String ownerInn;
    @JsonProperty("producer_inn")
    private String producerInn;
    @JsonProperty("production_date")
    private String productionDate;
    @JsonProperty("tnved_code")
    private String tnvedCode;
    @JsonProperty("uit_code")
    private String uitCode;
    @JsonProperty("uitu_code")
    private String uituCode;
}
