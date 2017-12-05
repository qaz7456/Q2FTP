package tw.com.q2ftp.request.mode;

import java.util.List;

public class BankPro {
	private FtpClient ftpClient;
	private QueueConnectionFactory queueConnectionFactory;
	private HeartBeatClient heartBeatClient;
	private List<OrderMaster> orderMasters;

	public FtpClient getFtpClient() {
		return ftpClient;
	}

	public void setFtpClient(FtpClient ftpClient) {
		this.ftpClient = ftpClient;
	}

	public QueueConnectionFactory getQueueConnectionFactory() {
		return queueConnectionFactory;
	}

	public void setQueueConnectionFactory(QueueConnectionFactory queueConnectionFactory) {
		this.queueConnectionFactory = queueConnectionFactory;
	}

	public HeartBeatClient getHeartBeatClient() {
		return heartBeatClient;
	}

	public void setHeartBeatClient(HeartBeatClient heartBeatClient) {
		this.heartBeatClient = heartBeatClient;
	}

	public List<OrderMaster> getOrderMasters() {
		return orderMasters;
	}

	public void setOrderMasters(List<OrderMaster> orderMasters) {
		this.orderMasters = orderMasters;
	}



}
