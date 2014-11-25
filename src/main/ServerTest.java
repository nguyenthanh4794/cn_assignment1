package main;


import javax.swing.JFrame;
public class ServerTest {

	public static void main(String[] args) {
		Server Sally = new Server();
		Sally.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Sally.startRunning();
	}

}
