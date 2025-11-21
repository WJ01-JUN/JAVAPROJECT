package chat.common;

import javax.swing.*;
import java.awt.*;
import java.io.Serializable;

public class ChatMsg implements Serializable {
    public final static int MODE_LOGIN  =0x1;
    public final static int MODE_LOGOUT  =0x2;
    public final static int MODE_TX_STRING  =0x10;
    public final static int MODE_TX_FILE  =0x20;
    public final static int MODE_TX_IMAGE  =0x40;
    public static final int MODE_JOIN_ROOM  = 0x80;
    public static final int MODE_LEAVE_ROOM = 0x81;

    public String userID;
    String roomID;
    public int mode;
    public String message;
    public ImageIcon image;
    long size;

    public ChatMsg(String userID, String roomID,int mode, String message, ImageIcon image, long size){
        this.userID = userID;
        this.roomID = roomID;
        this.mode= mode;
        this.message =message;
        this.image=image;
        this.size=size;
    }

    public ChatMsg(String userID, int mode) {
        this(userID, "lobby", mode, null, null, 0);
    }

    public ChatMsg(String userID, int mode, String message) {
        this(userID, "lobby", mode, message, null, 0);
    }

    public ChatMsg(String userID, int mode, String message, ImageIcon image) {
        this(userID, "lobby", mode, message, image, 0);
    }

    public ChatMsg(String userID, int mode, ImageIcon image) {
        this(userID, "lobby", mode, null, image, 0);
    }

    public ChatMsg(String userID, int mode, String filename, long size) {
        this(userID, "lobby", mode, filename, null, size);
    }
}

