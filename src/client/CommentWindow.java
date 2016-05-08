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

public class CommentWindow extends JFrame{
	
	private JTextPane commentsPane;
	private JTextPane commentPane;
	private JButton btnComment;
	private VideoFile video;
	private Client client;
	private UserAccount user;
	private List<String> comments = new ArrayList<String>();

	public CommentWindow(VideoFile video,Client client,UserAccount user){
		
		this.client = client;
		this.video = video;
		this.user = user;
		
		getContentPane().setLayout(null);
		btnComment = new JButton("Submit");
		btnComment.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent arg0) {
				//test that comment box is not empty
				if(commentPane.getText().length() >= 1){
					client.send("COMMENT");
					client.send(video.getID());
					client.send("["+user.getUserNameID()+"]: " +commentPane.getText());
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
		client.send("GET VIDEO COMMENTS");
		client.send(video.getID());
		comments = (ArrayList<String>)client.read();
		String text = "";
		for (String comment : comments) {
			text += comment + "\n";
		}
		commentsPane.setText(text);
	}
}
