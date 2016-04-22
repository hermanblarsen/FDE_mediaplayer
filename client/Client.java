package client;
import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import server.VideoFile;
import javax.swing.JComboBox;
import java.awt.FlowLayout;

public class Client extends JFrame {
	
	private List<VideoFile> videoList;
	private Socket serverSocket;
	private ObjectInputStream inputFromServer;
	private int port = 1238;
	private String host = "127.0.0.1";
	
	private JPanel contentPane;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Client frame = new Client();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public Client() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 917, 664);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));
		
		JComboBox comboBox = new JComboBox();
		contentPane.add(comboBox, BorderLayout.EAST);
	}
	
	public void connectToTheServer() {
		 connectToTheServer(this.host, this.port);
	}
	
	public void connectToTheServer(String host,int port) {
		System.out.println("Trying to connect to " + host + ":" + port);
		try {
			this.serverSocket = new Socket(host,port);
			System.out.println("Successfully connected to " + host + ":" + port);
		} catch (UnknownHostException e) {
			System.out.println("Unknown host , unable to connect to " + host);
			System.exit(-1);
		} catch (IOException e) {
			System.out.println("COuldnt open IO connection "+ host + ":" + port);
			System.exit(-1);
		}
		readFromServer();
		closeSockets();
	}
	
	private void readFromServer(){
		try {
			inputFromServer = new ObjectInputStream(serverSocket.getInputStream());
			try {
				videoList = (List<VideoFile>)inputFromServer.readObject();
			} catch (ClassCastException e) {
				System.out.println("ClassCastException thingy");
				e.printStackTrace();
			}
			System.out.println("Reading of list complete " + host);
		} catch (ClassNotFoundException e) {
			System.out.println("Class not found for incoming object");
			e.printStackTrace();
			System.exit(-1);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void closeSockets(){
		try {
			this.serverSocket.close();
			System.out.println("Successfully closed client side sockets");
		} catch (IOException e) {
			System.out.println("Failed to close client side sockets");
			e.printStackTrace();
		}
	}
	
	public List<VideoFile> getVideoList() {
		return this.videoList;
	}

	public void setVideoList(List<VideoFile> videoList) {
		this.videoList = videoList;
	}

}
