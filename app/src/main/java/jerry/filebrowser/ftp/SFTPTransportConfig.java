package jerry.filebrowser.ftp;

import android.os.Parcel;
import android.os.Parcelable;

public class SFTPTransportConfig implements Parcelable {
    public String remotePath;
    public String localPath;

    public String destDir;
    public String name;

    public SFTPConfig config;
    public int progress;
    public boolean isFinish;

    public SFTPTransportConfig(String localPath, String remotePath) {
        this.localPath = localPath;
        this.remotePath = remotePath;
    }

    public SFTPTransportConfig(String destDir, String name, String remotePath) {
        this.destDir = destDir;
        this.name = name;
        this.remotePath = remotePath;
    }

    protected SFTPTransportConfig(Parcel in) {
        remotePath = in.readString();
        destDir = in.readString();
        name = in.readString();
        localPath = in.readString();
        config = in.readParcelable(SFTPConfig.class.getClassLoader());
        progress = in.readInt();
        isFinish = in.readByte() != 0;
    }

    public static final Creator<SFTPTransportConfig> CREATOR = new Creator<SFTPTransportConfig>() {
        @Override
        public SFTPTransportConfig createFromParcel(Parcel in) {
            return new SFTPTransportConfig(in);
        }

        @Override
        public SFTPTransportConfig[] newArray(int size) {
            return new SFTPTransportConfig[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(remotePath);
        dest.writeString(destDir);
        dest.writeString(name);
        dest.writeString(localPath);
        dest.writeParcelable(config, flags);
        dest.writeInt(progress);
        dest.writeByte((byte) (isFinish ? 1 : 0));
    }
}
