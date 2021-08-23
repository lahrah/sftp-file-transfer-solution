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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FileTransferTest {

    private String remoteHost = "127.0.0.1";
    private Integer port = 2222;
    private String username = "root";
    private String password = "password";
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

        FileSystemOptions opts = new FileSystemOptions();
        SftpFileSystemConfigBuilder.getInstance().setStrictHostKeyChecking(opts, "no");
        SftpFileSystemConfigBuilder.getInstance().setUserDirIsRoot(opts, false);
        IdentityInfo myIdentityInfo = new IdentityInfo(new File(".ssh/id_rsa"));

        SftpFileSystemConfigBuilder.getInstance(). setIdentityInfo(opts, myIdentityInfo);

        FileObject local = manager.resolveFile(System.getProperty("user.dir") + "/" + localFile);
        FileObject remote = manager.resolveFile("sftp://" + username + ":" + password + "@" + remoteHost + "/" + remoteDir + "vfsFile.txt");
        remote.copyFrom(local, Selectors.SELECT_SELF);
        local.close();
        remote.close();
    }

    @Test
    public void whenDownloadFileUsingApacheVfs_thenSuccess() throws IOException {
        FileSystemManager manager = VFS.getManager();
        FileObject local = manager.resolveFile(System.getProperty("user.dir") + "/" + localDir + "vfsFile.txt");
        FileObject remote = manager.resolveFile("sftp://" + username + ":" + password + "@" + remoteHost + "/" + remoteFile);
        local.copyFrom(remote, Selectors.SELECT_SELF);
        local.close();
        remote.close();
    }


    private ChannelSftp setupJsch() throws JSchException {
        JSch jsch = new JSch();
        logger.info("/.ssh/id_rsa");
        jsch.addIdentity(".ssh/id_rsa");
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
        client.authPublickey(username, ".ssh/id_rsa");
        logger.info("connection successful");
        return client;
    }
}