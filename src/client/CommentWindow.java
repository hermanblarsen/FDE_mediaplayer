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
	private String videoField;
	private Client clientField;
	private UserAccount userField;
	private List<String> comments = new ArrayList<String>();

	public CommentWindow(String video,Client client,UserAccount user){
		
		this.clientField = client;
		this.videoField = video;
		this.userField = user;
		
		getContentPane().setLayout(null);
		btnComment = new JButton("Submit");
		btnComment.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				//test that comment box is not empty
				if(commentPane.getText().length() >= 1){
					clientField.send("COMMENT");
					clientField.send(videoField);
					clientField.send("["+userField.getUserNameID()+"]: " +commentPane.getText());
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
		clientField.send("GET VIDEO COMMENTS");
		clientField.send(videoField);
		comments = (ArrayList<String>)clientField.read();
		String text = "";
		for (String comment : comments) {
			text += comment + "\n";
		}
		commentsPane.setText(text);
	}
}
