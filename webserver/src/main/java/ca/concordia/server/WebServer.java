package ca.concordia.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.constant.Constable;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

//create the WebServer class to receive connections on port 5000. Each connection is handled by a master thread that puts the descriptor in a bounded buffer. A pool of worker threads take jobs from this buffer if there are any to handle the connection.
public class WebServer {

	private static HashMap<String, Account> accounts = new HashMap<>();
	private static String filePath = "C:\\Users\\mehma\\Downloads\\webserver\\webserver\\src\\main\\resources\\accounts.txt";

	public WebServer() {
		loadAccounts();
	}

	// Method to load accounts
	private static void loadAccounts() {
		try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
			String line;
			reader.readLine();
			while ((line = reader.readLine()) != null) {
				String[] parts = line.trim().split(",");
				if (parts.length == 2) {
					String id = parts[0].trim();
					int balance = Integer.parseInt(parts[1].trim());
					accounts.put(id, new Account(balance, Integer.parseInt(id)));
				}
			}
			System.out.println("Loaded " + accounts.size() + " accounts");
		} catch (IOException e) {
			System.err.println("Error loading accounts: " + e.getMessage());
		}
	}

	// Method to store accounts
	private static void storeAccounts() {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
			writer.write("Account id, balance\n");
			for (Map.Entry<String, Account> entry : accounts.entrySet()) {
				writer.write(entry.getKey() + ", " + entry.getValue().getBalance() + "\n");
			}
			System.out.println("Accounts saved successfully");
		} catch (IOException e) {
			System.err.println("Error saving accounts: " + e.getMessage());
		}
	}

	public void start() throws java.io.IOException{
		//Create a server socket
		ServerSocket serverSocket = new ServerSocket(5000);
		while(true){
			System.out.println("Waiting for a client to connect...");
			//Accept a connection from a client
			Socket clientSocket = serverSocket.accept();
			System.out.println("New client...");


			Thread thread = new Thread(() -> {

				try {


					BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
					OutputStream out = clientSocket.getOutputStream();

					String request = in.readLine();
					if (request != null) {
						if (request.startsWith("GET")) {
							// Handle GET request
							handleGetRequest(out);
						} else if (request.startsWith("POST")) {
							// Handle POST request
							handlePostRequest(in, out);
						}
					}

					in.close();
					out.close();
					clientSocket.close();
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}
			});

			thread.start();
		}
	}

	private static void handleGetRequest(OutputStream out) throws IOException {
		// Respond with a basic HTML page
		System.out.println("Handling GET request");
		String response = "HTTP/1.1 200 OK\r\n\r\n" +
				"<!DOCTYPE html>\n" +
				"<html>\n" +
				"<head>\n" +
				"<title>Concordia Transfers</title>\n" +
				"</head>\n" +
				"<body>\n" +
				"\n" +
				"<h1>Welcome to Concordia Transfers</h1>\n" +
				"<p>Select the account and amount to transfer</p>\n" +
				"\n" +
				"<form action=\"/submit\" method=\"post\">\n" +
				"        <label for=\"account\">Account:</label>\n" +
				"        <input type=\"text\" id=\"account\" name=\"account\"><br><br>\n" +
				"\n" +
				"        <label for=\"value\">Value:</label>\n" +
				"        <input type=\"text\" id=\"value\" name=\"value\"><br><br>\n" +
				"\n" +
				"        <label for=\"toAccount\">To Account:</label>\n" +
				"        <input type=\"text\" id=\"toAccount\" name=\"toAccount\"><br><br>\n" +
				"\n" +
				"        <label for=\"toValue\">To Value:</label>\n" +
				"        <input type=\"text\" id=\"toValue\" name=\"toValue\"><br><br>\n" +
				"\n" +
				"        <input type=\"submit\" value=\"Submit\">\n" +
				"    </form>\n" +
				"</body>\n" +
				"</html>\n";
		out.write(response.getBytes());
		out.flush();
	}

	private static void handlePostRequest(BufferedReader in, OutputStream out) throws IOException {
		System.out.println("Handling post request");
		StringBuilder requestBody = new StringBuilder();
		int contentLength = 0;
		String line;

		// Read headers to get content length
		while ((line = in.readLine()) != null && !line.isEmpty()) {
			if (line.startsWith("Content-Length")) {
				contentLength = Integer.parseInt(line.substring(line.indexOf(' ') + 1));
			}
		}

		// Read the request body based on content length
		for (int i = 0; i < contentLength; i++) {
			requestBody.append((char) in.read());
		}

		System.out.println(requestBody.toString());
		// Parse the request body as URL-encoded parameters
		String[] params = requestBody.toString().split("&");
		String account = null, value = null, toAccount = null, toValue = null;

		for (String param : params) {
			String[] parts = param.split("=");
			if (parts.length == 2) {
				String key = URLDecoder.decode(parts[0], "UTF-8");
				String val = URLDecoder.decode(parts[1], "UTF-8");

				switch (key) {
				case "account":
					account = val;
					break;
				case "value":
					value = val;
					break;
				case "toAccount":
					toAccount = val;
					break;
				case "toValue":
					toValue = val;
					break;
				}
			}
		}

		// Add transfer processing
		String transferStatus = "Transfer not processed";
		if (account != null && value != null && toAccount != null) {
			try {
				int amount = Integer.parseInt(value);
				boolean success = processTransfer(account, amount, toAccount);
				transferStatus = success ? 
						"Transfer successful. New balances - From Account: $" + accounts.get(account).getBalance() + 
						", To Account: $" + accounts.get(toAccount).getBalance() :
							"Transfer failed. Please check account numbers and balance.";
			} catch (NumberFormatException e) {
				transferStatus = "Invalid amount format";
			}
		}

		// Create the response
		String responseContent = "<html><body><h1>Thank you for using Concordia Transfers</h1>" +
				"<h2>Received Form Inputs:</h2>"+
				"<p>Account: " + account + "</p>" +
				"<p>Value: " + value + "</p>" +
				"<p>To Account: " + toAccount + "</p>" +
				"<p>To Value: " + toValue + "</p>" +
				"<p>Status: " + transferStatus + "</p>" +
				"</body></html>";

		// Respond with the received form inputs
		String response = "HTTP/1.1 200 OK\r\n" +
				"Content-Length: " + responseContent.length() + "\r\n" +
				"Content-Type: text/html\r\n\r\n" +
				responseContent;

		out.write(response.getBytes());
		out.flush();
	}

	private static boolean processTransfer(String fromAccount, int amount, String toAccount) {
	    Account source = accounts.get(fromAccount);
	    Account destination = accounts.get(toAccount);

	    if (source == null || destination == null || source.getBalance() < amount) {
	        return false;
	    }

	    Account firstLockAccount, secondLockAccount;

	    // Determine locking order based on account IDs
	    if (source.getId() < destination.getId()) {
	        firstLockAccount = source;
	        secondLockAccount = destination;
	    } else {
	        firstLockAccount = destination;
	        secondLockAccount = source;
	    }

	    // Acquire locks in a specific order
	    firstLockAccount.getLock().lock();
	    try {
	        secondLockAccount.getLock().lock();
	        try {
	            // Perform the transfer operation
	            source.withdraw(amount);
	            destination.deposit(amount);

	            // Save accounts after transfer
	            storeAccounts();

	        } finally {
	            // Release the second lock
	            secondLockAccount.getLock().unlock();
	        }
	    } finally {
	        // Release the first lock
	        firstLockAccount.getLock().unlock();
	    }
	    return true;
	}
	
	public static void main(String[] args) {
		//Start the server, if an exception occurs, print the stack trace
		WebServer server = new WebServer();
		try {
			server.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

