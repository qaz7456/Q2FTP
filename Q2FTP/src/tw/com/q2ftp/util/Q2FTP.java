package tw.com.q2ftp.util;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import tw.com.heartbeat.clinet.serivce.HeartBeatService;
import tw.com.heartbeat.clinet.vo.HeartBeatClientVO;
import tw.com.q2ftp.request.mode.BankPro;
import tw.com.q2ftp.request.mode.FtpClient;
import tw.com.q2ftp.request.mode.OrderDetail;
import tw.com.q2ftp.request.mode.OrderMaster;

public class Q2FTP extends Thread {
	private static final Logger logger = LogManager.getLogger(Q2FTP.class);

	private String configPath = null;
	private String heartBeatXmlFilePath = null;

	public Q2FTP(String configPath, String heartBeatXmlFilePath) {
		this.configPath = configPath;
		this.heartBeatXmlFilePath = heartBeatXmlFilePath;
	}

	@Override
	public void run() {
		Document configDoc = XMLUtil.getDocumentForFilePath(configPath);
		Element configRoot = configDoc.getDocumentElement();
		NodeList heartBeatClient = configRoot.getElementsByTagName("heartBeatClient");

		NodeList clientInfo = heartBeatClient.item(0).getChildNodes();

		String beatID = null, fileName = null;
		long timeSeries = 0;
		LocalDateTime localDateTime = LocalDateTime.now();

		for (int i = 0; i < clientInfo.getLength(); i++) {
			Node node = (Node) clientInfo.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				String nodeName = node.getNodeName();
				String value = node.getTextContent();

				beatID = nodeName.equals("beatID") ? value : beatID;
				fileName = nodeName.equals("fileName") ? value : fileName;
				timeSeries = nodeName.equals("timeSeries") ? Long.parseLong(value) : timeSeries;
			}
		}

		HeartBeatClientVO heartBeatClientVO = new HeartBeatClientVO();

		heartBeatClientVO.setBeatID(beatID);
		heartBeatClientVO.setFileName(fileName);
		heartBeatClientVO.setLocalDateTime(localDateTime);
		heartBeatClientVO.setTimeSeries(timeSeries);

		HeartBeatService heartBeatService = new HeartBeatService(heartBeatXmlFilePath);
		heartBeatService.setHeartBeatClientVO(heartBeatClientVO);

		NodeList ftpClient = configRoot.getElementsByTagName("ftpClient");

		clientInfo = ftpClient.item(0).getChildNodes();

		String host = null, user = null, password = null, ein = null, dir = null;
		int port = 0;

		for (int i = 0; i < clientInfo.getLength(); i++) {
			Node node = (Node) clientInfo.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				String nodeName = node.getNodeName();
				String value = node.getTextContent();

				host = nodeName.equals("host") ? value : host;
				user = nodeName.equals("user") ? value : user;
				password = nodeName.equals("password") ? value : password;
				ein = nodeName.equals("ein") ? value : ein;
				dir = nodeName.equals("dir") ? value : dir;
				port = nodeName.equals("port") ? Integer.parseInt(value) : port;
			}
		}

		FtpClient sftp = new FtpClient();
		sftp.setHost(host);
		sftp.setPort(port);
		sftp.setUser(user);
		sftp.setPassword(password);
		sftp.setDir(dir);

		String content = null;

		while (true) {

			try {
				heartBeatService.beat();

				content = RabbitMQ.Pull(configPath);
				logger.debug("提取: {}", content);

				if (content != null) {

					try {
						String bankProStr = Q2FTP.jsonToBankPro(content);

						SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");

						String dateStr = sdf.format(new java.util.Date());

						if (Q2FTP.hasOrderDetails(content)) {
							ein += "-O-MD-";
							ein += dateStr;
						} else {
							ein += "-O-M-";
							ein += dateStr;
						}
						sftp.setEin(ein);

						logger.debug("轉換: {}", bankProStr);

						logger.debug("檔名: {}", ein);

						logger.debug(Q2FTP.strToSFTPFile(sftp, bankProStr) ? "上傳成功" : "上傳失敗");
					} catch (Exception e) {
						try {
							RabbitMQ.ErrorPush(content, configPath);
						} catch (Exception e1) {
							logger.error(e1.getMessage());
						}
					}
				}

			} catch (Exception e) {
				logger.error(e.getMessage());
			}
			if (content == null) {
				try {
					logger.debug("休息" + timeSeries + "毫秒");
					Thread.sleep(timeSeries);
				} catch (InterruptedException e) {
					logger.error(e.getMessage());
				}
			}
		}
	}

	public static boolean strToSFTPFile(FtpClient ftpClient, String content) {
		boolean isSuccess = false;

		try {
			String user = ftpClient.getUser();
			String host = ftpClient.getHost();
			String password = ftpClient.getPassword();
			String dir = ftpClient.getDir();
			String ein = ftpClient.getEin();
			int port = ftpClient.getPort();

			JSch jsch = new JSch();
			Session session = jsch.getSession(user, host, port);
			session.setPassword(password);
			Properties sshConfig = new Properties();
			sshConfig.put("StrictHostKeyChecking", "no");
			session.setConfig(sshConfig);
			session.connect();

			ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
			sftpChannel.connect();
			sftpChannel.cd(dir);

			InputStream stream = new ByteArrayInputStream(content.getBytes());
			sftpChannel.put(stream, (ein + ".txt"));
			isSuccess = true;
		} catch (Exception e) {
			e.printStackTrace();
			return isSuccess;
		}
		return isSuccess;
	}

	public void readMesToFile(String message) {

		Gson gson = new Gson();
		Type type = new TypeToken<BankPro>() {
		}.getType();
		BankPro bankPro = gson.fromJson(message, type);
		List<OrderMaster> orderMasters = bankPro.getOrderMasters();

		StringBuffer stringBuffer = new StringBuffer();

		for (int i = 0; i < orderMasters.size(); i++) {
			OrderMaster orderMaster = orderMasters.get(i);

			String order_code = orderMaster.getOrder_code();
			String order_num = orderMaster.getOrder_num();
			String order_status = orderMaster.getOrder_status();
			String order_date = orderMaster.getOrder_date();
			String order_ship_date = orderMaster.getOrder_ship_date();
			String order_vat_type = orderMaster.getOrder_vat_type();
			String order_without_vat_price = orderMaster.getOrder_without_vat_price();
			String order_vat_price = orderMaster.getOrder_vat_price();
			String order_include_vat_price = orderMaster.getOrder_include_vat_price();
			String seller_ein_code = orderMaster.getBuyer_ein_code();
			String seller_plant_code = orderMaster.getSeller_plant_code();
			String buyer_ein_code = orderMaster.getBuyer_ein_code();
			String buyer_company_name = orderMaster.getBuyer_company_name();
			String member_id = orderMaster.getMember_id();
			String member_name = orderMaster.getMember_name();
			String member_postal_code = orderMaster.getMember_postal_code();
			String member_address = orderMaster.getMember_address();
			String member_phone = orderMaster.getMember_phone();
			String member_landline = orderMaster.getMember_landline();
			String member_mail = orderMaster.getMember_mail();
			String bonus_points_discount_mount = orderMaster.getBonus_points_discount_mount();
			String request_paper_invoice = orderMaster.getRequest_paper_invoice();
			String invoice_donation_note = orderMaster.getInvoice_donation_note();
			String order_note = orderMaster.getOrder_note();
			String payment_method = orderMaster.getPayment_method();
			String related_number_1 = orderMaster.getRelated_number_1();
			String related_number_2 = orderMaster.getRelated_number_2();
			String related_number_3 = orderMaster.getRelated_number_3();
			String main_note = orderMaster.getMain_note();
			String product_name = orderMaster.getProduct_name();
			String carrier_category_number = orderMaster.getCarrier_category_number();
			String carrier_code_1 = orderMaster.getCarrier_code_1();
			String carrier_code_2 = orderMaster.getCarrier_code_2();
			String invoice_number = orderMaster.getInvoice_number();
			String random_code = orderMaster.getRandom_code();

			stringBuffer.append(order_code).append("|");
			stringBuffer.append(order_num).append("|");
			stringBuffer.append(order_status).append("|");
			stringBuffer.append(order_date).append("|");
			stringBuffer.append(order_ship_date).append("|");
			stringBuffer.append(order_vat_type).append("|");
			stringBuffer.append(order_without_vat_price).append("|");
			stringBuffer.append(order_vat_price).append("|");
			stringBuffer.append(order_include_vat_price).append("|");
			stringBuffer.append(seller_ein_code).append("|");
			stringBuffer.append(seller_plant_code).append("|");
			stringBuffer.append(buyer_ein_code).append("|");
			stringBuffer.append(buyer_company_name).append("|");
			stringBuffer.append(member_id).append("|");
			stringBuffer.append(member_name).append("|");
			stringBuffer.append(member_postal_code).append("|");
			stringBuffer.append(member_address).append("|");
			stringBuffer.append(member_phone).append("|");
			stringBuffer.append(member_landline).append("|");
			stringBuffer.append(member_mail).append("|");
			stringBuffer.append(bonus_points_discount_mount).append("|");
			stringBuffer.append(request_paper_invoice).append("|");
			stringBuffer.append(invoice_donation_note).append("|");
			stringBuffer.append(order_note).append("|");
			stringBuffer.append(payment_method).append("|");
			stringBuffer.append(related_number_1).append("|");
			stringBuffer.append(related_number_2).append("|");
			stringBuffer.append(related_number_3).append("|");
			stringBuffer.append(main_note).append("|");
			stringBuffer.append(product_name).append("|");
			stringBuffer.append(carrier_category_number).append("|");
			stringBuffer.append(carrier_code_1).append("|");
			stringBuffer.append(carrier_code_2).append("|");
			stringBuffer.append(invoice_number).append("|");
			stringBuffer.append(random_code).append("|");
			stringBuffer.append("\\n");

			List<OrderDetail> orderDetails = orderMaster.getOrderDetails();

			for (int j = 0; i < orderDetails.size(); j++) {
				OrderDetail orderDetail = orderDetails.get(j);
				String detail_code = orderDetail.getDetail_code();
				String detail_serial_num = orderDetail.getDetail_serial_num();
				String detail_order_num = orderDetail.getDetail_order_num();
				String detail_product_num = orderDetail.getDetail_product_num();
				String order_date_barcode = orderDetail.getOrder_date_barcode();
				String detail_product_name = orderDetail.getDetail_product_name();
				String detail_product_spec = orderDetail.getDetail_product_spec();
				String detail_unit = orderDetail.getDetail_unit();
				String detail_price = orderDetail.getDetail_price();
				String count = orderDetail.getCount();
				String without_vat_price = orderDetail.getWithout_vat_price();
				String include_vat_price = orderDetail.getInclude_vat_price();
				String health_donation = orderDetail.getHealth_donation();
				String vat_type = orderDetail.getVat_type();
				String bonus_points_discount_amount = orderDetail.getBonus_points_discount_amount();
				String details_remark = orderDetail.getDetails_remark();

				stringBuffer.append(detail_code).append("|");
				stringBuffer.append(detail_serial_num).append("|");
				stringBuffer.append(detail_order_num).append("|");
				stringBuffer.append(detail_product_num).append("|");
				stringBuffer.append(order_date_barcode).append("|");
				stringBuffer.append(detail_product_name).append("|");
				stringBuffer.append(detail_product_spec).append("|");
				stringBuffer.append(detail_unit).append("|");
				stringBuffer.append(detail_price).append("|");
				stringBuffer.append(count).append("|");
				stringBuffer.append(without_vat_price).append("|");
				stringBuffer.append(include_vat_price).append("|");
				stringBuffer.append(health_donation).append("|");
				stringBuffer.append(vat_type).append("|");
				stringBuffer.append(bonus_points_discount_amount).append("|");
				stringBuffer.append(details_remark).append("|");
				stringBuffer.append("\\n");
			}

		}

		Path path = Paths.get("D:\\xxxxxxxxxxxxxxxxxxxx.txt");
		try (BufferedWriter writer = Files.newBufferedWriter(path)) {
			writer.write(stringBuffer.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void ParseFileToJson(String path) {

		BankPro bankPro = new BankPro();

		List<OrderMaster> orderMasters = new ArrayList<OrderMaster>();

		List<OrderDetail> orderDetails = new ArrayList<OrderDetail>();

		StringBuffer stringBuffer = new StringBuffer();
		try (Stream<String> stream = Files.lines(Paths.get(path))) {
			stream.map(String::trim).forEach(i -> {
				stringBuffer.append(i).append("\n");
			});

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		String teString = stringBuffer.toString();
		String[] lines = teString.split("\\n", -1);

		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			logger.debug("line: {}", line);

			String[] arr = line.split("\\|", -1);

			OrderMaster orderMaster = new OrderMaster();
			OrderDetail orderDetail = null;
			stringBuffer.append(i).append("\n");

			if ("M".equals(arr[0])) {
				if (orderDetails != null && orderDetails.size() > 0) {
					orderDetails = new ArrayList<OrderDetail>();

				}
				orderMaster = new OrderMaster();

				logger.debug("order_code: {}", (arr[0]));
				logger.debug("order_num: {}", (arr[1]));
				logger.debug("order_status: {}", (arr[2]));
				logger.debug("order_date: {}", (arr[3]));
				logger.debug("order_ship_date: {}", (arr[4]));
				logger.debug("order_vat_type: {}", (arr[5]));
				logger.debug("order_without_vat_price: {}", (arr[6]));
				logger.debug("order_vat_price: {}", (arr[7]));
				logger.debug("order_include_vat_price: {}", (arr[8]));
				logger.debug("buyer_ein_code: {}", (arr[9]));
				logger.debug("seller_plant_code: {}", (arr[10]));
				logger.debug("buyer_ein_code: {}", (arr[11]));
				logger.debug("buyer_company_name: {}", (arr[12]));
				logger.debug("member_id: {}", (arr[13]));
				logger.debug("member_name: {}", (arr[14]));
				logger.debug("member_postal_code: {}", (arr[15]));
				logger.debug("member_address: {}", (arr[16]));
				logger.debug("member_phone: {}", (arr[17]));
				logger.debug("member_landline: {}", (arr[18]));
				logger.debug("member_mail: {}", (arr[19]));
				logger.debug("bonus_points_discount_mount: {}", (arr[20]));
				logger.debug("request_paper_invoice: {}", (arr[21]));
				logger.debug("invoice_donation_note: {}", (arr[22]));
				logger.debug("order_note: {}", (arr[23]));
				logger.debug("payment_method: {}", (arr[24]));
				logger.debug("related_number_1: {}", (arr[25]));
				logger.debug("related_number_2: {}", (arr[26]));
				logger.debug("related_number_3: {}", (arr[27]));
				logger.debug("main_note: {}", (arr[28]));
				logger.debug("product_name: {}", (arr[29]));
				logger.debug("carrier_category_number: {}", (arr[30]));
				logger.debug("carrier_code_1: {}", (arr[31]));
				logger.debug("carrier_code_2: {}", (arr[32]));
				logger.debug("invoice_number: {}", (arr[33]));
				logger.debug("random_code: {}", (arr[34]));

				orderMaster.setOrder_code(arr[0]);
				orderMaster.setOrder_num(arr[1]);
				orderMaster.setOrder_status(arr[2]);
				orderMaster.setOrder_date(arr[3]);
				orderMaster.setOrder_ship_date(arr[4]);
				orderMaster.setOrder_vat_type(arr[5]);
				orderMaster.setOrder_without_vat_price(arr[6]);
				orderMaster.setOrder_vat_price(arr[7]);
				orderMaster.setOrder_include_vat_price(arr[8]);
				orderMaster.setBuyer_ein_code(arr[9]);
				orderMaster.setSeller_plant_code(arr[10]);
				orderMaster.setBuyer_ein_code(arr[11]);
				orderMaster.setBuyer_company_name(arr[12]);
				orderMaster.setMember_id(arr[13]);
				orderMaster.setMember_name(arr[14]);
				orderMaster.setMember_postal_code(arr[15]);
				orderMaster.setMember_address(arr[16]);
				orderMaster.setMember_phone(arr[17]);
				orderMaster.setMember_landline(arr[18]);
				orderMaster.setMember_mail(arr[19]);
				orderMaster.setBonus_points_discount_mount(arr[20]);
				orderMaster.setRequest_paper_invoice(arr[21]);
				orderMaster.setInvoice_donation_note(arr[22]);
				orderMaster.setOrder_note(arr[23]);
				orderMaster.setPayment_method(arr[24]);
				orderMaster.setRelated_number_1(arr[25]);
				orderMaster.setRelated_number_2(arr[26]);
				orderMaster.setRelated_number_3(arr[27]);
				orderMaster.setMain_note(arr[28]);
				orderMaster.setProduct_name(arr[29]);
				orderMaster.setCarrier_category_number(arr[30]);
				orderMaster.setCarrier_code_1(arr[31]);
				orderMaster.setCarrier_code_2(arr[32]);
				orderMaster.setInvoice_number(arr[33]);
				orderMaster.setRandom_code(arr[34]);
				orderMaster.setOrderDetails(orderDetails);

				orderMasters.add(orderMaster);
			}

			if ("D".equals(arr[0])) {

				logger.debug("detail_code: {}", (arr[0]));
				logger.debug("detail_serial_num: {}", (arr[1]));
				logger.debug("detail_order_num: {}", (arr[2]));
				logger.debug("detail_product_num: {}", (arr[3]));
				logger.debug("order_date_barcode: {}", (arr[4]));
				logger.debug("detail_product_name: {}", (arr[5]));
				logger.debug("detail_product_spec: {}", (arr[6]));
				logger.debug("detail_unit: {}", (arr[7]));
				logger.debug("detail_price: {}", (arr[8]));
				logger.debug("count: {}", (arr[9]));
				logger.debug("without_vat_price: {}", (arr[10]));
				logger.debug("include_vat_price: {}", (arr[11]));
				logger.debug("health_donation: {}", (arr[12]));
				logger.debug("vat_type: {}", (arr[13]));
				logger.debug("bonus_points_discount_amount: {}", (arr[14]));
				logger.debug("details_remark: {}", (arr[15]));

				orderDetail = new OrderDetail();
				orderDetail.setDetail_code(arr[0]);
				orderDetail.setDetail_serial_num(arr[1]);
				orderDetail.setDetail_order_num(arr[2]);
				orderDetail.setDetail_product_num(arr[3]);
				orderDetail.setOrder_date_barcode(arr[4]);
				orderDetail.setDetail_product_name(arr[5]);
				orderDetail.setDetail_product_spec(arr[6]);
				orderDetail.setDetail_unit(arr[7]);
				orderDetail.setDetail_price(arr[8]);
				orderDetail.setCount(arr[9]);
				orderDetail.setWithout_vat_price(arr[10]);
				orderDetail.setInclude_vat_price(arr[11]);
				orderDetail.setHealth_donation(arr[12]);
				orderDetail.setVat_type(arr[13]);
				orderDetail.setBonus_points_discount_amount(arr[14]);
				orderDetail.setDetails_remark(arr[15]);

				orderDetails.add(orderDetail);

			}
		}

		bankPro.setOrderMasters(orderMasters);
		logger.debug(new Gson().toJson(bankPro));
	}

	public static String jsonToBankPro(String json) {
		Gson gson = new Gson();
		Type type = new TypeToken<BankPro>() {
		}.getType();
		BankPro bankPro = gson.fromJson(json, type);
		List<OrderMaster> orderMasters = bankPro.getOrderMasters();

		StringBuffer stringBuffer = new StringBuffer();

		for (int i = 0; i < orderMasters.size(); i++) {
			OrderMaster orderMaster = orderMasters.get(i);

			String order_code = orderMaster.getOrder_code();
			String order_num = orderMaster.getOrder_num();
			String order_status = orderMaster.getOrder_status();
			String order_date = orderMaster.getOrder_date();
			String order_ship_date = orderMaster.getOrder_ship_date();
			String order_vat_type = orderMaster.getOrder_vat_type();
			String order_without_vat_price = orderMaster.getOrder_without_vat_price();
			String order_vat_price = orderMaster.getOrder_vat_price();
			String order_include_vat_price = orderMaster.getOrder_include_vat_price();
			String seller_ein_code = orderMaster.getBuyer_ein_code();
			String seller_plant_code = orderMaster.getSeller_plant_code();
			String buyer_ein_code = orderMaster.getBuyer_ein_code();
			String buyer_company_name = orderMaster.getBuyer_company_name();
			String member_id = orderMaster.getMember_id();
			String member_name = orderMaster.getMember_name();
			String member_postal_code = orderMaster.getMember_postal_code();
			String member_address = orderMaster.getMember_address();
			String member_phone = orderMaster.getMember_phone();
			String member_landline = orderMaster.getMember_landline();
			String member_mail = orderMaster.getMember_mail();
			String bonus_points_discount_mount = orderMaster.getBonus_points_discount_mount();
			String request_paper_invoice = orderMaster.getRequest_paper_invoice();
			String invoice_donation_note = orderMaster.getInvoice_donation_note();
			String order_note = orderMaster.getOrder_note();
			String payment_method = orderMaster.getPayment_method();
			String related_number_1 = orderMaster.getRelated_number_1();
			String related_number_2 = orderMaster.getRelated_number_2();
			String related_number_3 = orderMaster.getRelated_number_3();
			String main_note = orderMaster.getMain_note();
			String product_name = orderMaster.getProduct_name();
			String carrier_category_number = orderMaster.getCarrier_category_number();
			String carrier_code_1 = orderMaster.getCarrier_code_1();
			String carrier_code_2 = orderMaster.getCarrier_code_2();
			String invoice_number = orderMaster.getInvoice_number();
			String random_code = orderMaster.getRandom_code();

			stringBuffer.append(order_code).append("|");
			stringBuffer.append(order_num).append("|");
			stringBuffer.append(order_status).append("|");
			stringBuffer.append(order_date).append("|");
			stringBuffer.append(order_ship_date).append("|");
			stringBuffer.append(order_vat_type).append("|");
			stringBuffer.append(order_without_vat_price).append("|");
			stringBuffer.append(order_vat_price).append("|");
			stringBuffer.append(order_include_vat_price).append("|");
			stringBuffer.append(seller_ein_code).append("|");
			stringBuffer.append(seller_plant_code).append("|");
			stringBuffer.append(buyer_ein_code).append("|");
			stringBuffer.append(buyer_company_name).append("|");
			stringBuffer.append(member_id).append("|");
			stringBuffer.append(member_name).append("|");
			stringBuffer.append(member_postal_code).append("|");
			stringBuffer.append(member_address).append("|");
			stringBuffer.append(member_phone).append("|");
			stringBuffer.append(member_landline).append("|");
			stringBuffer.append(member_mail).append("|");
			stringBuffer.append(bonus_points_discount_mount).append("|");
			stringBuffer.append(request_paper_invoice).append("|");
			stringBuffer.append(invoice_donation_note).append("|");
			stringBuffer.append(order_note).append("|");
			stringBuffer.append(payment_method).append("|");
			stringBuffer.append(related_number_1).append("|");
			stringBuffer.append(related_number_2).append("|");
			stringBuffer.append(related_number_3).append("|");
			stringBuffer.append(main_note).append("|");
			stringBuffer.append(product_name).append("|");
			stringBuffer.append(carrier_category_number).append("|");
			stringBuffer.append(carrier_code_1).append("|");
			stringBuffer.append(carrier_code_2).append("|");
			stringBuffer.append(invoice_number).append("|");
			stringBuffer.append(random_code).append("|");
			stringBuffer.append('\n');

			List<OrderDetail> orderDetails = orderMaster.getOrderDetails();

			for (int j = 0; j < orderDetails.size(); j++) {
				OrderDetail orderDetail = orderDetails.get(j);
				String detail_code = orderDetail.getDetail_code();
				String detail_serial_num = orderDetail.getDetail_serial_num();
				String detail_order_num = orderDetail.getDetail_order_num();
				String detail_product_num = orderDetail.getDetail_product_num();
				String order_date_barcode = orderDetail.getOrder_date_barcode();
				String detail_product_name = orderDetail.getDetail_product_name();
				String detail_product_spec = orderDetail.getDetail_product_spec();
				String detail_unit = orderDetail.getDetail_unit();
				String detail_price = orderDetail.getDetail_price();
				String count = orderDetail.getCount();
				String without_vat_price = orderDetail.getWithout_vat_price();
				String include_vat_price = orderDetail.getInclude_vat_price();
				String health_donation = orderDetail.getHealth_donation();
				String vat_type = orderDetail.getVat_type();
				String bonus_points_discount_amount = orderDetail.getBonus_points_discount_amount();
				String details_remark = orderDetail.getDetails_remark();

				stringBuffer.append(detail_code).append("|");
				stringBuffer.append(detail_serial_num).append("|");
				stringBuffer.append(detail_order_num).append("|");
				stringBuffer.append(detail_product_num).append("|");
				stringBuffer.append(order_date_barcode).append("|");
				stringBuffer.append(detail_product_name).append("|");
				stringBuffer.append(detail_product_spec).append("|");
				stringBuffer.append(detail_unit).append("|");
				stringBuffer.append(detail_price).append("|");
				stringBuffer.append(count).append("|");
				stringBuffer.append(without_vat_price).append("|");
				stringBuffer.append(include_vat_price).append("|");
				stringBuffer.append(health_donation).append("|");
				stringBuffer.append(vat_type).append("|");
				stringBuffer.append(bonus_points_discount_amount).append("|");
				stringBuffer.append(details_remark).append("|");
				stringBuffer.append('\n');
			}
		}
		if (orderMasters.size() > 0) {
			stringBuffer.append(orderMasters.size());
		}
		return stringBuffer.toString();
	}

	public static boolean hasOrderDetails(String json) {
		boolean isHas = false;
		try {
			Gson gson = new Gson();
			Type type = new TypeToken<BankPro>() {
			}.getType();
			BankPro bankPro = gson.fromJson(json, type);
			List<OrderMaster> orderMasters = bankPro.getOrderMasters();

			for (int i = 0; i < orderMasters.size(); i++) {
				OrderMaster orderMaster = orderMasters.get(i);

				List<OrderDetail> orderDetails = orderMaster.getOrderDetails();
				if (orderDetails.size() > 0)
					isHas = true;
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
			return isHas;
		}
		return isHas;
	}

	public static void main(String[] args) throws Exception {
		 String configPath = args[0];
		 String heartBeatXmlFilePath = args[1];
//		String configPath = "resources\\q2ftp-config.xml";
//		String heartBeatXmlFilePath = "resources\\q2ftp-heatBeatClinetBeans.xml";

//		new Q2FTP(configPath, heartBeatXmlFilePath).ParseFileToJson("resources\\12656354-O-20161104.txt");
		 new Q2FTP(configPath, heartBeatXmlFilePath).start();

	}
}
