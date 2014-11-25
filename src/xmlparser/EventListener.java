package xmlparser;

import java.util.List;

public interface EventListener {
	public void OnRegisteredPort(String userName,String port);
	public void OnStillAliveNoti(String userName);
	public void OnDisconnectNoti(String userName);
	public void OnRegisterDenied();
	public void OnRegisterAccepted(List<Triple<String,String,String>> userList);
	public void OnChatRequest(String userName);
	public void OnChatRequestDenied();
	public void OnChatRequestAccepted();
	public void OnChatMessage(String message);
}
