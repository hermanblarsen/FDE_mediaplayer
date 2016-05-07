package src.server;

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.component.EmbeddedMediaPlayerComponent;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.headless.HeadlessMediaPlayer;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;
import uk.co.caprica.vlcj.test.basic.PlayerControlsPanel;

import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.platform.win32.Netapi32Util.User;

public class Server {
	private List<VideoFile> videoList;
	private ServerSocket serverSocket;
	private String serverAddress = "127.0.0.1";
	private int listPort =  1337;
	private int initialStreamPort =  5555;
	private List<Integer> streamPortList = new ArrayList<Integer>();
	private String options = formatRtpStream(serverAddress, initialStreamPort);
	private Socket clientSocket;
	private List<ClientConnection> clientList = new ArrayList<ClientConnection>(); 

	public static void main(String[] args){
		new Server(); //Main for testing
	}
	
	private String formatRtpStream(String serverAddress, int streamPort) {
		StringBuilder sb = new StringBuilder(60);
		sb.append(":sout=#rtp{dst=");
		sb.append(serverAddress);
		sb.append(",port=");
		sb.append(streamPort);
		sb.append(",mux=ts}");
		return sb.toString();
		}

	public Server(){
		//creating the server socket
		try {
			serverSocket = new ServerSocket(listPort);
		} catch (IOException e) {
			System.out.println("ERROR ! Unable to create server socket");
			e.printStackTrace();
		}
		//Awaiting client connections
		while(true){
			try {
				//wait for client to connect to socket
				System.out.println("Successfully opened socket on port: "+ listPort + ", awaiting connection...");
				this.clientSocket = this.serverSocket.accept();
				System.out.println("Successfully connected to client.");
				streamPortList.add(initialStreamPort + streamPortList.size());
				
				int streamport = streamPortList.get(streamPortList.size()-1);
				String options = formatRtpStream(this.serverAddress,streamport);
				
				//creating and starting client thread
				ClientConnection tempclient = new ClientConnection(this.clientSocket, streamport,options);
				this.clientList.add(tempclient);
				Thread clientThread = new Thread(tempclient);
				clientThread.start();
				
				System.out.println("Successfully started client thread");
			} catch (IOException e) {
				System.out.println("ERROR! connection with client failed");
				e.printStackTrace();
				//prevents the start of a new thread if no connection is made.
			}
		}
	}
	

	public void closeSockets(Socket socket){
		try {
			socket.close();
		} catch (IOException e) {
			System.out.println("Failed to close server-sockets");
			e.printStackTrace();
		}
	}
}
