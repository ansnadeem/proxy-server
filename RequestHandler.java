package proxy;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;


// RequestHandler is thread that process requests of one client connection
public class RequestHandler extends Thread {

	
	Socket clientSocket;

	InputStream inFromClient;

	OutputStream outToClient;
	
	byte[] request = new byte[1024];

	BufferedReader proxyToClientBufferedReader;

	BufferedWriter proxyToClientBufferedWriter;
	
	private ProxyServer server;


	public RequestHandler(Socket clientSocket, ProxyServer proxyServer) {

		
		this.clientSocket = clientSocket;
		

		this.server = proxyServer;

		try {
			clientSocket.setSoTimeout(1000);
			inFromClient = clientSocket.getInputStream();
			outToClient = clientSocket.getOutputStream();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	
	@Override
	
	public void run() {
		this.proxyToClientBufferedReader = new BufferedReader(new
				InputStreamReader(inFromClient));
		String request = null;
		try 
		{
			request = proxyToClientBufferedReader.readLine();
			
			// (1) Check the request type, only process GET request and ignore others
			String requestParts[] = request.split(" ");
			if (requestParts.length < 2) {
				// Verifying we have at least the request type and the URL, other wise exit
				System.out.print("Invalid request");
				return; 
			}
			String requestType = requestParts[0];
			String requestUrl = requestParts[1];
			
			// Firefox sends alot of requests to this url, so we just skip it
			if (requestUrl.equals("http://detectportal.firefox.com/canonical.html"))
			{
				if (clientSocket!=null) {
					clientSocket.close();
				}
				return;
			}
			//System.out.println(request);
			if (!requestType.equalsIgnoreCase("GET")) {
				 // If the received request is not a GET request, then do not further work on it
				//System.out.println("Not a GET request, skipping ...");
				if (clientSocket!=null) {
					clientSocket.close();
					return;
				}
				return;
			}
			
			
			//  (2) If the url of GET request has been cached, respond with cached content
			String cachedFile = this.lookupCachedFile(requestUrl);
			if (cachedFile == null) {
				if (!this.proxyServertoClient(request)) {
					this.server.writeLog("Unkown error while processing request");
					return;
				}
				cachedFile = this.lookupCachedFile(requestUrl);
			}
	
			
			// (3) Otherwise, call method proxyServertoClient to process the GET request
			this.sendCachedInfoToClient(cachedFile);
		} 
		catch (Exception e) {
			return;
		}

	}
	
	private String lookupCachedFile(String url) {
		String fileName = this.server.getCache(url);
		return fileName; // filename is null incase the mapping isn't present
	}

	
	private boolean proxyServertoClient(String request) {

		Boolean success = true;
		FileOutputStream fileWriter = null;
		Socket serverSocket = null;
		InputStream inFromServer = null;
		DataOutputStream outToServer = null;
		
		// Create Buffered output stream to write to cached copy of file
		String fileName = "cached/" + generateRandomFileName() + ".dat";
		
		// to handle binary content, byte is used
		byte[] serverReply = new byte[4096];

		
		// (1) Create a socket to connect to the web server (default port 80)
		String[] requestParts = request.split(" ");
		String parsedUrl = requestParts[1].replaceAll("http://","");
		String[] requestedParams = parsedUrl.split("/",2);
		//System.out.println("Establishing TCP Connection to "+requestedParams[0]);
		String portNumber = "80";
		try {
			serverSocket = new Socket(requestedParams[0], Integer.parseInt(portNumber));
			String ip = serverSocket.getInetAddress().getHostAddress();
			// Log the request from within the thread, the writeLog is a synchronized method so it acts like a mutex
			this.server.writeLog(ip+ " "+ parsedUrl);
		} catch (Exception e) {
			return false;
		}
		
		try 
		{
			// (2) Send client's request (clientRequest) to the web server, you may want to use fluch() after writing.
			outToServer = new DataOutputStream(serverSocket.getOutputStream());
			String getRequestForServer = 
					String.format("GET /%s HTTP/1.1\r\nHost: %s\r\n\r\n",
					requestedParams[1],
					requestedParams[0]);
			outToServer.writeBytes(getRequestForServer);
			outToServer.flush();
			inFromServer = serverSocket.getInputStream();
			fileWriter = new FileOutputStream(fileName);
			
			
			// (3) Use a while loop to read all responses from web server and send back to client
			int bytesRead;
			while ((bytesRead = inFromServer.read(serverReply)) != -1)
			{
			    fileWriter.write(serverReply, 0, bytesRead);
			    
			}
            fileWriter.close();
            
            
            
            // (4) Write the web server's response to a cache file, put the request URL and cache file name to the cache Map
    		this.server.putCache(requestParts[1], fileName);
    		closeSocket(serverSocket);
		}
		catch(IOException e) {
			success = false;
		}
		
		finally 
		{
			// (5) close file, and sockets.
			closeSocket(serverSocket);
		}

		return success;
	}
	
	private void closeSocket(Socket socket) {
		try
		{
			socket.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	
	
	// Sends the cached content stored in the cache file to the client
	private void sendCachedInfoToClient(String fileName) {

		try {

			byte[] bytes = Files.readAllBytes(Paths.get(fileName));

			outToClient.write(bytes);
			outToClient.flush();

		} catch (Exception e) {
			e.printStackTrace();
		}

		try {

			if (clientSocket != null) {
				clientSocket.close();
			}

		} catch (Exception e) {
			e.printStackTrace();

		}
	}
	
	
	// Generates a random file name  
	public String generateRandomFileName() {

		String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-_";
		SecureRandom RANDOM = new SecureRandom();
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < 10; ++i) {
			sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
		}
		return sb.toString();
	}

}
