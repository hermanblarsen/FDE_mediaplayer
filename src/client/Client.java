package client;

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
import com.sun.jna.*;

import server.UserAccount;
import server.VideoFile;

import javax.swing.table.TableRowSorter;

import java.util.Timer;
import java.util.TimerTask;

public class Client extends JFrame {

	private UserAccount currentUser = null;
	protected boolean testMode = false;
	protected Socket serverSocket;
	private int communicationPort = 1337;
	private int clientSpecificStreamPort;
	private String hostAddress = "127.0.0.1";
	private ObjectInputStream inputFromServer;
	private ObjectOutputStream outputToServer;
	private String vlcLibraryDatapath = "external_archives/VLC/vlc-2.0.1";

	private EmbeddedMediaPlayerComponent mediaPlayerComponent;
	private EmbeddedMediaPlayer mediaPlayer;

	private Client thisClient = this;
	private List<VideoFile> videoList;
	private JPanel contentPane;
	protected JComboBox<String> selectionBox;
	protected JPanel subPanelControlMenu;
	protected JPanel subPanelAudioMenu;
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
	
	private boolean sliderEventActive = true;
	private TimerTask updateSliderPositionTask;
	private TimerTask autoHideControlPanelsTask;
	private ModifiedTimerTask skipTask;
	private JButton playPauseButton;
	private JButton stopButton;
	private JScrollPane listScrollPanel;
	private Boolean showListGrid;
	

	
	/*
	 * used to temporarily disable the slider event when the slider value is
	 * changed by code rather than by the user. the code changes the slider
	 * value when a video is streaming in order to display the current position
	 * of the stream.
	 */
	class ModifiedTimerTask extends TimerTask {
		private float sliderPosition;

		public ModifiedTimerTask(float changedSliderPosition) {
			sliderPosition = changedSliderPosition;
		}
		@Override
		public void run() {
			// first check if there is playable media
			if (mediaPlayer.isPlayable()) {
				// Avoid end of file freeze-up
				if (sliderPosition > 0.99f) {
					sliderPosition = 0.99f;
				}
				sendToServer("PAUSE");
				sendToServer("SKIPTOPOSITION");
				sendToServer(sliderPosition);
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
					Client newClient = new Client();
					newClient.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public Client() {
		setupGUI();
		if(!testMode){
			connectToTheServer();
		}
	}

	private void requestMovieStream() {
		if( mediaPlayer != null) {
			mediaPlayer.release();
		}
		setUpMediaPLayer();
		String mediaStreamAddress = "rtp://@127.0.0.1:" + clientSpecificStreamPort;
		mediaPlayer.playMedia(mediaStreamAddress);
	}

	/**
	 * Sets up the Client GUI. The GUI was buildt using the eclipse
	 * windowbuilder extension
	 */
	private void setupGUI() {
		// Setup a JFrame and a JPanel contentsPane
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setTitle("SuperFlix");
		this.setBounds(100, 100, 600, 400);
		this.contentPane = new JPanel();
		this.contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		this.setContentPane(contentPane);
		this.contentPane.setLayout(new BorderLayout(0, 0));
		
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
				boolean noVideoSelected = false;
				String videoTitle = "";
				try {
					videoTitle = (String) listTable.getValueAt(listTable.getSelectedRow(), 0);
				} catch (Exception e) {
					writeStatus("Select a video before commenting", Color.YELLOW);
					noVideoSelected = true;
				}				
				if (!noVideoSelected) {
					String videoID="";
					for (VideoFile eachVideo : videoList) {
						if (videoTitle.equals(eachVideo.getTitle())) {
							videoID = eachVideo.getID();
						}
					}
					writeStatus(new String(currentUser.getUserNameID() + " connected to server " + hostAddress + ":" + communicationPort), Color.GREEN);
					CommentWindow commentsWindow = new CommentWindow(videoTitle, videoID, thisClient, currentUser);
					commentsWindow.show();
				}
			}
		});
		listViewWestPanel.add(btnComment, BorderLayout.NORTH);

		JButton streamSelectedVideoButton = new JButton("STREAM");
		listViewWestPanel.add(streamSelectedVideoButton, BorderLayout.CENTER);
		streamSelectedVideoButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String selectedVideoTitle = "";
				boolean exceptionEntered = false;
				try {
					selectedVideoTitle = (String) listTable.getValueAt(listTable.getSelectedRow(), 0);
				} catch (Exception ee) {
					exceptionEntered = true;
					writeStatus("Select a video before streaming", Color.YELLOW);
				}
				if (!exceptionEntered) {
					sendToServer("STREAM");
					sendSelectedVideo(selectedVideoTitle);
					updateTimer.purge();
					updateSliderPositionTask();
					playPauseButton.setText("Pause");
					mediaPlayer.play();
					tabbedPane.setEnabledAt(2, true);
					tabbedPane.setSelectedIndex(2);
					//writeStatus("STREAMING...", Color.GREEN); //TODO remove?
					writeStatus(new String(currentUser.getUserNameID() + " connected to server " + hostAddress + ":" + communicationPort), Color.GREEN);
				} 
			}
		});

		settingsTab = new JPanel();
		settingsTab.setToolTipText("");
		tabbedPane.addTab("Settings", null, settingsTab, "Access settings and your SuperFlix account");
		settingsTab.setLayout(null);

		JLabel userNameLabel = new JLabel("Username: ");
		userNameLabel.setBounds(0, 16, 87, 32);
		settingsTab.add(userNameLabel);

		userNameField = new JTextField();
		userNameField.setBounds(102, 19, 146, 26);
		settingsTab.add(userNameField);
		userNameField.setColumns(10);

		JLabel lblPassword = new JLabel("Password: ");
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
					updateTimer.purge();
					updateSliderPositionTask();
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

		/*JButton fullSkipBackButton = new JButton("<--");
		subPanelControlMenu.add(fullSkipBackButton);

		JButton skipBackButton = new JButton("<-");
		subPanelControlMenu.add(skipBackButton);

		JButton skipButton = new JButton("->");
		subPanelControlMenu.add(skipButton);

		JButton fullSkipButton = new JButton("-->");
		subPanelControlMenu.add(fullSkipButton);*/
		
		
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
					updateTimer.schedule(skipTask, 250);
				}
			}
		});

		subPanelControlMenu.add(positionTimeSlider);
		JLabel durationOfMovie = new JLabel("Time playing");
		subPanelControlMenu.add(durationOfMovie);

		JToggleButton fullscreenButton = new JToggleButton("Fullscreen");
		fullscreenButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (thisClient.getExtendedState()==NORMAL) {
					thisClient.setExtendedState(MAXIMIZED_BOTH);
				} else {
					thisClient.setExtendedState(NORMAL);
				}
			}
		});

		subPanelControlMenu.add(fullscreenButton);

		Component verticalPlaceholder = Box.createVerticalStrut(20);
		mouseEventPanelControlMenu.add(verticalPlaceholder);

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

		Component horizontalPlaceholder = Box.createHorizontalStrut(20);
		mouseEventPanelAudioMenu.add(horizontalPlaceholder);

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
		clientStatusBar.setToolTipText("Messages about the working status of SuperFlix can be seen in this bar. Watch out for red!");
		clientStatusBar.setEditable(false);
		statusPanel.add(clientStatusBar, BorderLayout.CENTER);

		this.setPreferredSize(new Dimension(800, 600));
		this.tabbedPane.setSelectedIndex(1);
		
		// disable the video list and video player tab until the user logs in
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
		connectToTheServer(this.hostAddress, this.communicationPort);
	}
	public void connectToTheServer(String specifiedHostName, int specifiedPortNumber) {
		writeStatus(new String("Connecting to " + specifiedHostName + ":" + specifiedPortNumber + "..."), Color.YELLOW);
		try {
			this.serverSocket = new Socket(specifiedHostName, specifiedPortNumber);
			// setting up the output stream
			this.outputToServer = new ObjectOutputStream(this.serverSocket.getOutputStream());
			this.inputFromServer = new ObjectInputStream(serverSocket.getInputStream());
			writeStatus(new String("Successfully connected to " + specifiedHostName + ":" + specifiedPortNumber), Color.GREEN);
			;
		} catch (UnknownHostException e) {
			writeStatus(new String("Unknown host, unable to connect to: " + specifiedHostName + "."), Color.RED);
			;
			System.exit(-1);
		} catch (IOException e) {
			writeStatus(new String("Couldn't open I/O connection " + specifiedHostName + ":" + specifiedPortNumber + "."), Color.RED);
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
			writeStatus(new String("Class not found for incoming object(s)"), Color.RED);
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
			e.printStackTrace();
		} catch (IOException e) {
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
			updateTimer.purge();
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
		updateTimer.schedule(updateSliderPositionTask, 2000, 2000);
	}
	
	private void setupAutoHideConsoleTask() {
		// Cancels a previous task if it exists
		if (autoHideControlPanelsTask != null) {
			autoHideControlPanelsTask.cancel();
			updateTimer.purge();
		}

		// Creates a new timerTask to update the timer position
		autoHideControlPanelsTask = new TimerTask() {
			@Override
			public void run() {
				//TODO shitloads here
			}
		};
		updateTimer.schedule(autoHideControlPanelsTask, 0, 2000);
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
		sendToServer("REQEUSTLOGIN");
		sendToServer(usernameInput);
		sendToServer(passwordInput);
		
		//obtain the response from the server to see if login succeeded 
		String loginRequestAnswerString = "";
		Object loginRequestAnswerObject = readFromServer();
		if(loginRequestAnswerObject instanceof String){
			loginRequestAnswerString = (String) loginRequestAnswerObject;
		}
		
		if (loginRequestAnswerString.equals("LOGINSUCCEDED")) {
			writeStatus("LOGIN SUCCEDED", Color.GREEN);
			this.currentUser = (UserAccount) readFromServer();
			userNameField.setBackground(Color.WHITE);
			passwordField.setBackground(Color.WHITE);

			// disable login buttons and fields.
			userNameField.setEnabled(false);
			passwordField.setEnabled(false);
			loginButton.setEnabled(false);
			this.validate();

			sendToServer("REQUESTSTREAMPORT");
			this.clientSpecificStreamPort = (int) readFromServer();

			// get the video list from the server and set up mediaStreaming at given received port
			getVideoListFromServer();
			requestMovieStream();
			updateVideoList();

			// enable other tabs and switch to the list view
			tabbedPane.setEnabledAt(0, true);
			tabbedPane.setEnabledAt(1, true);
			//tabbedPane.setEnabledAt(2, true);

			tabbedPane.setSelectedIndex(0);
			writeStatus(new String(this.currentUser.getUserNameID() + " connected to server " + this.hostAddress + ":" + this.communicationPort), Color.GREEN);
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

	private void sendSelectedVideo(String selectedVideoTitle) {
		for (VideoFile aVideo : this.videoList) {
			if (selectedVideoTitle.equals(aVideo.getTitle())) {
				//Send the video ID of the videofile object with a mathcing title as the first column in selected row
				sendToServer(aVideo.getID());
			}
		}
	}
}
