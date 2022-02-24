package proxy;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;



public class ProxyServer {

	//cache is a Map: the key is the URL and the value is the file name of the file that stores the cached content
	Map<String, String> cache;
	
	ServerSocket proxySocket;

	String logFileName = "log.txt";

	public static void main(String[] args) {
		try
		{
			//new ProxyServer().startServer(3543); //DEBUG ONLY
			new ProxyServer().startServer(Integer.parseInt(args[0]));
		}
		catch(Exception e)
		{
			System.out.print("Unable to run proxy, see stack trace below");
			e.printStackTrace();
			return;
		}
	}

	void startServer(int proxyPort) throws IOException {

		cache = new ConcurrentHashMap<>();

		// create the directory to store cached files. 
		File cacheDir = new File("cached");
		if (!cacheDir.exists() || (cacheDir.exists() && !cacheDir.isDirectory())) {
			cacheDir.mkdirs();
		}
		
		
		// 1- Create the ServerSocket Object
		ServerSocket server = null;
		try {
			server = new ServerSocket(proxyPort);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		// 2-Create a thread (RequestHandler) for each new client connection 
		try {
			while(true) {
				Socket connectionSocket = server.accept();
				RequestHandler task = new RequestHandler(connectionSocket, this);
				Thread thread = new Thread(task) ;
				thread.start();
			}
		} 
		catch (IOException e) {
			if (server!=null)
			{
				server.close();
			}
		}
		
	}



	public String getCache(String hashcode) {
		return cache.get(hashcode);
	}

	public void putCache(String hashcode, String fileName) {
		cache.put(hashcode, fileName);
	}

	public synchronized void writeLog(String info) {
		String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
		try {
			FileWriter fileWriter = new FileWriter("log.txt", true);
		    BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
		    String logLine = String.format("%s: %s\n", timeStamp, info);
			bufferedWriter.write(logLine);
			System.out.print(logLine);
			bufferedWriter.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
