import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

public class Main {
    public static final String API_URL = "https://openexchangerates.org/api/latest.json";
    public static final String API_ID = "d066d12bd3f64461b0e3a126c28a0858";
    public static final String DATA_FILE = "exchange_data.json";
    public static void main(String[] args) {
        // Проверка на наличие сохраненных данных
        JSONObject rates;
        if (isDataAvailable()) {
            System.out.println("Loading exchange rates from file...");
            rates = loadExchangeRatesFromFile();
        } else {
            System.out.println("Fetching exchange rates from API...");
            rates = fetchExchangeRatesFromAPI();
            saveExchangeRatesToFile(rates);
        }
        if (rates != null) {
            SimpleGUI app = new SimpleGUI(rates);
            app.setVisible(true);
        } else {
            System.out.println("Exchange rates not available. GUI cannot be created.");
        }
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
    private static JSONObject fetchExchangeRatesFromAPI() {
        try {
            String response = sendGetRequest(API_URL + "?app_id=" + API_ID);
            JSONObject jsonObject = new JSONObject(response);
            return jsonObject.getJSONObject("rates");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
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
            throw new IOException("HTTP request failed. Response Code: " + responseCode);
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
        setTitle("Currency Converter");
        setSize(300, 150);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        Container container = getContentPane();
        container.setLayout(new GridLayout(3, 2, 2, 2));
        String[] string2 = {"USD", "EUR", "BYN","KZT", "UAH", "JPY", "CNY", "AED", "RUB"};
        comboBox1 = new JComboBox<>(string2);
        comboBox2 = new JComboBox<>(string2);
        comboBox1.setEditable(true);
        comboBox2.setEditable(true);
        container.add(comboBox1);
        container.add(comboBox2);
        JButton button = new JButton("Convert");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                convertCurrency();
            }
        });
        container.add(button);
    }
    private void convertCurrency() {
        String currency1 = (String) comboBox1.getSelectedItem();
        String currency2 = (String) comboBox2.getSelectedItem();

        if (currency1.equals(currency2)) {
            JOptionPane.showMessageDialog(this, "Please select different currencies", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            double rate1 = exchangeRates.getDouble(currency1);
            double rate2 = exchangeRates.getDouble(currency2);
            double amount = 1.0;
            try {
                amount = Double.parseDouble(JOptionPane.showInputDialog(this, "Enter amount:", "Currency Converter", JOptionPane.PLAIN_MESSAGE));
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid amount", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            double result = amount * rate2 / rate1;
            JOptionPane.showMessageDialog(this, amount + " " + currency1 + " = " + result + " " + currency2, "Result", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "An error occurred", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
