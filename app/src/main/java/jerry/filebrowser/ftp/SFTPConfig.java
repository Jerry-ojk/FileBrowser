package jerry.filebrowser.ftp;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import jerry.filebrowser.ssh.SSHConnectConfig;
import jerry.filebrowser.util.Util;

public class SFTPConfig extends SSHConnectConfig implements Parcelable {
    public String path;
    public boolean passive;
    public long time = 0;

    public SFTPConfig() {
    }

    protected SFTPConfig(Parcel in) {
        host = in.readString();
        port = in.readInt();
        user = in.readString();
        pwd = in.readString();
        path = in.readString();
        passive = in.readByte() != 0;
        time = in.readLong();
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(host);
        dest.writeInt(port);
        dest.writeString(user);
        dest.writeString(pwd);
        dest.writeString(path);
        dest.writeByte((byte) (passive ? 1 : 0));
        dest.writeLong(time);
    }

    public boolean equals(@Nullable SFTPConfig config) {
        if (this == config) return true;
        if (config == null) return false;
        return Util.equals(this.host, config.host)
                && port == config.port
                && Util.equals(this.user, config.user)
                && Util.equals(this.pwd, config.pwd)
                && this.passive == config.passive;
    }

    public static final Creator<SFTPConfig> CREATOR = new Creator<SFTPConfig>() {
        @Override
        public SFTPConfig createFromParcel(Parcel in) {
            return new SFTPConfig(in);
        }

        @Override
        public SFTPConfig[] newArray(int size) {
            return new SFTPConfig[size];
        }
    };
}