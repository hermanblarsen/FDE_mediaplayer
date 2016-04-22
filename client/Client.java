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
import javax.swing.JOptionPane;
import javax.swing.*;

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
		setupGUI();
	}
	
	private void setupGUI() {
		//Setup a JFrame and a JPanel contentsPane
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 600, 400);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));
		
		//Creates a tabbed pane, where later JPanels can be added and hence sectioned into tabs
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		
		JPanel listViewTab = new JPanel();
		tabbedPane.addTab("Video List", null, listViewTab, "Browse videos to watch");
		listViewTab.setLayout(null);
		
		selectionBox = new JComboBox<String>();
		selectionBox.setBounds(40, 80, 360, 30);
		selectionBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JComboBox<String> combobox = (JComboBox)e.getSource();
				String selectedTitle = (String)combobox.getSelectedItem();
			}
		});
		listViewTab.add(selectionBox);
	
		JPanel videoPlayerTab = new JPanel();
		tabbedPane.addTab("Video Player", null, videoPlayerTab, "Watch the videos in the inbuilt media player");
		contentPane.add(tabbedPane);
	
		JPanel settingsTab = new JPanel();
		settingsTab.setToolTipText("");
		tabbedPane.addTab("Settings", null, settingsTab, "Access settings and your SuperFlix account");
		
		
		connectToTheServer(); //TODO Only for testing
		
		
		//Sets up and adds the different tabs to the tabbed pane  //TODO Put into this system when finsihed
		/*setupListViewTab();
		setupVideoPlayerTab();
		setupSettingsTab();*/
	}

	/*private void setupListViewTab() {
		//TODO Put into this system when finsihed
	}
	
	private void setupVideoPlayerTab() {
		//TODO Put into this system when finsihed
	}
	
	private void setupSettingsTab() {
		//TODO Put into this system when finsihed
	}*/

	public void connectToTheServer() {
		 connectToTheServer(this.host, this.port);
	}	
	public void connectToTheServer(String host,int port) {
		System.out.println("Connecting to " + host + ":" + port + "...");
		try {
			this.serverSocket = new Socket(host,port);
			System.out.println("Successfully connected to " + host + ":" + port);
		} catch (UnknownHostException e) {
			System.out.println("Unknown host, unable to connect to: " + host + ".");
			System.exit(-1);
		} catch (IOException e) {
			System.out.println("Couldn't open I/O connection " + host + ":" + port + ".");
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
				System.out.println("Could not cast input object stream to videoList");
				e.printStackTrace();
			}
		} catch (ClassNotFoundException e) {
			System.out.println("Class not found for incoming object(s)");
			e.printStackTrace();
			System.exit(-1);
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Reading list complete.");
		updateClientWindow();
		emptyListErrorCatch();
	}
	
	private void emptyListErrorCatch() {
		if(this.videoList.isEmpty())
		{
			JOptionPane.showMessageDialog(contentPane, "Could not find any videos in list" , "Error: Empty List", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private void updateClientWindow() {
		//
		for ( VideoFile video : videoList){
			selectionBox.addItem(video.getTitle());
		}
		this.validate();
		//selectionBox.validate();
	}

	public void closeSockets(){
		try {
			this.serverSocket.close();
		} catch (IOException e) {
			System.out.println("Failed to close client-sockets");
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
