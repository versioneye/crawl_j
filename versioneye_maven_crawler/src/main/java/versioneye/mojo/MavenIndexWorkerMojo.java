package versioneye.mojo;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.QueueingConsumer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import versioneye.domain.MavenRepository;
import versioneye.domain.Repository;
import versioneye.service.RabbitMqService;

import java.util.Date;
import java.util.Properties;


@Mojo( name = "maven_index_worker", defaultPhase = LifecyclePhase.PROCESS_SOURCES )
public class MavenIndexWorkerMojo extends AetherMojo {

    private final static String QUEUE_NAME = "maven_index_worker";

    public void execute() throws MojoExecutionException, MojoFailureException {
        try{
            super.execute();

            username = null;
            password = null;

            Connection connection = initConnection();
            Channel channel = connection.createChannel();

            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            getLog().info(" [*] Waiting for messages. To exit press CTRL+C");

            QueueingConsumer consumer = new QueueingConsumer(channel);
            channel.basicConsume(QUEUE_NAME, true, consumer);

            while (true) {
                QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                String message = new String(delivery.getBody());
                getLog().info(" . ");
                getLog().info(" [x] Received '" + message + "'");
                processMessage( message );
            }
        } catch( Exception exception ){
            exception.printStackTrace();
            getLog().error(exception);
            throw new MojoExecutionException("Oh no! Something went wrong. Get in touch with the VersionEye guys and give them feedback.", exception);
        }
    }

    private void processMessage(String message){
        try{
            String[] sps = message.split("::");

            String repoName     = sps[0].toLowerCase();
            String repoUrl      = sps[1];
            String gav          = sps[2];
            String lastModified = sps[3];

            fetchUserAndPassword();

            Repository repo = repositoryUtils.convertRepository(repoName, repoUrl, null);
            setRepository(repo, true);

            MavenRepository mrepo = repositoryUtils.convertMavenRepository(repoName, repoUrl, null);
            setMavenRepository(mrepo, true);

            Date releasedAt = getReleasedDate(lastModified);

            processGav(gav, releasedAt);
        } catch (Exception exception) {
            exception.printStackTrace();
            getLog().error(exception);
        }
    }

    private Connection initConnection() throws Exception {
        String rabbitmqAddr = System.getenv("RM_PORT_5672_TCP_ADDR");
        String rabbitmqPort = System.getenv("RM_PORT_5672_TCP_PORT");
        if (rabbitmqAddr == null || rabbitmqAddr.isEmpty() || rabbitmqPort == null || rabbitmqPort.isEmpty()){
            Properties properties = getProperties();
            rabbitmqAddr = properties.getProperty("rabbitmq_addr");
            rabbitmqPort = properties.getProperty("rabbitmq_port");
        }
        return RabbitMqService.getConnection(rabbitmqAddr, new Integer(rabbitmqPort));
    }


    private Date getReleasedDate(String lastModified){
        Date releasedAt = null;
        try{
            if (lastModified != null && !lastModified.trim().isEmpty()){
                releasedAt = new Date(new Long(lastModified));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return releasedAt;
    }

}
