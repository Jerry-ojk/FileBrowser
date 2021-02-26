package com.jcraft.jsch;

public interface Logger {

    public final int DEBUG = 0;
    public final int INFO = 1;
    public final int WARN = 2;
    public final int ERROR = 3;
    public final int FATAL = 4;

    public boolean isEnabled(int level);

    public void log(int level, String message);

  /*
  public final Logger SIMPLE_LOGGER=new Logger(){
      public boolean isEnabled(int level){return true;}
      public void log(int level, String message){System.err.println(message);}
    };
  final Logger DEVNULL=new Logger(){
      public boolean isEnabled(int level){return false;}
      public void log(int level, String message){}
    };
  */
}