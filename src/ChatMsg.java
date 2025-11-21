import javax.swing.*;
import java.awt.*;
import java.io.Serializable;

public class ChatMsg implements Serializable {
    public final static int MODE_LOGIN  =0x1;
    public final static int MODE_LOGOUT  =0x2;
    public final static int MODE_TX_STRING  =0x10;
    public final static int MODE_TX_FILE  =0x20;
    public final static int MODE_TX_IMAGE  =0x40;

    public String userID;
    public int mode;
    public String message;
    public ImageIcon image;
    public long size;

    public ChatMsg(String userID, int mode, String message, ImageIcon image, long size){
        this.userID = userID;
        this.mode= mode;
        this.message =message;
        this.image=image;
        this.size=size;
    }

    public ChatMsg(String userID, int mode, String message, ImageIcon image){
        this(userID, mode, message, image, 0);
    }

    public ChatMsg(String userID, int mode){
        this(userID, mode, null, null);
    }

    public ChatMsg(String userID, int mode, String message){
        this(userID, mode, message, null);
    }

    public ChatMsg(String userID, int mode , ImageIcon image){
        this(userID, mode, null, image);
    }

    public ChatMsg(String userID, int mode, String filename, long size){
        this(userID, mode, filename, null, size);
    }

}
