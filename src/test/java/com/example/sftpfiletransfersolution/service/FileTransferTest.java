package com.example.sftpfiletransfersolution.service;

import com.jcraft.jsch.*;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.apache.commons.vfs2.*;
import org.apache.commons.vfs2.provider.sftp.IdentityInfo;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.Duration;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FileTransferTest {

    private String remoteHost = "127.0.0.1";
    private Integer port = 2222;
    private String username = "root";
    private String password = "password";
    private String keyPath = ".ssh/id_rsa";
    private String localFile = "src/main/resources/testFile.txt";
    private String remoteFile = "sample.txt";
    private String localDir = "src/main/resources/";
    private String remoteDir = "remote_sftp_test/";
    private Integer timeout = 15000;

    private Logger logger = LoggerFactory.getLogger(FileTransferTest.class);


    @Test
    public void whenUploadFileUsingJsch_thenSuccess() throws JSchException, SftpException {
        logger.info("About to upload file using jsch");
        ChannelSftp channelSftp = setupJsch();
        channelSftp.connect();
        channelSftp.put(localFile, "remote_jsch_test/jschFile.txt");
        channelSftp.exit();
        logger.info("Finished uploading file using jsch");
    }

    @Test
    public void whenDownloadFileUsingJsch_thenSuccess() throws JSchException, SftpException {
        logger.info("About to download file using jsch");
        ChannelSftp channelSftp = setupJsch();
        channelSftp.connect();
        channelSftp.get(remoteFile, localDir + "jschFile.txt");
        channelSftp.exit();
        logger.info("Finished downloading file using jsch");
    }

    @Test
    public void whenUploadFileUsingSshj_thenSuccess() throws IOException {
        logger.info("About to upload file using sshj");
        SSHClient sshClient = setupSshj();
        SFTPClient sftpClient = sshClient.newSFTPClient();
        sftpClient.put(localFile, remoteDir + "sshjFile.txt");
        sftpClient.close();
        sshClient.disconnect();
        logger.info("Finished uploading file using sshj");
    }

    @Test
    public void whenDownloadFileUsingSshj_thenSuccess() throws IOException {
        logger.info("About to download file using sshj");
        SSHClient sshClient = setupSshj();
        SFTPClient sftpClient = sshClient.newSFTPClient();
        sftpClient.get(remoteFile, localDir + "sshjFile.txt");
        sftpClient.close();
        sshClient.disconnect();
        logger.info("Finished downloading file using sshj");
    }

    @Test
    public void whenUploadFileUsingApacheVfs_thenSuccess() throws IOException {
        FileSystemManager manager = VFS.getManager();
        FileObject local = manager.resolveFile(System.getProperty("user.dir") + "/" + localFile);
        logger.info("local file path;", local);
        FileObject remote = manager.resolveFile(createVfsConnectionString(remoteHost, username, password, keyPath, "", remoteFile), createDefaultVfsOptions(keyPath, ""));
        logger.info("connection successful");
        remote.copyFrom(local, Selectors.SELECT_SELF);
        local.close();
        remote.close();
    }

    @Test
    public void whenDownloadFileUsingApacheVfs_thenSuccess() throws IOException {
        FileSystemManager manager = VFS.getManager();
        FileObject local = manager.resolveFile(System.getProperty("user.dir") + "/" + localDir + "vfsFile.txt");
        logger.info("local file path;", local);
        FileObject remote = manager.resolveFile(createVfsConnectionString(remoteHost, username, password, keyPath, "", remoteFile), createDefaultVfsOptions(keyPath, ""));
        logger.info("connection successful", remote);
        local.copyFrom(remote, Selectors.SELECT_SELF);
        local.close();
        remote.close();
    }


    private ChannelSftp setupJsch() throws JSchException {
        JSch jsch = new JSch();
        logger.info("/.ssh/id_rsa");
        jsch.addIdentity(keyPath);
        logger.info("private key added");
        Session jschSession = jsch.getSession(username, remoteHost, port);
        jschSession.setConfig("StrictHostKeyChecking", "no");
        jschSession.connect();
        return (ChannelSftp) jschSession.openChannel("sftp");
    }

    private SSHClient setupSshj() throws IOException {
        SSHClient client = new SSHClient();
        client.addHostKeyVerifier(new PromiscuousVerifier());
        client.connect(remoteHost, port);
        client.authPublickey(username, keyPath);
        logger.info("connection successful");
        return client;
    }

    private String createVfsConnectionString(String hostName, String username, String password, String keyPath, String passphrase, String remoteFilePath) {
        if (keyPath != null) {
            return "sftp://" + username + "@" + hostName + "/" + remoteFilePath;
        } else {
            return "sftp://" + username + ":" + password + "@" + hostName + "/" + remoteFilePath;
        }
    }

    private FileSystemOptions createDefaultVfsOptions(final String keyPath, final String passphrase) throws FileSystemException {

        //create options for sftp
        FileSystemOptions options = new FileSystemOptions();
        //ssh key
        SftpFileSystemConfigBuilder.getInstance().setStrictHostKeyChecking(options, "no");
        //set root directory to user home
        SftpFileSystemConfigBuilder.getInstance().setUserDirIsRoot(options, true);
        //timeout
        SftpFileSystemConfigBuilder.getInstance().setSessionTimeout(options, Duration.ofSeconds(10000));

        if (keyPath != null) {
            IdentityInfo identityInfo = null;
            if(passphrase != ""){
                identityInfo = new IdentityInfo(new File(keyPath), passphrase.getBytes());
            }else{
                identityInfo =  new IdentityInfo(new File(keyPath));
            }
            SftpFileSystemConfigBuilder.getInstance().setIdentityProvider(options, identityInfo);
            SftpFileSystemConfigBuilder.getInstance().setPreferredAuthentications(options, "publickey");
        }


        return options;
    }
}