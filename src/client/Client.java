package src.client;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import src.server.VideoFile;
import javax.swing.JComboBox;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.GroupLayout.Alignment;
import javax.swing.*;
import java.awt.Color;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;

//For VLC player
import uk.co.caprica.vlcj.*;
import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.component.EmbeddedMediaPlayerComponent;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;
import uk.co.caprica.vlcj.test.basic.PlayerControlsPanel;

import com.sun.jna.*;

public class Client extends JFrame {
	
	private List<VideoFile> videoList;
	protected Socket serverSocket;
	private ObjectInputStream inputFromServer;
	private ObjectOutputStream outputToServer;
	private int port = 1337;
	private int streamPort;
	private String host = "127.0.0.1";
	
	private PlayerControlsPanel controlPanel;
	private EmbeddedMediaPlayer mediaPlayer;
	
	private JPanel contentPane;
	protected JComboBox<String> selectionBox;
	protected JPanel sub_panel_Time_Menu;
	protected JPanel sub_panel_Audio_Menu;
	protected JOptionPane errorOptionPane;
	private JPanel listViewTab;
	private JPanel settingsTab;
	private JPanel videoPlayerTab;
	private JPanel vlcPlayerTestTab;
	private JTabbedPane tabbedPane;
	private JInternalFrame internalFrame;
	private long mediaLength;

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
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				closeSockets();
			}
		});
		
		setupGUI();
		connectToTheServer(); //TODO Only for manualtesting
		setUpMediaPLayer();
		requestMovieStream();
	}
	
	private void requestMovieStream() {
		//send request
		
		//wait for confirmation
		
		//opens mediastream for chosen movie
		String media = "rtp://@127.0.0.1:"+streamPort;
		mediaPlayer.playMedia(media);
	}
	
	/**
	 * Sets up the Client GUI. The GUI was build using the eclipse windowbuilder extension
	 */
	private void setupGUI() {
		//Setup a JFrame and a JPanel contentsPane
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 600, 400);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));
		
		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		
		listViewTab = new JPanel();
		tabbedPane.addTab("Video List", null, listViewTab, "Browse videos to watch");
		listViewTab.setLayout(null);
		
		selectionBox = new JComboBox<String>();
		selectionBox.setBounds(40, 80, 360, 30);
		//Temporary solution to select a video from the video list
		listViewTab.add(selectionBox);
		
		// TEMP PLAY BUTTON
		JButton btnPlayyyy = new JButton("PLAYYYY");
		btnPlayyyy.setBounds(415, 81, 115, 29);
		btnPlayyyy.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				send("STREAM");
				String videoID = videoList.get(selectionBox.getSelectedIndex()).getID();
				send(videoID);
				try {
					mediaLength = (long) inputFromServer.readObject();
				} catch (ClassNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
		listViewTab.add(btnPlayyyy);
		contentPane.add(tabbedPane);
	
		settingsTab = new JPanel();
		settingsTab.setToolTipText("");
		tabbedPane.addTab("Settings",null, settingsTab, "Access settings and your SuperFlix account");
		
		/////////////////////////////////////////////////////////////////////////////////////////////
		//Tab where the selected video will be displayed
		/////////////////////////////////////////////////////////////////////////////////////////////
		videoPlayerTab = new JPanel();
		videoPlayerTab.setBackground(Color.BLACK);
		tabbedPane.addTab("Video Player", null, videoPlayerTab, null);
		videoPlayerTab.setLayout(new BorderLayout(0, 0));
		videoPlayerTab.addMouseListener(new MouseAdapter() {
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
		videoPlayerTab.add(Time_mouse_event_panel, BorderLayout.SOUTH);
		
		sub_panel_Time_Menu = new JPanel();
		sub_panel_Time_Menu.setVisible(false);
		Time_mouse_event_panel.add(sub_panel_Time_Menu);
		JButton btnPlaypause = new JButton("Play/Pause");
		btnPlaypause.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				//check if there is anything to play
				if (mediaPlayer.isPlayable()){
					//depending on if the video is allready playing the button function changes.
					if(mediaPlayer.isPlaying()){
						mediaPlayer.pause();
					}else{
						mediaPlayer.play();
					}
				}
			}
		});
		
		sub_panel_Time_Menu.add(btnPlaypause);
		JLabel lblTimeRemaining = new JLabel("Time Remaining");
		sub_panel_Time_Menu.add(lblTimeRemaining);
		JSlider slider = new JSlider(0,100);
		slider.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider slider = (JSlider) e.getSource();
				float value = ((float)slider.getValue())/100f;
				//first check if there is playable media
				if (mediaPlayer.isPlayable()) {
					// Avoid end of file freeze-up
			        if(value > 0.99f) {
			        	value = 0.99f;
			        }
					send("SKIP");
					send(value);
				}
			}
		});
		sub_panel_Time_Menu.add(slider);
		JLabel lblTimePlaying = new JLabel("Time playing");
		sub_panel_Time_Menu.add(lblTimePlaying);
		
		Component verticalStrut = Box.createVerticalStrut(20);
		Time_mouse_event_panel.add(verticalStrut);
		
		JPanel Audio_mouse_event_panel = new JPanel();
		Audio_mouse_event_panel.setOpaque(false);
		videoPlayerTab.add(Audio_mouse_event_panel, BorderLayout.EAST);
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
		slider_1.setMaximum(100);
		slider_1.setMinimum(0);
		slider_1.setValue(50);
		slider_1.setOrientation(SwingConstants.VERTICAL);
		slider_1.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider slider = (JSlider)e.getSource();
				int volume_value = slider.getValue();
				//check if video is actually playable
				if( mediaPlayer.isPlayable()){
					mediaPlayer.setVolume(volume_value);
				}
			}
		});
		sub_panel_Audio_Menu.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		sub_panel_Audio_Menu.add(slider_1);
		
		this.setPreferredSize(new Dimension(800, 600));
		this.pack();//makes sure everything is displayable.
			
	}

/////////////////////////////////////////////////////////////////////////////////////////////
//Methods used to set up the connection to the server
/////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Connects to the default host:port
	 */
	public void connectToTheServer() {
		 connectToTheServer(this.host, this.port);
	}	
	public void connectToTheServer(String host,int port) {
		System.out.println("Connecting to " + host + ":" + port + "...");
		try {
			this.serverSocket = new Socket(host,port);
			
			//setting up the output stream
			this.outputToServer = new ObjectOutputStream(this.serverSocket.getOutputStream());
			this.inputFromServer = new ObjectInputStream(serverSocket.getInputStream());
			System.out.println("Successfully connected to " + host + ":" + port);
		} catch (UnknownHostException e) {
			System.out.println("Unknown host, unable to connect to: " + host + ".");
			System.exit(-1);
		} catch (IOException e) {
			System.out.println("Couldn't open I/O connection " + host + ":" + port + ".");
			System.exit(-1);
		}
		
		//request the streamport from the server
		System.out.println("getting streaming port... ");
		send("STREAMPORT");
		try {
			this.streamPort = (int)inputFromServer.readObject();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("getting streaming port :"+ this.streamPort);
		//get the video list from the server
		readVideoListFromServer();
	}
	
/////////////////////////////////////////////////////////////////////////////////////////////
//Read video list from server
//NOTE:	the server should not directly send the video list as soon as the client connects,
//		it should wait untill the client requests the list, this is so that the client can 
//		refresh its list by re-requesting it from the server. The current approach only allows
//		the list to be sent once at the start.
/////////////////////////////////////////////////////////////////////////////////////////////
	private void readVideoListFromServer(){
		try {
			//tell the server to send the videolist
			send("GETLIST");
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
		validateVideoListContentsAndFormat();
	}
	
	/**
	 * sends message to server
	 * @param message
	 */
	private void send(Object obj){
		try {
			outputToServer.writeObject(obj);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//refreshes all GUI elements.
	private void updateClientWindow() {
		for ( VideoFile video : videoList){
			selectionBox.addItem(video.getTitle());
		}
		this.validate();
	}

	//closes all sockets to make sure that they can be used again if the client is run again.
	public void closeSockets(){
		try {
			serverSocket.close();
		} catch (IOException e) {
			System.out.println("Failed to close client-sockets");
			e.printStackTrace();
		} catch (NullPointerException e) {
			//if connection fails then no server socket exists, hence null.
		}
	}
	
	//check that the videos received are valid
	public boolean validateVideoListContentsAndFormat(){
		boolean listIsValid = true;
		// First make sure that there are actually any videos in the list.
		if(this.videoList.isEmpty())
		{
			this.errorOptionPane = new JOptionPane();
			// we should remove the popup message and just display red text where the list should be
			errorOptionPane.showMessageDialog(contentPane, "Could not get videos from the server :(, Sorry !" , "Error: Empty List", JOptionPane.ERROR_MESSAGE);
			listIsValid = false;
		}
		//For each of the videos make sure that the ID has the correct format and that the file
		//extension is valid.
		for (VideoFile video : this.videoList){
			//check that the video ID is the right length 
			if(!(video.getID().length() == 10)){
				listIsValid = false;
			}
			else if(!(video.getFilename().contains(".mp4") || video.getFilename().contains(".mpg"))){
				listIsValid = false;
			}
		}
		return listIsValid;
	}
	
/////////////////////////////////////////////////////////////////////////////////////////////
//Methods used to set up the media player
/////////////////////////////////////////////////////////////////////////////////////////////
	private void setUpMediaPLayer(){
		this.setUpMediaPlayer("external_archives/VLC/vlc-2.0.1");
	}
	private void setUpMediaPlayer(String vlcLibraryPath){
		NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), vlcLibraryPath);
		Native.loadLibrary(RuntimeUtil.getLibVlcLibraryName(), LibVlc.class);
		
		final EmbeddedMediaPlayerComponent mediaPlayerComponent = new EmbeddedMediaPlayerComponent();
		
		mediaPlayer = mediaPlayerComponent.getMediaPlayer();
		//controlPanel = new PlayerControlsPanel(mediaPlayer);
		
		
		videoPlayerTab.add(mediaPlayerComponent, BorderLayout.CENTER);
		//videoPlayerTab.add(controlPanel, BorderLayout.SOUTH);
		
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				mediaPlayerComponent.release();
				//TODO might need to add more closing stuff here, eg sockets also possibly make it dependant on closing something else....
			}
		});
		
	}
	
/////////////////////////////////////////////////////////////////////////////////////////////
//MISC: getters and setters etc
/////////////////////////////////////////////////////////////////////////////////////////////
	public List<VideoFile> getVideoList() {
		return this.videoList;
	}

	public void setVideoList(List<VideoFile> videoList) {
		this.videoList = videoList;
	}
}
