package src.client;

import javax.swing.JFrame;

import src.server.UserAccount;
import src.server.VideoFile;
import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.JTextArea;
import javax.swing.JEditorPane;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.awt.event.ActionEvent;
import java.awt.Dimension;
import java.awt.Point;

public class CommentWindow extends JFrame{
	
	private JTextPane commentsPane;
	private JTextPane commentPane;
	private JButton btnComment;
	private String selectedVideo;
	private Client connectedClient;
	private UserAccount connectedUser;
	private List<String> comments = new ArrayList<String>();

	public CommentWindow(String aSelectedVideo, Client aConnectedClient, UserAccount aConnectedUser) {
		setLocation(new Point(100, 100));
		setPreferredSize(new Dimension(600, 400));
		setResizable(false);
		setMinimumSize(new Dimension(600, 430));
		setMaximumSize(new Dimension(600, 430));
		setAlwaysOnTop(true);
		
		this.connectedClient = aConnectedClient;
		this.selectedVideo = aSelectedVideo;
		this.connectedUser = aConnectedUser;
		
		getContentPane().setLayout(null);
		btnComment = new JButton("Submit");
		btnComment.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				//test that comment box is not empty
				if(commentPane.getText().length() >= 1){
					connectedClient.send("COMMENT");
					connectedClient.send(selectedVideo);
					connectedClient.send("["+connectedUser.getUserNameID()+"]: " +commentPane.getText());
					//resetting the comment pane
					commentPane.setText("");
					//obtaining the new updated comment list
					getVideoComments();
				}
			}
		});
		btnComment.setBounds(471, 307, 115, 78);
		getContentPane().add(btnComment);
		
		commentsPane = new JTextPane();
		commentsPane.setEditable(false);
		commentsPane.setBounds(15, 16, 571, 275);
		getContentPane().add(commentsPane);
		
		commentPane = new JTextPane();
		commentPane.setBounds(15, 307, 445, 78);
		getContentPane().add(commentPane);
		
		getVideoComments();
		
	}

	private void getVideoComments() {
		connectedClient.send("GET VIDEO COMMENTS");
		connectedClient.send(selectedVideo);
		
		//Make sure the object in the stream is the arraylist.
		Object clientInput;
		do {
			clientInput = connectedClient.read();
		} while(!(clientInput instanceof ArrayList<?>));
		//TODO potential infinite loop
		
		comments = (ArrayList<String>)clientInput;
		String text = "";
		//if there are no comments comments will be null, hence the check.
		if (comments != null) {
			for (String comment : comments) {
				text += comment + "\n";
			} 
		}
		commentsPane.setText(text);
	}
}
