package jerry.filebrowser.shell;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import jerry.filebrowser.util.Util;
import jerry.filebrowser.ssh.SSHConnectConfig;

public class ShellConfig extends SSHConnectConfig implements Parcelable {
    public long time = 0;

    public ShellConfig() {
    }

    protected ShellConfig(Parcel in) {
        host = in.readString();
        port = in.readInt();
        user = in.readString();
        pwd = in.readString();
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
        dest.writeLong(time);
    }

    public boolean equals(@Nullable ShellConfig config) {
        if (this == config) return true;
        if (config == null) return false;
        return Util.equals(this.host, config.host)
                && port == config.port
                && Util.equals(this.user, config.user)
                && Util.equals(this.pwd, config.pwd);
    }

    public static final Creator<ShellConfig> CREATOR = new Creator<ShellConfig>() {
        @Override
        public ShellConfig createFromParcel(Parcel in) {
            return new ShellConfig(in);
        }

        @Override
        public ShellConfig[] newArray(int size) {
            return new ShellConfig[size];
        }
    };
}