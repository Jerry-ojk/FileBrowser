package com.jcraft.jsch;

import com.jcraft.jsch.exception.JSchException;

import java.util.Vector;

class LocalIdentityRepository implements IdentityRepository {
    private static final String name = "Local Identity Repository";

    private Vector<Identity> identities = new Vector<Identity>();
    private SSHClient sshClient;

    LocalIdentityRepository(SSHClient sshClient) {
        this.sshClient = sshClient;
    }

    public String getName() {
        return name;
    }

    public int getStatus() {
        return RUNNING;
    }

    public synchronized Vector<Identity> getIdentities() {
        removeDupulicates();
        Vector<Identity> v = new Vector<Identity>();
        for (int i = 0; i < identities.size(); i++) {
            v.addElement(identities.elementAt(i));
        }
        return v;
    }

    public synchronized void add(Identity identity) {
        if (!identities.contains(identity)) {
            byte[] blob1 = identity.getPublicKeyBlob();
            if (blob1 == null) {
                identities.addElement(identity);
                return;
            }
            for (int i = 0; i < identities.size(); i++) {
                byte[] blob2 = (identities.elementAt(i)).getPublicKeyBlob();
                if (blob2 != null && Util.array_equals(blob1, blob2)) {
                    if (!identity.isEncrypted() &&
                            (identities.elementAt(i)).isEncrypted()) {
                        remove(blob2);
                    } else {
                        return;
                    }
                }
            }
            identities.addElement(identity);
        }
    }

    public synchronized boolean add(byte[] identity) {
        try {
            Identity _identity =
                    IdentityFile.newInstance("from remote:", identity, null, sshClient);
            add(_identity);
            return true;
        } catch (JSchException e) {
            return false;
        }
    }

    synchronized void remove(Identity identity) {
        if (identities.contains(identity)) {
            identities.removeElement(identity);
            identity.clear();
        } else {
            remove(identity.getPublicKeyBlob());
        }
    }

    public synchronized boolean remove(byte[] blob) {
        if (blob == null) return false;
        for (int i = 0; i < identities.size(); i++) {
            Identity _identity = (identities.elementAt(i));
            byte[] _blob = _identity.getPublicKeyBlob();
            if (_blob == null || !Util.array_equals(blob, _blob))
                continue;
            identities.removeElement(_identity);
            _identity.clear();
            return true;
        }
        return false;
    }

    public synchronized void removeAll() {
        for (int i = 0; i < identities.size(); i++) {
            Identity identity = (identities.elementAt(i));
            identity.clear();
        }
        identities.removeAllElements();
    }

    private void removeDupulicates() {
        Vector<byte[]> v = new Vector<byte[]>();
        int len = identities.size();
        if (len == 0) return;
        for (int i = 0; i < len; i++) {
            Identity foo = identities.elementAt(i);
            byte[] foo_blob = foo.getPublicKeyBlob();
            if (foo_blob == null) continue;
            for (int j = i + 1; j < len; j++) {
                Identity bar = identities.elementAt(j);
                byte[] bar_blob = bar.getPublicKeyBlob();
                if (bar_blob == null) continue;
                if (Util.array_equals(foo_blob, bar_blob) &&
                        foo.isEncrypted() == bar.isEncrypted()) {
                    v.addElement(foo_blob);
                    break;
                }
            }
        }
        for (int i = 0; i < v.size(); i++) {
            remove(v.elementAt(i));
        }
    }
}
