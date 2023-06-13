import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import org.json.JSONObject;

public class Main {
    public static final String CONFIG_FILE = "config.txt";
    public static final String DATA_FILE = "exchange_data.json";

    public static void main(String[] args) {
        try {
            // Загрузка конфигурации
            JSONObject config = loadConfigFromFile();
            if (config != null) {
                String apiUrl = config.optString("api_url");
                String apiId = config.optString("api_id");

                // Проверка наличия URL и ID
                if (apiUrl.isEmpty() || apiId.isEmpty()) {
                    System.out.println("URL или ID API не определены в конфигурационном файле.");
                    return;
                }

                // Проверка правильности URL и ID
                try {
                    if (!isValidURL(apiUrl)) {
                        JOptionPane.showMessageDialog(null, "Неправильный формат URL: " + apiUrl, "Ошибка", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    if (!isValidAPIId(apiId)) {
                        JOptionPane.showMessageDialog(null, "Неправильный формат ID API: " + apiId, "Ошибка", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                } catch (IllegalArgumentException e) {
                    JOptionPane.showMessageDialog(null, e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Сохранение конфигурации в файл
                saveConfigToFile(config);

                // Проверка на наличие сохраненных данных
                JSONObject rates;
                if (isDataAvailable()) {
                    System.out.println("Загрузка обменных курсов из файла...");
                    rates = loadExchangeRatesFromFile();
                } else {
                    System.out.println("Получение обменных курсов из API...");
                    try {
                        rates = fetchExchangeRatesFromAPI(apiUrl, apiId);
                        if (rates != null) {
                            saveExchangeRatesToFile(rates);
                        } else {
                            JOptionPane.showMessageDialog(null, "Не удалось получить обменные курсы из API.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    } catch (IOException e) {
                        JOptionPane.showMessageDialog(null, "Не удалось получить обменные курсы из API. Проверьте URL и ID.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }

                SimpleGUI app = new SimpleGUI(rates);
                app.setVisible(true);
            } else {
                JOptionPane.showMessageDialog(null, "Ошибка при загрузке конфигурационного файла.", "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static JSONObject loadConfigFromFile() {
        try {
            FileReader reader = new FileReader(CONFIG_FILE);
            BufferedReader bufferedReader = new BufferedReader(reader);
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                response.append(line);
            }
            bufferedReader.close();

            return new JSONObject(response.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void saveConfigToFile(JSONObject config) {
        try {
            FileWriter writer = new FileWriter(CONFIG_FILE);
            writer.write(config.toString());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean isValidURL(String url) {
        try {
            new URL(url);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    private static boolean isValidAPIId(String apiId) {
        // Дополнительные проверки правильности ID API
        // Можно добавить специфичные для вашего API проверки здесь
        // В данном примере просто проверяем, что ID не пустое и состоит только из букв и цифр
        return !apiId.isEmpty() && apiId.matches("^[a-zA-Z0-9]+$");
    }

    private static boolean isDataAvailable() {
        File file = new File(DATA_FILE);
        return file.exists();
    }

    private static JSONObject loadExchangeRatesFromFile() {
        try {
            FileReader reader = new FileReader(DATA_FILE);
            BufferedReader bufferedReader = new BufferedReader(reader);
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                response.append(line);
            }
            bufferedReader.close();

            JSONObject jsonObject = new JSONObject(response.toString());
            return jsonObject.getJSONObject("rates");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static JSONObject fetchExchangeRatesFromAPI(String apiUrl, String apiId) throws IOException {
        try {
            String response = sendGetRequest(apiUrl + "?app_id=" + apiId);
            JSONObject jsonObject = new JSONObject(response);
            return jsonObject.getJSONObject("rates");
        } catch (IOException e) {
            throw new IOException("Ошибка при выполнении запроса к API: " + e.getMessage());
        }
    }

    private static void saveExchangeRatesToFile(JSONObject rates) {
        try {
            FileWriter writer = new FileWriter(DATA_FILE);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("rates", rates);
            writer.write(jsonObject.toString());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String sendGetRequest(String url) throws IOException {
        URL obj = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
        connection.setRequestMethod("GET");
        connection.setReadTimeout(5000);
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            return response.toString();
        } else {
            throw new IOException("Ошибка HTTP-запроса. Код ответа: " + responseCode);
        }
    }
}

class SimpleGUI extends JFrame {
    private JComboBox<String> comboBox1;
    private JComboBox<String> comboBox2;
    private JSONObject exchangeRates;

    public SimpleGUI(JSONObject rates) {
        this.exchangeRates = rates;
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Конвертер валют");
        setSize(300, 150);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        Container container = getContentPane();
        container.setLayout(new GridLayout(3, 2, 2, 2));
        String[] currencies = getCurrencies();
        comboBox1 = new JComboBox<>(currencies);
        comboBox2 = new JComboBox<>(currencies);
        comboBox1.setEditable(true);
        comboBox2.setEditable(true);
        container.add(comboBox1);
        container.add(comboBox2);
        JButton button = new JButton("Конвертировать");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                convertCurrency();
            }
        });
        container.add(button);
    }

    private String[] getCurrencies() {
        if (exchangeRates != null) {
            java.util.Set<String> keys = exchangeRates.keySet();
            java.util.List<String> currenciesList = new java.util.ArrayList<>(keys);
            return currenciesList.toArray(new String[0]);
        }
        return new String[0];
    }

    private void convertCurrency() {
        String currency1 = (String) comboBox1.getSelectedItem();
        String currency2 = (String) comboBox2.getSelectedItem();
        double amount;
        try {
            amount = Double.parseDouble(JOptionPane.showInputDialog(null, "Введите сумму для конвертации:"));
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Неправильный формат суммы.", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (exchangeRates != null) {
            if (!exchangeRates.has(currency1)) {
                JOptionPane.showMessageDialog(null, "Неправильная валюта 1: " + currency1, "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!exchangeRates.has(currency2)) {
                JOptionPane.showMessageDialog(null, "Неправильная валюта 2: " + currency2, "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }

            double rate1 = exchangeRates.optDouble(currency1);
            double rate2 = exchangeRates.optDouble(currency2);

            if (rate1 == 0) {
                JOptionPane.showMessageDialog(null, "Обменный курс для валюты 1 равен 0.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }

            double convertedAmount = amount * rate2 / rate1;
            JOptionPane.showMessageDialog(null, amount + " " + currency1 + " = " + convertedAmount + " " + currency2, "Результат", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
