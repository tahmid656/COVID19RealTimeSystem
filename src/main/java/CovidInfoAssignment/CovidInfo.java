package CovidInfoAssignment;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.annotations.Benchmark;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.*;

public class CovidInfo {
    public static void main(String[] args) throws Exception {
        ScheduledExecutorService covidInfoGen = Executors.newScheduledThreadPool(1);// Producer
        ScheduledExecutorService customerGenerator = Executors.newScheduledThreadPool(1);// Part of the consumer, used to generate customer accommodated with 1 thread
        ExecutorService countryOptions = Executors.newCachedThreadPool(); // Part of producer, gets all the available countries and returns it to the customer for future use

        covidInfoGen.scheduleAtFixedRate(new notifyNewCase(), 0 , 10 , TimeUnit.SECONDS);//Cases update every 5 seconds
        Future<String[]> countries = countryOptions.submit(new getCountries());
        String[] availableCountries = countries.get();
        customerGenerator.scheduleAtFixedRate(new periodicCustomer(availableCountries), 0 , 2 , TimeUnit.SECONDS);//Cases update every 5 seconds
    }
}

//Producer
class notifyNewCase implements Runnable{
    @Override
    public void run() {
        BufferedReader br = null;
        String location = "covid-global-data.csv";
        String[] country = new String[238];
        int[] newCase = new int[238];
        int i = 0;
        //Read from csv file
        try {
            br = new BufferedReader(new FileReader(location));
            String headerLine = br.readLine(); //Dump first row from csv as not relevant
        } catch (IOException e) {
            e.printStackTrace();
        }
        String line;
        try {
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                country[i] = data[2];
                newCase[i] = Integer.parseInt(data[3]);
                i++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (int j = 0; j < country.length - 1; j++) {
            ExecutorService ex = Executors.newCachedThreadPool();
            try {
                ex.submit(new addToRabbitMQ(country[j], newCase[j]));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("--------------------------------------------------");
        System.out.println("[Producer]: COVID-19 Information has been updated!");
        System.out.println("--------------------------------------------------\n");
        //Push data to relevant queues
    }

}

//Producer
class addToRabbitMQ implements Runnable{
    String country;
    int newCases;

    public addToRabbitMQ (String country, int newCases)  throws IOException {
        this.country = country;
        this.newCases = newCases;
    }
    @Override
    public void run() {
        ConnectionFactory factory = new ConnectionFactory();
        try (Connection conn = factory.newConnection()) {
            Channel channel = conn.createChannel();
            channel.exchangeDeclare("direct-exchange", BuiltinExchangeType.DIRECT, true);
            channel.queueDeclare(country, false, false, false, null);
            channel.queueBind(country, "direct-exchange", country);   // routing key assigned
            channel.basicPublish("direct-exchange", country, null, Integer.toString(newCases).getBytes());
        } catch(IOException | TimeoutException e){
            e.printStackTrace();
        }
    }
}

class getCountries implements Callable{

    @Override
    public String[] call() throws Exception {
        String line = "";
        String location = "covid-global-data.csv";
        String[] country = new String[238];
        int i = 0;

        //Read from csv file
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(location));
            String headerLine = br.readLine(); //Dump first row from csv as not relevant
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            while((line = br.readLine()) != null){
                String[] data = line.split(",");
                country[i] = data[2];
                i++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return country;
    }
}

// Aperiodic Producer
class getAllInfo implements Runnable{
    int id;
    String infoType;

    getAllInfo(int id, String infoType) {
        this.id = id;
        this.infoType = infoType;
    }

    @Override
    public void run() {
        String line = "";
        String location = "covid-global-data.csv";
        String[] filteredData = new String[1];
        int currentLine = 0;

        //Read from csv file
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(location));
            String headerLine = br.readLine(); // Dump first row from csv as not relevant
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            while((line = br.readLine()) != null) {
                if (currentLine == id) {
                    filteredData = line.split(",");
                    break;
                }
                currentLine++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        ConnectionFactory factory = new ConnectionFactory();
        try (Connection conn = factory.newConnection()) {
            Channel channel = conn.createChannel();
            channel.exchangeDeclare("direct-exchange", BuiltinExchangeType.DIRECT, true);

            channel.queueDeclare(filteredData[2]+infoType, false, false, false, null);
            channel.queueBind(filteredData[2]+infoType, "direct-exchange", filteredData[2]+infoType);
            channel.basicPublish("direct-exchange", filteredData[2]+infoType, null, line.getBytes());
        } catch(IOException | TimeoutException e){
            e.printStackTrace();
        }
    }
}

// Random number generator
class getRandomNumber implements Callable {
    int min, max;
    String[] country;

    getRandomNumber (int min, int max, String[] country) {
        this.min = min;
        this.max = max;
        this.country = country;
    }
    @Override
    public Integer call() throws IOException, TimeoutException {
        Random random = new Random();
        int randomNumber = random.nextInt(max - min) + min;
        if (randomNumber % 2 == 0) {
            ExecutorService aperiodicCustomerGenerator = Executors.newFixedThreadPool(1);
            aperiodicCustomerGenerator.submit(new aperiodicCustomer(randomNumber, country));
        }
        return randomNumber;
    }
}


//Consumer
class periodicCustomer implements Runnable {
    String[] customerCountry;
    public periodicCustomer(String[] country) {
        this.customerCountry = country;
    }

    @Override
    public void run() {
        ExecutorService ex = Executors.newCachedThreadPool();
        Future<Integer> id = ex.submit(new getRandomNumber(0, 238, customerCountry));
        ConnectionFactory factory = new ConnectionFactory();
        try (Connection conn = factory.newConnection()) {
            Channel channel = conn.createChannel();
            channel.queueDeclare(customerCountry[id.get()], false, false, false, null);
            channel.basicConsume(customerCountry[id.get()], true, (x, message) -> {
                        String cases = new String(message.getBody(), "UTF-8");
                        try {
                            System.out.println(customerCountry[id.get()] +" with " + cases + " cases today.");
                        } catch (ExecutionException | InterruptedException e) {
                            e.printStackTrace();
                        }
                        System.out.println("\n");
                    }, x -> {}
            );
        } catch(IOException | TimeoutException | InterruptedException | ExecutionException e){
            e.printStackTrace();
        }
    }
}

class aperiodicCustomer implements Runnable {
    int id;
    String infoType = "AllInfo";
    String[] customerCountry;

    //RabbitMQ Channel
    aperiodicCustomer(int id, String[] country) throws IOException, TimeoutException {
        this.id = id;
        this.customerCountry = country;
    }

    @Override
    public void run() {
        ExecutorService aperiodicCustomerGenerator = Executors.newFixedThreadPool(1);
        aperiodicCustomerGenerator.submit(new getAllInfo(id, infoType));
        ConnectionFactory factory = new ConnectionFactory();
        try (Connection conn = factory.newConnection()) {
            Channel channel = conn.createChannel();
            channel.queueDeclare(customerCountry[id]+infoType, false, false, false, null);
            channel.basicConsume(customerCountry[id]+infoType, true, (x, message) -> {
                        String info = new String(message.getBody(), "UTF-8");
                        String data[] = info.split(",");
                        System.out.println(data[2] +" with " + data[3] + " cases today. Total cases: " + data[4] + ". New Deaths: " + data[5] + ". Cumulative Deaths: " + data[6] + ".\n");
                    }, x -> {}
            );
        } catch(IOException | TimeoutException e){
            e.printStackTrace();
        }
    }
}
