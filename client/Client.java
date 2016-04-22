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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JTabbedPane;
import javax.swing.BoxLayout;
import javax.swing.JSplitPane;

public class Client extends JFrame {
	
	private List<VideoFile> videoList;
	private Socket serverSocket;
	private ObjectInputStream inputFromServer;
	private int port = 1238;
	private String host = "127.0.0.1";
	
	private JPanel contentPane;
	protected JComboBox<String> selectionBox;

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
		setBounds(100, 100, 653, 485);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));
		
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		tabbedPane.setToolTipText("Choose which tab to display");
		
		JPanel listViewPanel = new JPanel();
		tabbedPane.addTab("New tab", null, listViewPanel, null);
		listViewPanel.setLayout(null);
		
		selectionBox = new JComboBox<String>();
		selectionBox.setBounds(46, 78, 362, 24);
		selectionBox.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				JComboBox<String> combobox = (JComboBox)e.getSource();
				String selectedTitle = (String)combobox.getSelectedItem();
				System.out.println("selected Title: " + selectedTitle);
				
			}
		});
		listViewPanel.add(selectionBox);
		selectionBox.setVisible(true);
		
		JPanel videoPlayerPanel = new JPanel();
		tabbedPane.addTab("Video Player", null, videoPlayerPanel, null);
		contentPane.add(tabbedPane);
		
		JPanel settingsPanel = new JPanel();
		settingsPanel.setToolTipText("");
		tabbedPane.addTab("Settings", null, settingsPanel, null);
	}
	
	public void connectToTheServer() {
		 connectToTheServer(this.host, this.port);
	}
	
	public void connectToTheServer(String host,int port) {
		System.out.println("Trying to connect to " + host + ":" + port + ".");
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
		updateClientWindow();
	}
	
	private void updateClientWindow() {
		//
		for ( VideoFile video : videoList){
			selectionBox.addItem(video.getTitle());
		}
		this.validate();
		selectionBox.validate();
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
