package main;

import xmlparser.EventListener;
import xmlparser.Triple;

import java.util.List;

public class P2PChatListener implements EventListener {
    public boolean isAccept() {
        return accept;
    }

    public void setAccept(boolean accept) {
        this.accept = accept;
    }

    private boolean accept;
    @Deprecated
    @Override
    public void OnRegisteredPort(String userName, String port) {

    }
    @Deprecated
    @Override
    public void OnStillAliveNoti(String userName) {

    }

    @Override
    public void OnDisconnectNoti(String userName) {

    }

    @Override
    public void OnRegisterDenied() {

    }

    @Override
    public void OnRegisterAccepted(List<Triple<String, String, String>> userList) {

    }

    @Override
    public void OnChatRequest(String userName) {

    }

    @Override
    public void OnChatRequestDenied() {
        accept=false;
    }

    @Override
    public void OnChatRequestAccepted() {
        accept=true;
    }

    @Override
    public void OnChatMessage(String message) {

    }
}