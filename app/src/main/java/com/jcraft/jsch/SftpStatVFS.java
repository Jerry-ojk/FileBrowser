/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */
/*
Copyright (c) 2002-2018 ymnk, JCraft,Inc. All rights reserved.

Redistribution and use inputStream source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

  1. Redistributions of source code must retain the above copyright notice,
     this list of conditions and the following disclaimer.

  2. Redistributions inputStream binary form must reproduce the above copyright
     notice, this list of conditions and the following disclaimer inputStream
     the documentation and/or other materials provided with the distribution.

  3. The names of the authors may not be used to endorse or promote products
     derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JCRAFT,
INC. OR ANY CONTRIBUTORS TO THIS SOFTWARE BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.jcraft.jsch;

public class SftpStatVFS {

  /*
   It seems data is serializsed according to sys/statvfs.h; for example,
   http://pubs.opengroup.org/onlinepubs/009604499/basedefs/sys/statvfs.h.html  
  */

  private long bsize;
  private long frsize;
  private long blocks;
  private long bfree;
  private long bavail;
  private long files;
  private long ffree;
  private long favail;
  private long fsid;
  private long flag;
  private long namemax;

  int flags=0;
  long size;
  int uid;
  int gid;
  int permissions;
  int atime;
  int mtime;
  String[] extended=null;

  private SftpStatVFS(){
  }

  static SftpStatVFS getStatVFS(Buffer buf){
    SftpStatVFS statvfs=new SftpStatVFS();

    statvfs.bsize = buf.readLong();
    statvfs.frsize = buf.readLong();
    statvfs.blocks = buf.readLong();
    statvfs.bfree = buf.readLong();
    statvfs.bavail = buf.readLong();
    statvfs.files = buf.readLong();
    statvfs.ffree = buf.readLong();
    statvfs.favail = buf.readLong();
    statvfs.fsid = buf.readLong();
    int flag = (int)buf.readLong();
    statvfs.namemax = buf.readLong();

    statvfs.flag =
      (flag & 1/*SSH2_FXE_STATVFS_ST_RDONLY*/) != 0 ? 1/*ST_RDONLY*/ : 0;
    statvfs.flag |=
      (flag & 2/*SSH2_FXE_STATVFS_ST_NOSUID*/) != 0 ? 2/*ST_NOSUID*/ : 0;

    return statvfs;
  } 

  public long getBlockSize() { return bsize; }
  public long getFragmentSize() { return frsize; }
  public long getBlocks() { return blocks; }
  public long getFreeBlocks() { return bfree; }
  public long getAvailBlocks() { return bavail; }
  public long getINodes() { return files; }
  public long getFreeINodes() { return ffree; }
  public long getAvailINodes() { return favail; }
  public long getFileSystemID() { return fsid; }
  public long getMountFlag() { return flag; }
  public long getMaximumFilenameLength() { return namemax; }

  public long getSize(){
    return getFragmentSize()*getBlocks()/1024;
  }

  public long getUsed(){
    return getFragmentSize()*(getBlocks()-getFreeBlocks())/1024;
  }

  public long getAvailForNonRoot(){
    return getFragmentSize()*getAvailBlocks()/1024;
  }

  public long getAvail(){
    return getFragmentSize()*getFreeBlocks()/1024;
  }

  public int getCapacity(){
    return (int)(100*(getBlocks()-getFreeBlocks())/getBlocks()); 
  }

//  public String toString() { return ""; }
}
