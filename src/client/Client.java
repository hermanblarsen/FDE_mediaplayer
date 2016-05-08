package src.client;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import src.server.UserAccount;
import src.server.VideoFile;
import javax.swing.JComboBox;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.*;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.component.EmbeddedMediaPlayerComponent;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;
import uk.co.caprica.vlcj.test.basic.PlayerControlsPanel;

import com.sun.jna.*;

public class Client extends JFrame {
	
	private UserAccount user = null;

	
	protected Socket serverSocket;
	private int communicationPort = 1337;
	private int streamPort;
	private String host = "127.0.0.1";
	private ObjectInputStream inputFromServer;
	private ObjectOutputStream outputToServer;
	private String vlcLibraryDatapath = "external_archives/VLC/vlc-2.0.1";

	private PlayerControlsPanel controlPanel;
	private EmbeddedMediaPlayer mediaPlayer;

	private List<VideoFile> videoList;
	private JPanel contentPane;
	protected JComboBox<String> selectionBox;
	protected JPanel subPanelControlMenu;
	protected JPanel subPanelAudioMenu;
	protected JOptionPane errorOptionPane;
	private JPanel listViewTab;
	private JPanel settingsTab;
	private JPanel videoPlayerTab;
	private JTabbedPane tabbedPane;
	private long mediaLength;
	private JTextField userNameField;
	private JPasswordField passwordField;
	private JTable listTable;
	private JTextField searchField;

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

	public Client() {
		setupGUI();
		connectToTheServer(); // TODO Only for manualtesting
	}

	private void requestMovieStream() {
		String media = "rtp://@127.0.0.1:" + streamPort;
		mediaPlayer.playMedia(media);
	}

	/**
	 * Sets up the Client GUI. The GUI was buildt using the eclipse
	 * windowbuilder extension
	 */
	private void setupGUI() {
		// Setup a JFrame and a JPanel contentsPane
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 600, 400);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));

		tabbedPane = new JTabbedPane(JTabbedPane.TOP);

		listViewTab = new JPanel();
		tabbedPane.addTab("Video List", null, listViewTab, "Browse videos to watch");
		listViewTab.setLayout(new BorderLayout(0, 0));
		
		JPanel listViewNorthPanel = new JPanel();
		listViewTab.add(listViewNorthPanel, BorderLayout.NORTH);
		listViewNorthPanel.setLayout(new BorderLayout(0, 0));
		
		searchField = new JTextField();
		listViewNorthPanel.add(searchField, BorderLayout.CENTER);
		searchField.setColumns(10);
		
		JButton searchButton = new JButton("Search");
		searchButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("SCREAM AND SHOUT!");
				// TODO Auto-generated method stub
				
			}
		});
		listViewNorthPanel.add(searchButton, BorderLayout.EAST);
		
		JPanel listViewWestPanel = new JPanel();
		listViewTab.add(listViewWestPanel, BorderLayout.WEST);
		listViewWestPanel.setLayout(new BorderLayout(0, 0));
		
		
		listTable = new JTable(new VideoTableModel(this.videoList));
		listTable.setShowGrid(false);
		listViewTab.add(listTable, BorderLayout.CENTER);
		contentPane.add(tabbedPane);
		
		//selectionBox = new JComboBox<String>();
		//selectionBox.setBounds(40, 80, 360, 30);
		// Temporary solution to select a video from the video list
		//listViewTab.add(selectionBox);

		// TEMP PLAY BUTTON
		JButton playButton = new JButton("PLAY");
		listViewWestPanel.add(playButton, BorderLayout.NORTH);
		playButton.setBounds(415, 81, 115, 29);
		playButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				send("STREAM");
				send(videoList.get(selectionBox.getSelectedIndex()).getID());
				tabbedPane.setSelectedIndex(2);
				try {
					mediaLength = (long) inputFromServer.readObject();
				} catch (ClassNotFoundException e1) {
					e1.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
		
		
		
		

		
		settingsTab = new JPanel();
		settingsTab.setToolTipText("");
		tabbedPane.addTab("Settings",null, settingsTab, "Access settings and your SuperFlix account");
		settingsTab.setLayout(null);
		
		JLabel lblUsername = new JLabel("Username:");
		lblUsername.setBounds(0, 16, 87, 32);
		settingsTab.add(lblUsername);
		
		userNameField = new JTextField();
		userNameField.setBounds(102, 19, 146, 26);
		settingsTab.add(userNameField);
		userNameField.setColumns(10);
		
		JLabel lblPassword = new JLabel("Password:");
		lblPassword.setBounds(0, 64, 87, 20);
		settingsTab.add(lblPassword);
		
		passwordField = new JPasswordField();
		passwordField.setBounds(102, 61, 146, 26);
		settingsTab.add(passwordField);
		
		JButton btnLogin = new JButton("Login");
		btnLogin.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				login();
			}
		});
		btnLogin.setBounds(0, 103, 248, 29);
		settingsTab.add(btnLogin);
		

		/////////////////////////////////////////////////////////////////////////////////////////////
		// Tab where the selected video will be displayed
		/////////////////////////////////////////////////////////////////////////////////////////////
		videoPlayerTab = new JPanel();
		videoPlayerTab.setBackground(Color.BLACK);
		tabbedPane.addTab("Video Player", null, videoPlayerTab, null);
		videoPlayerTab.setLayout(new BorderLayout(0, 0));
		videoPlayerTab.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				subPanelControlMenu.setVisible(false);
				subPanelControlMenu.repaint();
				subPanelAudioMenu.setVisible(false);
				subPanelAudioMenu.repaint();
			}
		});

		JPanel mouseEventPanelControlMenu = new JPanel();
		mouseEventPanelControlMenu.setOpaque(false);
		mouseEventPanelControlMenu.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				subPanelControlMenu.setVisible(true);
				subPanelControlMenu.repaint();
			}
		});
		videoPlayerTab.add(mouseEventPanelControlMenu, BorderLayout.SOUTH);

		subPanelControlMenu = new JPanel();
		subPanelControlMenu.setVisible(false);
		mouseEventPanelControlMenu.add(subPanelControlMenu);
		JButton playPauseButton = new JButton("Play/Pause");
		playPauseButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// check if there is anything to play
				if (mediaPlayer.isPlayable()) {
					// depending on if the video is already playing the button
					// function changes.
					if (mediaPlayer.isPlaying()) {
						mediaPlayer.pause();
					} else {
						mediaPlayer.play();
					}
				}
			}
		});

		subPanelControlMenu.add(playPauseButton);
		JLabel timeRemainingLabel = new JLabel("Time Remaining");
		subPanelControlMenu.add(timeRemainingLabel);
		JSlider positionTimeSlider = new JSlider(0, 100);
		positionTimeSlider.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider sliderTemp = (JSlider) e.getSource();
				float value = ((float) sliderTemp.getValue()) / 100f;
				// first check if there is playable media
				if (mediaPlayer.isPlayable()) {
					// Avoid end of file freeze-up
					if (value > 0.99f) {
						value = 0.99f;
					}
					send("SKIP");
					send(value);
				}
			}
		});
		
		
		subPanelControlMenu.add(positionTimeSlider);
		JLabel timePLayingLabel = new JLabel("Time playing");
		subPanelControlMenu.add(timePLayingLabel);

		Component verticalStrut = Box.createVerticalStrut(20);
		mouseEventPanelControlMenu.add(verticalStrut);

		JPanel mouseEventPanelAudioMenu = new JPanel();
		mouseEventPanelAudioMenu.setOpaque(false);
		videoPlayerTab.add(mouseEventPanelAudioMenu, BorderLayout.EAST);
		mouseEventPanelAudioMenu.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				subPanelAudioMenu.setVisible(true);
				subPanelAudioMenu.repaint();
				// System.out.println("Mouse entered the sub_panel_Audio_Menu");
			}
		});

		Component horizontalStrut = Box.createHorizontalStrut(20);
		mouseEventPanelAudioMenu.add(horizontalStrut);

		subPanelAudioMenu = new JPanel();
		mouseEventPanelAudioMenu.add(subPanelAudioMenu);
		subPanelAudioMenu.setOpaque(false);
		subPanelAudioMenu.setVisible(false);

		JSlider audioSlider = new JSlider();
		audioSlider.setMaximum(100);
		audioSlider.setMinimum(0);
		audioSlider.setValue(50);
		audioSlider.setOrientation(SwingConstants.VERTICAL);
		audioSlider.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider sliderTemp = (JSlider) e.getSource();
				int volume_value = sliderTemp.getValue();
				// check if video is actually playable
				if (mediaPlayer.isPlayable()) {
					mediaPlayer.setVolume(volume_value);
				}
			}
		});
		subPanelAudioMenu.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		subPanelAudioMenu.add(audioSlider);
		
		JPanel statusPanel = new JPanel();
		contentPane.add(statusPanel, BorderLayout.SOUTH);
		statusPanel.setLayout(new BorderLayout(0, 0));
		
		JLabel statusBar = new JLabel("Status:");
		statusBar.setToolTipText("Messages about the working status of SuperFlix can be seen in this bar. Watch out for red!");
		statusPanel.add(statusBar, BorderLayout.WEST);
		
		JTextPane textPane = new JTextPane();
		textPane.setEditable(false);
		statusPanel.add(textPane, BorderLayout.CENTER);

		this.setPreferredSize(new Dimension(800, 600));
		this.tabbedPane.setSelectedIndex(1);
		this.pack();// makes sure everything is displayable.

	}

	/////////////////////////////////////////////////////////////////////////////////////////////
	// Methods used to set up the connection to the server
	/////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Connects to the default host:port
	 */
	public void connectToTheServer() {
		connectToTheServer(this.host, this.communicationPort);
	}

	public void connectToTheServer(String host, int port) {
		System.out.println("Connecting to " + host + ":" + port + "..."); // TODO
																			// change
																			// to
																			// status
																			// bar
		try {
			this.serverSocket = new Socket(host, port);

			// setting up the output stream
			this.outputToServer = new ObjectOutputStream(this.serverSocket.getOutputStream());
			this.inputFromServer = new ObjectInputStream(serverSocket.getInputStream());
			System.out.println("Successfully connected to " + host + ":" + port);
		} catch (UnknownHostException e) {
			System.out.println("Unknown host, unable to connect to: " + host + "."); // TODO
																						// change
																						// to
																						// status
																						// bar
			System.exit(-1);
		} catch (IOException e) {
			System.out.println("Couldn't open I/O connection " + host + ":" + port + "."); // TODO
																							// change
																							// to
																							// status
																							// bar
			System.exit(-1);
		}

		// request the streamport from the server
		System.out.println("Requesting streaming port number..."); // TODO
																	// change to
																	// status
																	// bar	
	}

	private void getVideoListFromServer() {
		try {
			// tell the server to send the videolist
			send("GETLIST");
			try {
				videoList = (List<VideoFile>) inputFromServer.readObject();
			} catch (ClassCastException e) {
				System.out.println("Could not cast input object stream to videoList"); // TODO
																						// change
																						// to
																						// status
																						// bar
				e.printStackTrace();
			}
		} catch (ClassNotFoundException e) {
			System.out.println("Class not found for incoming object(s)"); // TODO
																			// change
																			// to
																			// status
																			// bar
			e.printStackTrace();
			System.exit(-1);
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Reading list complete."); // TODO change to status
														// bar
		updateClientWindow();
		validateVideoListContentsAndFormat();
	}

	/**
	 * sends message to server
	 * 
	 * @param message
	 */
	private void send(Object object) {
		try {
			outputToServer.writeObject(object);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private Object read(){
		Object obj = null;
		try {
			obj = inputFromServer.readObject();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return obj;
	}

	// refreshes all GUI elements.
	private void updateClientWindow() {
		for (VideoFile video : videoList) {
			//selectionBox.addItem(video.getTitle()); //TODO add each video to table!
		}
		this.validate();
	}

	// closes all sockets to make sure that they can be used again if the client
	// is run again.
	public void closeSockets() {
		try {
			serverSocket.close();
		} catch (IOException e) {
			System.out.println("Failed to close client-sockets"); // TODO change
																	// to status
																	// bar
			e.printStackTrace();
		} catch (NullPointerException e) {
			// if connection fails then no server socket exists, hence null.
		}
	}

	// check that the videos received are valid
	public boolean validateVideoListContentsAndFormat() {
		boolean listIsValid = true;
		// First make sure that there are actually any videos in the list.
		if (this.videoList.isEmpty()) {
			this.errorOptionPane = new JOptionPane(); // TODO change to status
														// bar!
			errorOptionPane.showMessageDialog(contentPane, "Could not get videos from the server :(, Sorry !",
					"Error: Empty List", JOptionPane.ERROR_MESSAGE);
			listIsValid = false;
		}
		// For each of the videos make sure that the ID has the correct format
		// and that the file
		// extension is valid.
		for (VideoFile video : this.videoList) {
			// check that the video ID is the right length
			if (!(video.getID().length() == 10)) {
				listIsValid = false;
			} else if (!(video.getFilename().contains(".mp4") || video.getFilename().contains(".mpg"))) {
				listIsValid = false;
			}
		}
		return listIsValid;
	}

	/////////////////////////////////////////////////////////////////////////////////////////////
	// Methods used to set up the media player
	/////////////////////////////////////////////////////////////////////////////////////////////
	private void setUpMediaPLayer() {
		this.setUpMediaPlayer(vlcLibraryDatapath);
	}

	private void setUpMediaPlayer(String vlcLibraryPath) {
		NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), vlcLibraryPath);
		Native.loadLibrary(RuntimeUtil.getLibVlcLibraryName(), LibVlc.class);

		final EmbeddedMediaPlayerComponent mediaPlayerComponent = new EmbeddedMediaPlayerComponent();

		mediaPlayer = mediaPlayerComponent.getMediaPlayer();
		// controlPanel = new PlayerControlsPanel(mediaPlayer);//TODO if not
		// used

		videoPlayerTab.add(mediaPlayerComponent, BorderLayout.CENTER);
		// videoPlayerTab.add(controlPanel, BorderLayout.SOUTH);//TODO if not
		// used

		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				send("CLOSECONNECTION");
				mediaPlayerComponent.release();
				closeSockets();
				// TODO might need to add more closing stuff here, eg sockets
				// also possibly make it dependant on closing something else....
			}
		});
	}

	/////////////////////////////////////////////////////////////////////////////////////////////
	// MISC: getters and setters etc
	/////////////////////////////////////////////////////////////////////////////////////////////
	public List<VideoFile> getVideoList() {
		return this.videoList;
	}

	public void setVideoList(List<VideoFile> videoList) {
		this.videoList = videoList;
	}


	public void login() {
		send(this.userNameField.getText().toString());
		send(new String(this.passwordField.getPassword()));
		String response =(String) read();
		if (response.equals("LOGIN SUCCEDED")) {
			this.user = (UserAccount) read();
			send("STREAMPORT");
			this.streamPort = (int) read();
			System.out.println("Streaming port: " + this.streamPort + "received"); // TODO
																					// change
																					// to
																					// status
																					// bar
			// get the video list from the server
			getVideoListFromServer();
			setUpMediaPLayer();
			requestMovieStream();
			tabbedPane.setSelectedIndex(0);
		}
	}
}
