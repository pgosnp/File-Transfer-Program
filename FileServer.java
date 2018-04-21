import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class FileServer {
    private String desPath = "/Users/ShInGSon/";
    private Lock lock = new Lock();
    //    String[] filelist = new String[]{
//            "1.txt", "2.txt", "3.txt"
//    };
    String[] filelist;

    public FileServer(int port) throws IOException, InterruptedException {
        ServerSocket sSocket = new ServerSocket(port);
        Socket cSocket = sSocket.accept();
        DataInputStream in = new DataInputStream(cSocket.getInputStream());
        System.out.println("socket is connected.");
        filelist =  receiveFilename(in).split(" ");
        System.out.println("Downloading " + filelist.length + " files with multiple threads");
        for (String file : filelist
                ) {
            DownloadThread downloadThread = new DownloadThread(file, desPath, lock, cSocket);
            downloadThread.start();
        }
        while (lock.getRunningThreadsNum() > 0) {
            synchronized (lock) {
                lock.wait();
            }
        }
    }

    public String receiveFilename(DataInputStream in) throws IOException{
        String str="";
        byte[] buffer = new byte[1024];
        int count = 0,len=0;
        int size = in.readInt();
        while (count<size){
            len = in.read(buffer, 0, (int) Math.min(buffer.length, size - count));
            count += len;
            str+=new String(buffer,0,len);
        }
        return str;
    }

    public void checkDir(String str){
        boolean check=false;
        String [] filelist;
        filelist = str.split(" ");
        File curDir = new File(desPath);
        for (String filename:filelist
                ) {
            File file = new File(desPath+"/"+filename);
            if(file.exists()&&file.isDirectory())check=true;
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        FileServer server = new FileServer(9000);
    }
}

class FileDownload {
    String filename;
    String desPath;
    Socket socket;

    public FileDownload(String filename, String desPath, Socket socket) throws InterruptedException {
        this.filename = filename;
        this.desPath = desPath;
        this.socket = socket;
    }

    public void sendFile(DataOutputStream out) throws IOException {
        String msg = "";
        byte[] buffer = new byte[1024];
        String filePath = desPath + "/" + filename;
        File file = new File(filePath);
        System.out.println(file.getCanonicalPath());
        FileInputStream fin = new FileInputStream(file);
        long size = file.length();
        long count = 0;
        int len;
        out.writeLong(size);
        while (count < size) {
            len = fin.read(buffer, 0, buffer.length);
            count += len;
            out.write(buffer, 0, len);
            System.out.println("file sends out...");
        }
    }
}

class Lock {
    private int runningThreadsNum;

    public Lock() {
        runningThreadsNum = 0;
    }

    public int getRunningThreadsNum() {
        return runningThreadsNum;
    }

    public void addRunningThreadsNum() {
        runningThreadsNum++;
    }

    public void removeRunningThreadsNum() {
        runningThreadsNum--;
    }
}

class DownloadThread extends Thread {
    private String filename;
    private String desPath;
    private Lock lock;
    private Socket socket;

    public DownloadThread(String filename, String desPath, Lock lock, Socket socket) {
        this.filename = filename;
        this.desPath = desPath;
        this.lock = lock;
        this.socket = socket;
    }

    public void run() {
        try {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            lock.addRunningThreadsNum();
            new FileDownload(filename, desPath, socket).sendFile(out);
            lock.removeRunningThreadsNum();
            synchronized (lock) {
                lock.notify();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

