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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

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
import uk.co.caprica.vlcj.player.embedded.DefaultFullScreenStrategy;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;
import uk.co.caprica.vlcj.test.basic.PlayerControlsPanel;

import com.sun.jna.*;
import javax.swing.table.TableRowSorter;

import java.util.Timer;
import java.util.TimerTask;

public class Client extends JFrame {

	private UserAccount currentUser = null;
	protected boolean testMode = false;
	protected Socket serverSocket;
	private int communicationPort = 1337;
	private int clientSpecificStreamPort;
	private String host = "127.0.0.1";
	private ObjectInputStream inputFromServer;
	private ObjectOutputStream outputToServer;
	private String vlcLibraryDatapath = "external_archives/VLC/vlc-2.0.1";

	private PlayerControlsPanel controlPanel;
	private EmbeddedMediaPlayerComponent mediaPlayerComponent;
	private EmbeddedMediaPlayer mediaPlayer;

	private Client thisClient = this;
	private List<VideoFile> videoList;
	private JPanel contentPane;
	protected JComboBox<String> selectionBox;
	protected JPanel subPanelControlMenu;
	protected JPanel subPanelAudioMenu;
	//protected JOptionPane errorOptionPane;
	private JPanel listViewTab;
	private JPanel settingsTab;
	private JPanel videoPlayerTab;
	private JTabbedPane tabbedPane;
	private JTextField userNameField;
	private JPasswordField passwordField;
	private JTextField searchField;
	private JButton loginButton;
	private JPanel statusPanel;
	private JTextPane clientStatusBar;
	private JTable listTable;
	private VideoTableModel listTableModel;
	private TableRowSorter<VideoTableModel> listTableRowSorter;

	private JSlider positionTimeSlider;
	private Timer updateTimer = new Timer();
	/*
	 * used to temporarily disable the slider event when the slider value is
	 * changed by code rather than by the user. the code changes the slider
	 * value when a video is streaming in order to display the current position
	 * of the stream.
	 */
	private boolean sliderEventActive = true;
	private TimerTask updateSliderPositionTask;
	private ModifiedTimerTask skipTask;
	private JButton playPauseButton;
	private JButton stopButton;
	private DefaultFullScreenStrategy fullScreenStrategy;
	private JScrollPane listScrollPanel;
	private Boolean showListGrid;
	

	class ModifiedTimerTask extends TimerTask {
		private float taskvalue;

		public ModifiedTimerTask(float value) {
			taskvalue = value;
		}

		@Override
		public void run() {
			// first check if there is playable media
			if (mediaPlayer.isPlayable()) {
				// Avoid end of file freeze-up
				if (taskvalue > 0.99f) {
					taskvalue = 0.99f;
				}
				sendToServer("PAUSE");
				sendToServer("SKIP");
				sendToServer(taskvalue);
				sendToServer("PLAY");
			}
		}
	};

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
		if(!testMode){
			connectToTheServer(); // TODO Only for manualtesting
		}
	}

	private void requestMovieStream() {
		mediaPlayer.release();
		setUpMediaPLayer();
		writeStatus("STREAMING...", Color.GREEN);
		String media = "rtp://@127.0.0.1:" + clientSpecificStreamPort;
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
		
		
		searchField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void removeUpdate(DocumentEvent e) {
				String searchText = searchField.getText().trim();
				
				if (searchText.length()==0) {
					listTableRowSorter.setRowFilter(null);
				}else {
					listTableRowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + searchText));
				}
			}
			@Override
			public void insertUpdate(DocumentEvent e) {
				String searchText = searchField.getText().trim();
				
				if (searchText.length()==0) {
					listTableRowSorter.setRowFilter(null);
				}else {
					listTableRowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + searchText));
				}
			}
			@Override
			public void changedUpdate(DocumentEvent e) {				
			}
		});

		JPanel listViewWestPanel = new JPanel();
		listViewTab.add(listViewWestPanel, BorderLayout.WEST);
		listViewWestPanel.setLayout(new BorderLayout(0, 0));
		contentPane.add(tabbedPane);

		listScrollPanel = new JScrollPane();
		listViewTab.add(listScrollPanel, BorderLayout.CENTER);

		JButton btnComment = new JButton("Comment");
		btnComment.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String videoID = (String) listTable.getValueAt(listTable.getSelectedRow(), 0);
				for (VideoFile eachVideo : videoList) {
					if (listTable.getValueAt(listTable.getSelectedRow(), 0).equals(eachVideo.getTitle())) {
						videoID = eachVideo.getID();
					}
				}
				CommentWindow commentsWindow = new CommentWindow(videoID, thisClient, currentUser);
				commentsWindow.show();
			}
		});
		listViewWestPanel.add(btnComment, BorderLayout.NORTH);

		JButton playButton = new JButton("PLAY");
		listViewWestPanel.add(playButton, BorderLayout.CENTER);
		playButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				sendToServer("STREAM");
				playPauseButton.setText("Pause");
				updateSliderPositionTask();
				sendSelectedVideo();
				tabbedPane.setSelectedIndex(2);
			}
		});

		settingsTab = new JPanel();
		settingsTab.setToolTipText("");
		tabbedPane.addTab("Settings", null, settingsTab, "Access settings and your SuperFlix account");
		settingsTab.setLayout(null);

		JLabel userNameLabel = new JLabel("Username:");
		userNameLabel.setBounds(0, 16, 87, 32);
		settingsTab.add(userNameLabel);

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

		loginButton = new JButton("Login");
		loginButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				login();
			}
		});
		
		loginButton.setBounds(0, 103, 248, 29);
		settingsTab.add(loginButton);

		/////////////////////////////////////////////////////////////////////////////////////////////
		// Tab where the selected video will be displayed
		/////////////////////////////////////////////////////////////////////////////////////////////
		videoPlayerTab = new JPanel();
		videoPlayerTab.setEnabled(false);
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
		playPauseButton = new JButton("Pause");

		playPauseButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// check if there is anything to play

				if (mediaPlayer.isPlayable()) {
					// depending on if the video is already playing the button
					// function changes.
					if (mediaPlayer.isPlaying()) {
						mediaPlayer.pause();
						sendToServer("PAUSE");
						if(updateSliderPositionTask!=null) {
							updateSliderPositionTask.cancel();
						}
						
						playPauseButton.setText("Play");
						
					} else {
						mediaPlayer.play();
						sendToServer("PLAY");
						//if(updateSliderPositionTask==null) {
						updateTimer.purge();
						updateSliderPositionTask();
						//}
						
						playPauseButton.setText("Pause");
					}
				}else {
					sendToServer("PLAY");
					mediaPlayer.play();
					//if(updateTimer.g==null) {
					updateTimer.purge();
					updateSliderPositionTask();
					//}
					playPauseButton.setText("Pause");
				}
			}
		});
		subPanelControlMenu.add(playPauseButton);

		stopButton = new JButton("Stop");
		stopButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// check if there is anything to play

				if (mediaPlayer.isPlayable()) {
					sendToServer("STOP");
					mediaPlayer.stop();
					playPauseButton.setText("Play From Start");
					updateSliderPositionTask.cancel();
				}

			}
		});
		subPanelControlMenu.add(stopButton);

		JButton fullSkipBackButton = new JButton("<--");
		subPanelControlMenu.add(fullSkipBackButton);

		JButton skipBackButton = new JButton("<-");
		subPanelControlMenu.add(skipBackButton);

		JButton skipButton = new JButton("->");
		subPanelControlMenu.add(skipButton);

		JButton fullSkipButton = new JButton("-->");
		subPanelControlMenu.add(fullSkipButton);
		JLabel timePlayingLabel = new JLabel("Time Remaining");
		subPanelControlMenu.add(timePlayingLabel);

		positionTimeSlider = new JSlider(0, 100);
		positionTimeSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				// created new TimerTask class so that i can pass in a value as
				// argument.

				// prevents event having any effect if the slider value is
				// changed by code and not by user
				if (sliderEventActive) {
					// get the position of the slider as percentage
					JSlider sliderTemp = (JSlider) e.getSource();
					float value = ((float) sliderTemp.getValue()) / 100f;

					// Cancel the previous skiptask;
					if (skipTask != null) {
						skipTask.cancel();
					}
					skipTask = new ModifiedTimerTask(value);
					updateTimer.schedule(skipTask, 500);
				}
			}
		});

		subPanelControlMenu.add(positionTimeSlider);
		JLabel durationOfMovie = new JLabel("Time playing");
		subPanelControlMenu.add(durationOfMovie);

		fullScreenStrategy = new DefaultFullScreenStrategy(this);
		JButton fullscreenButton = new JButton("Fullscreen");
		fullscreenButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (fullScreenStrategy.isFullScreenMode()) {
					fullScreenStrategy.exitFullScreenMode();
					mediaPlayerComponent.requestFocus();
				} else {
					fullScreenStrategy.enterFullScreenMode();
				}

			}
		});

		subPanelControlMenu.add(fullscreenButton);

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

		statusPanel = new JPanel();
		statusPanel.enableInputMethods(false);
		contentPane.add(statusPanel, BorderLayout.SOUTH);
		statusPanel.setLayout(new BorderLayout(0, 0));

		JLabel statusBar = new JLabel("Status:");
		statusBar.setToolTipText(
				"Messages about the working status of SuperFlix can be seen in this bar. Watch out for red!");
		statusPanel.add(statusBar, BorderLayout.WEST);
		clientStatusBar = new JTextPane();
		clientStatusBar.setEditable(false);
		statusPanel.add(clientStatusBar, BorderLayout.CENTER);

		this.setPreferredSize(new Dimension(800, 600));
		this.tabbedPane.setSelectedIndex(1);
		// disable the video list and video player tab untill the user logs in
		this.tabbedPane.setEnabledAt(0, false);
		this.tabbedPane.setEnabledAt(1, false);
		this.tabbedPane.setEnabledAt(2, false);
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
		writeStatus(new String("Connecting to " + host + ":" + port + "..."), Color.YELLOW);
		try {
			this.serverSocket = new Socket(host, port);
			// setting up the output stream
			this.outputToServer = new ObjectOutputStream(this.serverSocket.getOutputStream());
			this.inputFromServer = new ObjectInputStream(serverSocket.getInputStream());
			writeStatus(new String("Successfully connected to " + host + ":" + port), Color.GREEN);
			;
		} catch (UnknownHostException e) {
			writeStatus(new String("Unknown host, unable to connect to: " + host + "."), Color.RED);
			;
			System.exit(-1);
		} catch (IOException e) {
			writeStatus(new String("Couldn't open I/O connection " + host + ":" + port + "."), Color.RED);
			;
			System.exit(-1);
		}

	}

	private void getVideoListFromServer() {
		try {
			// tell the server to send the videolist
			sendToServer("GETLIST");
			try {
				this.videoList = (List<VideoFile>) inputFromServer.readObject();
			} catch (ClassCastException e) {
				writeStatus(new String("Could not cast input object stream to videoList"), Color.RED);
				e.printStackTrace();
			}
		} catch (ClassNotFoundException e) {
			writeStatus(new String("Class not found for incoming object(s)"), Color.RED); // TODO
			e.printStackTrace();
			System.exit(-1);
		} catch (IOException e) {
			e.printStackTrace();
		}
		writeStatus(new String("Reading video list complete."), Color.GREEN);
		updateVideoList();
		validateVideoListContentsAndFormat();
	}

	/**
	 * sends message to server
	 * 
	 * @param message
	 */
	public void sendToServer(Object object) {
		try {
			outputToServer.writeObject(object);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Object readFromServer() {
		Object inputObjectFromStream = null;
		try {
			inputObjectFromStream = inputFromServer.readObject();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return inputObjectFromStream;
	}

	// closes all sockets to make sure that they can be used again if the client
	// is run again.
	public void closeSockets() {
		try {
			serverSocket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// check that the videos received are valid
	public boolean validateVideoListContentsAndFormat() {
		boolean listIsValid = true;
		// First make sure that there are actually any videos in the list.
		if (this.videoList.isEmpty()) {
			writeStatus("Could not get videos from the server :(, Sorry !", Color.RED);
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

		mediaPlayerComponent = new EmbeddedMediaPlayerComponent();
		mediaPlayer = mediaPlayerComponent.getMediaPlayer();

		videoPlayerTab.add(mediaPlayerComponent, BorderLayout.CENTER);

		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				if (updateSliderPositionTask != null) {
					updateSliderPositionTask.cancel();
				}
				mediaPlayerComponent.release();
				mediaPlayer.release();
				
				if (!serverSocket.isClosed()) {
					sendToServer("CLOSECONNECTION");
					closeSockets();
				}
			}
		});
	}

	private void updateSliderPositionTask() {
		// Cancels a previous task if it exists
		if (updateSliderPositionTask != null) {
			updateSliderPositionTask.cancel();
		}

		// Creates a new timerTask to update the timer position
		updateSliderPositionTask = new TimerTask() {
			@Override
			public void run() {
				sendToServer("STREAM POSITION");
				float positionInTime = 0.0f;
				Object inputFromServer = readFromServer();
				if (inputFromServer instanceof Float) {
					positionInTime = (float) inputFromServer;
				}
				if (positionInTime >= 0 && positionInTime <= 1) {
					// prevent the change listener from firing
					sliderEventActive = false;
					positionTimeSlider.setValue(Math.round(positionInTime * positionTimeSlider.getMaximum()));
					positionTimeSlider.validate();

					// Set to active again after moving slider
					sliderEventActive = true;
				}
			}
		};
		updateTimer.schedule(updateSliderPositionTask, 0, 2000);
	}

	/////////////////////////////////////////////////////////////////////////////////////////////
	// MISC: getters and setters etc
	/////////////////////////////////////////////////////////////////////////////////////////////



	public void writeStatus(String status,Color statusColor){
		clientStatusBar.setText(status);
		clientStatusBar.setBackground(statusColor);
	}
	
	/*
	 * Trys to log into the user account on the server with the data from the textfields.
	 */
	public void login(){
		String usernameInput = this.userNameField.getText().toString();
		String passwordInput = new String(this.passwordField.getPassword());
		login(usernameInput, passwordInput);
	}
	public boolean login(String usernameInput, String passwordInput) {
		sendToServer("LOGIN");
		sendToServer(usernameInput);
		sendToServer(passwordInput);
		//obtain the response from the server to see if login succeeded 
		String serverResponse = "";
		Object streamInput = readFromServer();
		if(streamInput instanceof String){
			serverResponse = (String) streamInput;
		}
		
		if (serverResponse.equals("LOGIN SUCCEDED")) {
			this.currentUser = (UserAccount) readFromServer();
			writeStatus("LOGIN SUCCEDED", Color.GREEN);
			userNameField.setBackground(Color.WHITE);
			passwordField.setBackground(Color.WHITE);

			// disable login buttons and fields.
			userNameField.setEnabled(false);
			passwordField.setEnabled(false);
			loginButton.setEnabled(false);
			this.validate();

			sendToServer("STREAMPORT");
			this.clientSpecificStreamPort = (int) readFromServer();

			// get the video list from the server
			getVideoListFromServer();
			setUpMediaPLayer();
			requestMovieStream();
			updateVideoList();

			// enable and switch to the other tabs.
			tabbedPane.setEnabledAt(0, true);
			tabbedPane.setEnabledAt(1, true);
			tabbedPane.setEnabledAt(2, true);

			tabbedPane.setSelectedIndex(0);
			return true;
		}else{
			userNameField.setBackground(Color.RED);
			passwordField.setBackground(Color.RED);
			writeStatus("LOGIN FAILED", Color.RED);
		}
		return false;
	}

	public void updateVideoList() {
		showListGrid = true;
		listTableModel = new VideoTableModel(this.videoList.size());
		listTableRowSorter = new TableRowSorter<>(listTableModel);
		
		listTable = new JTable(listTableModel);
		listTable.setRowSorter(listTableRowSorter);
		listTable.setShowGrid(showListGrid);
		listTable.getTableHeader().setReorderingAllowed(false);
		listScrollPanel.setViewportView(listTable);
		
		int rowCounter = 0;
		for (VideoFile aVideo : this.videoList) {
			int columnCounter = 0;
			this.listTable.setValueAt(aVideo.getTitle(), rowCounter, columnCounter);
			columnCounter++;
			this.listTable.setValueAt(aVideo.getDurationInSeconds(), rowCounter, columnCounter);
			columnCounter++;
			this.listTable.setValueAt(aVideo.getIsFavourite(), rowCounter, columnCounter);
			rowCounter++;
		}
		validate();
	}

	private void sendSelectedVideo() {
		for (VideoFile aVideo : this.videoList) {
			if (this.listTable.getValueAt(this.listTable.getSelectedRow(), 0).equals(aVideo.getTitle())) {
				//Send the video ID of the videofile object with a mathcing title as the first column in selected row
				sendToServer(aVideo.getID());
			}
		}
	}
}
