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
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JTabbedPane;
import javax.swing.BoxLayout;
import javax.swing.JSplitPane;
import javax.swing.JOptionPane;
import javax.swing.*;
import java.awt.Color;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;

public class Client extends JFrame {
	
	private List<VideoFile> videoList;
	private Socket serverSocket;
	private ObjectInputStream inputFromServer;
	private int port = 1238;
	private String host = "127.0.0.1";
	
	private JPanel contentPane;
	protected JComboBox<String> selectionBox;
	protected JPanel sub_panel_Time_Menu;
	protected JPanel sub_panel_Audio_Menu;

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
		contentPane.add(tabbedPane);
	
		JPanel settingsTab = new JPanel();
		settingsTab.setToolTipText("");
		tabbedPane.addTab("Settings",null, settingsTab, "Access settings and your SuperFlix account");
		
		JPanel Video_Player_Tab = new JPanel();
		Video_Player_Tab.setBackground(Color.BLACK);
		tabbedPane.addTab("Video Player", null, Video_Player_Tab, null);
		Video_Player_Tab.setLayout(new BorderLayout(0, 0));
		Video_Player_Tab.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseEntered(MouseEvent e) {
				sub_panel_Time_Menu.setVisible(false);
				sub_panel_Time_Menu.repaint();
				sub_panel_Audio_Menu.setVisible(false);
				sub_panel_Audio_Menu.repaint();
				System.out.println("Mouse entered the Video_player_Tab ");
			}
			
		});
		
		JPanel Time_mouse_event_panel = new JPanel();
		Time_mouse_event_panel.setOpaque(false);
		Time_mouse_event_panel.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseEntered(MouseEvent e) {
				sub_panel_Time_Menu.setVisible(true);
				sub_panel_Time_Menu.repaint();
				System.out.println("Mouse entered the Time menu sub panel");
			}

		});
		Video_Player_Tab.add(Time_mouse_event_panel, BorderLayout.SOUTH);
		
		sub_panel_Time_Menu = new JPanel();
		sub_panel_Time_Menu.setVisible(false);
		Time_mouse_event_panel.add(sub_panel_Time_Menu);
		JButton btnPlaypause = new JButton("Play/Pause");
		sub_panel_Time_Menu.add(btnPlaypause);
		JLabel lblTimeRemaining = new JLabel("Time Remaining");
		sub_panel_Time_Menu.add(lblTimeRemaining);
		JSlider slider = new JSlider();
		sub_panel_Time_Menu.add(slider);
		JLabel lblTimePlaying = new JLabel("Time playing");
		sub_panel_Time_Menu.add(lblTimePlaying);
		
		Component verticalStrut = Box.createVerticalStrut(20);
		Time_mouse_event_panel.add(verticalStrut);
		
		JPanel Audio_mouse_event_panel = new JPanel();
		Audio_mouse_event_panel.setOpaque(false);
		Video_Player_Tab.add(Audio_mouse_event_panel, BorderLayout.EAST);
		Audio_mouse_event_panel.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseEntered(MouseEvent e) {
				sub_panel_Audio_Menu.setVisible(true);
				sub_panel_Audio_Menu.repaint();
				System.out.println("Mouse entered the sub_panel_Audio_Menu");
			}

		});
		
		Component horizontalStrut = Box.createHorizontalStrut(20);
		Audio_mouse_event_panel.add(horizontalStrut);
		
		sub_panel_Audio_Menu = new JPanel();
		Audio_mouse_event_panel.add(sub_panel_Audio_Menu);
		sub_panel_Audio_Menu.setOpaque(false);
		sub_panel_Audio_Menu.setVisible(false);
		
		
		JSlider slider_1 = new JSlider();
		slider_1.setOrientation(SwingConstants.VERTICAL);
		sub_panel_Audio_Menu.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		sub_panel_Audio_Menu.add(slider_1);
		
		
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
		catchEmptyListError();
	}
	
	public void catchEmptyListError() {
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
