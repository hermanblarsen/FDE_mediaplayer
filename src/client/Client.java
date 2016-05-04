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

import src.server.VideoFile;
import javax.swing.JComboBox;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.GroupLayout.Alignment;
import javax.swing.*;
import java.awt.Color;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
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
	private BufferedWriter outputToServer;
	private int port = 1337;
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
	private JTabbedPane tabbedPane;
	private JInternalFrame internalFrame;

	private Timer automaticPanelHiding;

	// Constants:
	private static int VIDEO_UI_DELAY = 2000;// (in ms)

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
		setupAutomaticHideTimer();

		connectToTheServer(); // TODO Only for manualtesting
		setUpMediaPLayer();
		requestMovieStream();
	}

	private void requestMovieStream() {
		// send request

		// wait for confirmation

		// opens mediastream for chosen movie
		String media = "rtp://@127.0.0.1:5555";
		mediaPlayer.playMedia(media);
	}

	private void setupAutomaticHideTimer() {
		this.automaticPanelHiding = new Timer(VIDEO_UI_DELAY, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				sub_panel_Time_Menu.setVisible(false);
				sub_panel_Audio_Menu.repaint();
				sub_panel_Audio_Menu.setVisible(false);
				sub_panel_Time_Menu.repaint();
			}
		});
		this.automaticPanelHiding.setRepeats(true);
		this.automaticPanelHiding.setInitialDelay(VIDEO_UI_DELAY * 2);
	}

	/**
	 * Sets up the Client GUI. The GUI was build using the eclipse windowbuilder
	 * extension
	 */
	private void setupGUI() {
		// Setup a JFrame and a JPanel contentsPane
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 600, 400);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		this.setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));

		tabbedPane = new JTabbedPane(JTabbedPane.TOP);

		listViewTab = new JPanel();
		tabbedPane.addTab("Video List", null, listViewTab, "Browse videos to watch");
		listViewTab.setLayout(null);

		selectionBox = new JComboBox<String>();
		selectionBox.setBounds(40, 80, 360, 30);
		// Temporary solution to select a video from the video list
		listViewTab.add(selectionBox);

		JButton btnPlayyyy = new JButton("PLAYYYY");
		btnPlayyyy.setBounds(415, 81, 115, 29);
		btnPlayyyy.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				send("STREAM");
				String videoID = videoList.get(selectionBox.getSelectedIndex()).getID();
				send(videoID);
			}
		});
		listViewTab.add(btnPlayyyy);
		contentPane.add(tabbedPane);

		settingsTab = new JPanel();
		settingsTab.setToolTipText("");
		tabbedPane.addTab("Settings", null, settingsTab, "Access settings and your SuperFlix account");

		/////////////////////////////////////////////////////////////////////////////////////////////
		// Tab where the selected video will be displayed
		/////////////////////////////////////////////////////////////////////////////////////////////
		videoPlayerTab = new JPanel();
		videoPlayerTab.setBackground(Color.BLACK);
		tabbedPane.addTab("Video Player", null, videoPlayerTab, null);
		videoPlayerTab.setLayout(new BorderLayout(0, 0));

		// TODO remove
		/*
		 * videoPlayerTab.addMouseListener(new MouseAdapter() {
		 * 
		 * @Override public void mouseEntered(MouseEvent e) {
		 * sub_panel_Time_Menu.setVisible(false); sub_panel_Time_Menu.repaint();
		 * sub_panel_Audio_Menu.setVisible(false);
		 * sub_panel_Audio_Menu.repaint(); } });
		 */

		/*
		 * JPanel videoPlayerMouseEventPanel = new MyGlas();
		 * videoPlayerMouseEventPanel.setOpaque(false);
		 * videoPlayerTab.add(videoPlayerMouseEventPanel, BorderLayout.CENTER);
		 */
		this.getGlassPane().setVisible(true);
		this.getGlassPane().addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(MouseEvent e) {
				redispatchMouseEvent(e);
			}

			@Override
			public void mousePressed(MouseEvent e) {
				redispatchMouseEvent(e);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				redispatchMouseEvent(e);
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				redispatchMouseEvent(e);
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				redispatchMouseEvent(e);
			}
		});
		this.getGlassPane().addMouseMotionListener(new MouseMotionListener() {

			@Override
			public void mouseMoved(MouseEvent e) {
				sub_panel_Time_Menu.setVisible(true);
				sub_panel_Audio_Menu.setVisible(true);
				automaticPanelHiding.restart();
				automaticPanelHiding.start();
				redispatchMouseEvent(e);
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				redispatchMouseEvent(e);
			}
		});
		this.getGlassPane().addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				// From Oracle documentation:
				// https://docs.oracle.com/javase/tutorial/uiswing/events/mousewheellistener.html
				String message;
				int notches = e.getWheelRotation();
				if (notches < 0) {
					message = "Mouse wheel moved UP " + -notches + " notch(es)" + System.lineSeparator();
				} else {
					message = "Mouse wheel moved DOWN " + notches + " notch(es)" + System.lineSeparator();
				}
				if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
					message += "    Scroll type: WHEEL_UNIT_SCROLL" + System.lineSeparator();
					message += "    Scroll amount: " + e.getScrollAmount() + " unit increments per notch"
							+ System.lineSeparator();
					message += "    Units to scroll: " + e.getUnitsToScroll() + " unit increments"
							+ System.lineSeparator();
					/*
					 * message += "    Vertical unit increment: " +
					 * scrollPane.getVerticalScrollBar().getUnitIncrement(1) +
					 * " pixels" + newline;
					 */
				} else { // scroll type == MouseWheelEvent.WHEEL_BLOCK_SCROLL
					message += "    Scroll type: WHEEL_BLOCK_SCROLL" + System.lineSeparator();
					/*
					 * message += "    Vertical block increment: " +
					 * scrollPane.getVerticalScrollBar().getBlockIncrement(1) +
					 * " pixels" + newline;
					 */
				}
				System.out.print(message);
			}
		});

		// TODO remove
		/*
		 * JPanel Time_mouse_event_panel = new JPanel();
		 * Time_mouse_event_panel.setOpaque(false);
		 * Time_mouse_event_panel.addMouseListener(new MouseAdapter() {
		 * 
		 * @Override public void mouseEntered(MouseEvent e) {
		 * sub_panel_Time_Menu.setVisible(true); sub_panel_Time_Menu.repaint();
		 * }
		 * 
		 * }); videoPlayerTab.add(Time_mouse_event_panel, BorderLayout.SOUTH);
		 */
		sub_panel_Time_Menu = new JPanel();
		sub_panel_Time_Menu.setVisible(false); // TODO remove

		// Time_mouse_event_panel.add(sub_panel_Time_Menu);//TODO remove
		JButton btnPlaypause = new JButton("Play/Pause");
		sub_panel_Time_Menu.add(btnPlaypause);
		JLabel lblTimeRemaining = new JLabel("Time Remaining");
		sub_panel_Time_Menu.add(lblTimeRemaining);
		JSlider slider = new JSlider();
		sub_panel_Time_Menu.add(slider);
		JLabel lblTimePlaying = new JLabel("Time playing");
		sub_panel_Time_Menu.add(lblTimePlaying);

		/*
		 * Component verticalStrut = Box.createVerticalStrut(20);
		 * Time_mouse_event_panel.add(verticalStrut);
		 */

		// TODO remove
		/*
		 * JPanel Audio_mouse_event_panel = new JPanel();
		 * Audio_mouse_event_panel.setOpaque(false);
		 * videoPlayerTab.add(Audio_mouse_event_panel, BorderLayout.EAST);
		 * Audio_mouse_event_panel.addMouseListener(new MouseAdapter() {
		 * 
		 * @Override public void mouseEntered(MouseEvent e) {
		 * sub_panel_Audio_Menu.setVisible(true);
		 * sub_panel_Audio_Menu.repaint(); } });
		 */

		/*
		 * Component horizontalStrut = Box.createHorizontalStrut(20);
		 * Audio_mouse_event_panel.add(horizontalStrut);
		 */

		sub_panel_Audio_Menu = new JPanel();
		// Audio_mouse_event_panel.add(sub_panel_Audio_Menu);
		sub_panel_Audio_Menu.setOpaque(false);
		sub_panel_Audio_Menu.setVisible(false); // TODO remove

		JSlider slider_1 = new JSlider();
		slider_1.setOrientation(SwingConstants.VERTICAL);
		sub_panel_Audio_Menu.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		sub_panel_Audio_Menu.add(slider_1);

		this.setPreferredSize(new Dimension(800, 600));
		this.pack();// makes sure everything is displayable.

	}

	private void redispatchMouseEvent(MouseEvent e) {
		// From Oracle documentation:
		// https://docs.oracle.com/javase/tutorial/uiswing/events/mousewheellistener.html
		boolean inMenuBar = false;
		boolean inTimeBar = false;
		boolean inTabMenu = false;
		boolean inAnything = true;
		Point glassPanePoint = e.getPoint();
		Container container = contentPane;
		Point containerPoint = SwingUtilities.convertPoint(this.getGlassPane(), glassPanePoint, contentPane);

		if (containerPoint.y < 0) {
			// we're not in the content pane. Could have special code to handle
			// mouse events over
			// the menu bar or non-system window decorations, such as the ones
			// provided by the Java look and feel.
		} else {
			// The mouse event is probably over the content pane. Find out
			// exactly which component it's over.
			Component component = SwingUtilities.getDeepestComponentAt(container, containerPoint.x, containerPoint.y);

			if ((component != null)) {
				// Forward events over the check box.
				Point componentPoint = SwingUtilities.convertPoint(this.getGlassPane(), glassPanePoint, component);

				component.dispatchEvent(new MouseEvent(component, e.getID(), e.getWhen(), e.getModifiers(),
						componentPoint.x, componentPoint.y, e.getClickCount(), e.isPopupTrigger()));
			}
			if ((component != null) && ((component.equals(this)|| component.equals(mediaPlayer)|| component.equals(controlPanel)|| component.equals(videoPlayerTab)))) {
				Point componentPoint = SwingUtilities.convertPoint(this.getGlassPane(), glassPanePoint, component);

				component.dispatchEvent(new MouseEvent(component, e.getID(), e.getWhen(), e.getModifiers(),
						componentPoint.x, componentPoint.y, e.getClickCount(), e.isPopupTrigger()));
			}
		
		}
	}

	/*
	 * private void redispatchMouseEventTeest(MouseEvent e) { // From Oracle
	 * documentation: //
	 * https://docs.oracle.com/javase/tutorial/uiswing/events/mousewheellistener
	 * .html boolean inMenuBar = false; boolean inTimeBar = false; boolean
	 * inTabMenu = false;
	 * 
	 * Point glassPanePoint = e.getPoint(); Container container =
	 * videoPlayerTab; Point containerPoint = SwingUtilities.convertPoint(
	 * this.getGlassPane(), glassPanePoint, videoPlayerTab);
	 * 
	 * int eventID = e.getID();
	 * 
	 * if (containerPoint.y < 0) { //we're not in the content pane. Could have
	 * special code to handle mouse events over //the menu bar or non-system
	 * window decorations, such as the ones provided by the Java look and feel.
	 * } else { //The mouse event is probably over the content pane. //Find out
	 * exactly which component it's over. Component component =
	 * SwingUtilities.getDeepestComponentAt(container, containerPoint.x,
	 * containerPoint.y);
	 * 
	 * if ((component != null) && (component.equals(this))) { //Forward events
	 * over the box. Point componentPoint =
	 * SwingUtilities.convertPoint(this.getGlassPane(),
	 * glassPanePoint,component); component.dispatchEvent(new
	 * MouseEvent(component, e.getID(), e.getWhen(), e.getModifiers(),
	 * componentPoint.x, componentPoint.y, e.getClickCount(),
	 * e.isPopupTrigger())); }
	 */
	/////////////////////////////////////////////////////////////////////////////////////////////
	// Methods used to set up the connection to the server
	/////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Connects to the default host:port
	 */
	public void connectToTheServer() {
		connectToTheServer(this.host, this.port);
	}

	public void connectToTheServer(String host, int port) {
		System.out.println("Connecting to " + host + ":" + port + "...");
		try {
			this.serverSocket = new Socket(host, port);
			System.out.println("Successfully connected to " + host + ":" + port);
			// setting up the output stream
			this.outputToServer = new BufferedWriter(new OutputStreamWriter(this.serverSocket.getOutputStream()));
		} catch (UnknownHostException e) {
			System.out.println("Unknown host, unable to connect to: " + host + ".");
			System.exit(-1);
		} catch (IOException e) {
			System.out.println("Couldn't open I/O connection " + host + ":" + port + ".");
			System.exit(-1);
		}
		readVideoListFromServer();
	}

	/////////////////////////////////////////////////////////////////////////////////////////////
	// Read video list from server
	// NOTE: the server should not directly send the video list as soon as the
	///////////////////////////////////////////////////////////////////////////////////////////// client
	///////////////////////////////////////////////////////////////////////////////////////////// connects,
	// it should wait untill the client requests the list, this is so that the
	///////////////////////////////////////////////////////////////////////////////////////////// client
	///////////////////////////////////////////////////////////////////////////////////////////// can
	// refresh its list by re-requesting it from the server. The current
	///////////////////////////////////////////////////////////////////////////////////////////// approach
	///////////////////////////////////////////////////////////////////////////////////////////// only
	///////////////////////////////////////////////////////////////////////////////////////////// allows
	// the list to be sent once at the start.
	/////////////////////////////////////////////////////////////////////////////////////////////
	private void readVideoListFromServer() {
		try {
			// tell the server to send the videolist
			send("GETLIST");
			inputFromServer = new ObjectInputStream(serverSocket.getInputStream());
			try {
				videoList = (List<VideoFile>) inputFromServer.readObject();
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
	 * 
	 * @param message
	 */
	private void send(String message) {
		try {
			outputToServer.write(message);
			outputToServer.newLine();
			outputToServer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// refreshes all GUI elements.
	private void updateClientWindow() {
		for (VideoFile video : videoList) {
			selectionBox.addItem(video.getTitle());
		}
		this.validate();
	}

	// closes all sockets to make sure that they can be used again if the client
	// is run again.
	public void closeSockets() {
		try {
			serverSocket.close();
		} catch (IOException e) {
			System.out.println("Failed to close client-sockets");
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
			this.errorOptionPane = new JOptionPane();
			// we should remove the popup message and just display red text
			// where the list should be
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
		this.setUpMediaPlayer("external_archives/VLC/vlc-2.0.1");
	}

	private void setUpMediaPlayer(String vlcLibraryPath) {
		NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), vlcLibraryPath);
		Native.loadLibrary(RuntimeUtil.getLibVlcLibraryName(), LibVlc.class);

		final EmbeddedMediaPlayerComponent mediaPlayerComponent = new EmbeddedMediaPlayerComponent();

		mediaPlayer = mediaPlayerComponent.getMediaPlayer();
		controlPanel = new PlayerControlsPanel(mediaPlayer);

		videoPlayerTab.add(mediaPlayerComponent, BorderLayout.CENTER);
		videoPlayerTab.add(controlPanel, BorderLayout.SOUTH);

		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				mediaPlayerComponent.release();
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
}
