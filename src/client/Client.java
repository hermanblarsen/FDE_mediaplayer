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
import java.awt.TextArea;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.hamcrest.core.IsNull;

import java.util.Timer;
import java.util.TimerTask;

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

	private Client thisClient = this;
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
	private JTextField searchField;
	private JButton btnLogin ;
	private JPanel statusPanel;
	private JTextPane textPane;
	private JTable listTable;
	private VideoTableModel listVideoTableModel;
	
	private JSlider positionTimeSlider;
	private Timer update_Timer =  new Timer();
	/*used to temporarily disable the slider event when the slider value
	 * is changed by code rather than by the user. the code changes the slider
	 * value when a video is streaming in order to display the current position of the stream.*/
	private boolean sliderEventActive = true;
	private TimerTask updateSliderPositionTask = new TimerTask() {
		@Override
		public void run() {
			send("STREAM POSITION");
			float position = (float) read();
			if (position > 0f && position < 1f) {
				//prevent the change listener from firing
				sliderEventActive = false;
				positionTimeSlider.setValue((int) position * 100);
				positionTimeSlider.validate();
				sliderEventActive = true;
			}
		}
	};
	ModifiedTimerTask skipTask = new ModifiedTimerTask(0);
	class ModifiedTimerTask extends TimerTask{
		private float taskvalue;
		public ModifiedTimerTask(float value){
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
				send("SKIP");
				send(taskvalue);
				send("PLAY");
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
		connectToTheServer(); // TODO Only for manualtesting
	}

	private void requestMovieStream() {
		mediaPlayer.release();
		setUpMediaPLayer();
		writeStatus("STREAMING...", Color.GREEN);
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
				// TODO Update the list to display titles containing the search string
				
			}
		});
		listViewNorthPanel.add(searchButton, BorderLayout.EAST);
		
		JPanel listViewWestPanel = new JPanel();
		listViewTab.add(listViewWestPanel, BorderLayout.WEST);
		listViewWestPanel.setLayout(new BorderLayout(0, 0));
		contentPane.add(tabbedPane);
	
		
		
		JScrollPane listScrollPanel = new JScrollPane();
		listViewTab.add(listScrollPanel, BorderLayout.CENTER);
		
		listVideoTableModel = new VideoTableModel();
		listTable = new JTable(listVideoTableModel);
		listTable.setShowGrid(false);
		//listTable.getTableHeader().setEnabled(false);
		listScrollPanel.setViewportView(listTable);
		
		
		JButton btnComment = new JButton("Comment");
		btnComment.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String videoID= (String) listTable.getValueAt(listTable.getSelectedRow(), 0);
				for(VideoFile eachVideo : videoList) {
					if(listTable.getValueAt(listTable.getSelectedRow(), 0).equals(eachVideo.getTitle())) {
						videoID = eachVideo.getID();
					}
				}
				CommentWindow commentsWindow = new CommentWindow(videoID, thisClient , user);
				commentsWindow.show();
			}
		});
		listViewWestPanel.add(btnComment, BorderLayout.NORTH);
		
		JButton playButton = new JButton("PLAY");
		listViewWestPanel.add(playButton, BorderLayout.CENTER);
		playButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				send("STREAM");
				sendSelectedVideo();
				tabbedPane.setSelectedIndex(2);
			}
		});
		
		settingsTab = new JPanel();
		settingsTab.setToolTipText("");
		tabbedPane.addTab("Settings",null, settingsTab, "Access settings and your SuperFlix account");
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
		
		btnLogin = new JButton("Login");
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
		JButton playPauseButton = new JButton("Play");
		
		playPauseButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// check if there is anything to play
				
				if (mediaPlayer.isPlayable()) {
					// depending on if the video is already playing the button
					// function changes.
					if (mediaPlayer.isPlaying()) {
						mediaPlayer.pause();
						send("PAUSE");
					} else {
						mediaPlayer.play();
						send("PLAY");
					}
				}
			}
		});

		subPanelControlMenu.add(playPauseButton);
		
		JButton stopButton = new JButton("Stop");
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
				//created new TimerTask class so that i can pass in a value as argument.
				
				//prevents event having any effect if the slider value is changed by code and not by user
				if (sliderEventActive) {
					//get the position of the slider as percentage
					JSlider sliderTemp = (JSlider) e.getSource();
					float value = ((float) sliderTemp.getValue()) / 100f;
					//cancel the previous skiptask;
					send("PAUSE");
					skipTask.cancel();
					skipTask = new ModifiedTimerTask(value);
					update_Timer.schedule(skipTask, 500);
				}
			}
		});
		
		
		subPanelControlMenu.add(positionTimeSlider);
		JLabel durationOfMovie = new JLabel("Time playing");
		subPanelControlMenu.add(durationOfMovie);
		
		JButton fullscreenButton = new JButton("Fullscreen");
		fullscreenButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
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
		statusBar.setToolTipText("Messages about the working status of SuperFlix can be seen in this bar. Watch out for red!");
		statusPanel.add(statusBar, BorderLayout.WEST);
		
		textPane = new JTextPane();
		textPane.setEditable(false);
		statusPanel.add(textPane, BorderLayout.CENTER);

		this.setPreferredSize(new Dimension(800, 600));
		this.tabbedPane.setSelectedIndex(1);
		//disable the video list and video player tab untill the user logs in
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
			writeStatus(new String("Successfully connected to " + host + ":" + port), Color.GREEN);;
		} catch (UnknownHostException e) {
			writeStatus(new String("Unknown host, unable to connect to: " + host + "."), Color.RED);; 
			System.exit(-1);
		} catch (IOException e) {
			writeStatus(new String("Couldn't open I/O connection " + host + ":" + port + "."), Color.RED);;
			System.exit(-1);
		}

	}

	private void getVideoListFromServer() {
		try {
			// tell the server to send the videolist
			send("GETLIST");
			try {
				this.videoList = (List<VideoFile>) inputFromServer.readObject();
			} catch (ClassCastException e) {
				writeStatus(new String("Could not cast input object stream to videoList"), Color.RED); 
				e.printStackTrace();
			}
		} catch (ClassNotFoundException e) {
			writeStatus(new String("Class not found for incoming object(s)"),Color.RED); // TODO
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
	public void send(Object object) {
		try {
			outputToServer.writeObject(object);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public Object read(){
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
			this.errorOptionPane = new JOptionPane(); 
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

		final EmbeddedMediaPlayerComponent mediaPlayerComponent = new EmbeddedMediaPlayerComponent();

		mediaPlayer = mediaPlayerComponent.getMediaPlayer();
		// controlPanel = new PlayerControlsPanel(mediaPlayer);//TODO if not
		// used

		videoPlayerTab.add(mediaPlayerComponent, BorderLayout.CENTER);
		// videoPlayerTab.add(controlPanel, BorderLayout.SOUTH);//TODO if not
		// used
		
		//starting the update slider position task
		updateSliderPositionTask.cancel();
		updateSliderPositionTask = new TimerTask() {
			@Override
			public void run() {
				send("STREAM POSITION");
				float position = 0.0f;
				Object input = read();
				try {
					position = (float) input;
				}catch (Exception e){
				}
				if (position > 0 && position < 1) {
					//prevent the change listener from firing
					sliderEventActive = false;
					positionTimeSlider.setValue(Math.round(position * positionTimeSlider.getMaximum()));
					positionTimeSlider.validate();
					sliderEventActive = true;
				}
			}
		};
		update_Timer.schedule(updateSliderPositionTask, 0, 1000);

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

	public void writeStatus(String status,Color statusColor){
		textPane.setText(status);
		textPane.setBackground(statusColor);
	}

	public void login() {
		send(this.userNameField.getText().toString());
		send(new String(this.passwordField.getPassword()));
		String response =(String) read();
		if (response.equals("LOGIN SUCCEDED")) {
			this.user = (UserAccount) read();
			writeStatus("LOGIN SUCCEDED", Color.GREEN);
			userNameField.setBackground(Color.WHITE);
			passwordField.setBackground(Color.WHITE);
			
			//disable login buttons and fields.
			userNameField.setEnabled(false);
			passwordField.setEnabled(false);
			btnLogin.setEnabled(false);
			this.validate();
			
			send("STREAMPORT");
			this.streamPort = (int) read();
			
			// get the video list from the server
			getVideoListFromServer();			
			setUpMediaPLayer();
			requestMovieStream();
			updateVideoList();
			
			
			//enable and switch to the other tabs.
			tabbedPane.setEnabledAt(0, true);
			tabbedPane.setEnabledAt(1, true);
			tabbedPane.setEnabledAt(2, true);
			
			tabbedPane.setSelectedIndex(0);
			
		}else{
			userNameField.setBackground(Color.RED);
			passwordField.setBackground(Color.RED);
			writeStatus("LOGIN FAILED", Color.RED);
		}
	}
	
	public void updateVideoList() {
		//this.listVideoTableModel = new VideoTableModel(this.videoList.size());
		//this.listTable = new JTable(this.listVideoTableModel);
		
		int rowCounter = 0;
		for(VideoFile eachVideo : this.videoList) {
			int columnCounter = 0;
			this.listTable.setValueAt(eachVideo.getTitle(), rowCounter, columnCounter);
			columnCounter++;
			this.listTable.setValueAt(eachVideo.getDurationInSeconds(), rowCounter, columnCounter);
			columnCounter++;
			this.listTable.setValueAt(eachVideo.getIsFavourite(), rowCounter, columnCounter);
			rowCounter++;	
		}
		
		this.listTable.updateUI();		
		validate();
		
	}
	private void sendSelectedVideo() {
		for(VideoFile eachVideo : this.videoList) {
			if(this.listTable.getValueAt(this.listTable.getSelectedRow(), 0).equals(eachVideo.getTitle())) {
				send(eachVideo.getID());
			}
		}
	}
}
