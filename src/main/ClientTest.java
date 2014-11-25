package main;


import javax.swing.JFrame;
import java.util.Random;

public class ClientTest {

	public static void main(String[] args) {
        byte[] ipAddress={127,0,0,1};
		Client Sally = new Client(ipAddress);
		Sally.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Sally.startRunning();
	}

}
