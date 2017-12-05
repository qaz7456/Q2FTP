package tw.com.q2ftp.request.mode;

import java.util.List;

public class OrderMaster {
	private String order_code;
	private String order_num;
	private String order_status;
	private String order_date;
	private String order_ship_date;
	private String order_vat_type;
	private String order_without_vat_price;
	private String order_vat_price;
	private String order_include_vat_price;
	private String seller_ein_code;
	private String seller_plant_code;
	private String buyer_ein_code;
	private String buyer_company_name;
	private String member_id;
	private String member_name;
	private String member_postal_code;
	private String member_address;
	private String member_phone;
	private String member_landline;
	private String member_mail;
	private String Bonus_points_discount_mount;
	private String request_paper_invoice;
	private String invoice_donation_note;
	private String order_note;
	private String payment_method;
	private String related_number_1;
	private String related_number_2;
	private String related_number_3;
	private String main_note;
	private String product_name;
	private String carrier_category_number;
	private String carrier_code_1;
	private String carrier_code_2;
	private String invoice_number;
	private String random_code;

	private List<OrderDetail> orderDetails;

	public List<OrderDetail> getOrderDetails() {
		return orderDetails;
	}

	public void setOrderDetails(List<OrderDetail> orderDetails) {
		this.orderDetails = orderDetails;
	}

	public String getOrder_code() {
		return order_code;
	}

	public void setOrder_code(String order_code) {
		this.order_code = order_code;
	}

	public String getOrder_num() {
		return order_num;
	}

	public void setOrder_num(String order_num) {
		this.order_num = order_num;
	}

	public String getOrder_status() {
		return order_status;
	}

	public void setOrder_status(String order_status) {
		this.order_status = order_status;
	}

	public String getOrder_date() {
		return order_date;
	}

	public void setOrder_date(String order_date) {
		this.order_date = order_date;
	}

	public String getOrder_ship_date() {
		return order_ship_date;
	}

	public void setOrder_ship_date(String order_ship_date) {
		this.order_ship_date = order_ship_date;
	}

	public String getOrder_vat_type() {
		return order_vat_type;
	}

	public void setOrder_vat_type(String order_vat_type) {
		this.order_vat_type = order_vat_type;
	}

	public String getOrder_without_vat_price() {
		return order_without_vat_price;
	}

	public void setOrder_without_vat_price(String order_without_vat_price) {
		this.order_without_vat_price = order_without_vat_price;
	}

	public String getOrder_vat_price() {
		return order_vat_price;
	}

	public void setOrder_vat_price(String order_vat_price) {
		this.order_vat_price = order_vat_price;
	}

	public String getOrder_include_vat_price() {
		return order_include_vat_price;
	}

	public void setOrder_include_vat_price(String order_include_vat_price) {
		this.order_include_vat_price = order_include_vat_price;
	}

	public String getSeller_ein_code() {
		return seller_ein_code;
	}

	public void setSeller_ein_code(String seller_ein_code) {
		this.seller_ein_code = seller_ein_code;
	}

	public String getSeller_plant_code() {
		return seller_plant_code;
	}

	public void setSeller_plant_code(String seller_plant_code) {
		this.seller_plant_code = seller_plant_code;
	}

	public String getBuyer_ein_code() {
		return buyer_ein_code;
	}

	public void setBuyer_ein_code(String buyer_ein_code) {
		this.buyer_ein_code = buyer_ein_code;
	}

	public String getBuyer_company_name() {
		return buyer_company_name;
	}

	public void setBuyer_company_name(String buyer_company_name) {
		this.buyer_company_name = buyer_company_name;
	}

	public String getMember_id() {
		return member_id;
	}

	public void setMember_id(String member_id) {
		this.member_id = member_id;
	}

	public String getMember_name() {
		return member_name;
	}

	public void setMember_name(String member_name) {
		this.member_name = member_name;
	}

	public String getMember_postal_code() {
		return member_postal_code;
	}

	public void setMember_postal_code(String member_postal_code) {
		this.member_postal_code = member_postal_code;
	}

	public String getMember_address() {
		return member_address;
	}

	public void setMember_address(String member_address) {
		this.member_address = member_address;
	}

	public String getMember_phone() {
		return member_phone;
	}

	public void setMember_phone(String member_phone) {
		this.member_phone = member_phone;
	}

	public String getMember_landline() {
		return member_landline;
	}

	public void setMember_landline(String member_landline) {
		this.member_landline = member_landline;
	}

	public String getMember_mail() {
		return member_mail;
	}

	public void setMember_mail(String member_mail) {
		this.member_mail = member_mail;
	}

	public String getBonus_points_discount_mount() {
		return Bonus_points_discount_mount;
	}

	public void setBonus_points_discount_mount(String bonus_points_discount_mount) {
		Bonus_points_discount_mount = bonus_points_discount_mount;
	}

	public String getRequest_paper_invoice() {
		return request_paper_invoice;
	}

	public void setRequest_paper_invoice(String request_paper_invoice) {
		this.request_paper_invoice = request_paper_invoice;
	}

	public String getInvoice_donation_note() {
		return invoice_donation_note;
	}

	public void setInvoice_donation_note(String invoice_donation_note) {
		this.invoice_donation_note = invoice_donation_note;
	}

	public String getOrder_note() {
		return order_note;
	}

	public void setOrder_note(String order_note) {
		this.order_note = order_note;
	}

	public String getPayment_method() {
		return payment_method;
	}

	public void setPayment_method(String payment_method) {
		this.payment_method = payment_method;
	}

	public String getRelated_number_1() {
		return related_number_1;
	}

	public void setRelated_number_1(String related_number_1) {
		this.related_number_1 = related_number_1;
	}

	public String getRelated_number_2() {
		return related_number_2;
	}

	public void setRelated_number_2(String related_number_2) {
		this.related_number_2 = related_number_2;
	}

	public String getRelated_number_3() {
		return related_number_3;
	}

	public void setRelated_number_3(String related_number_3) {
		this.related_number_3 = related_number_3;
	}

	public String getMain_note() {
		return main_note;
	}

	public void setMain_note(String main_note) {
		this.main_note = main_note;
	}

	public String getProduct_name() {
		return product_name;
	}

	public void setProduct_name(String product_name) {
		this.product_name = product_name;
	}

	public String getCarrier_category_number() {
		return carrier_category_number;
	}

	public void setCarrier_category_number(String carrier_category_number) {
		this.carrier_category_number = carrier_category_number;
	}

	public String getCarrier_code_1() {
		return carrier_code_1;
	}

	public void setCarrier_code_1(String carrier_code_1) {
		this.carrier_code_1 = carrier_code_1;
	}

	public String getCarrier_code_2() {
		return carrier_code_2;
	}

	public void setCarrier_code_2(String carrier_code_2) {
		this.carrier_code_2 = carrier_code_2;
	}

	public String getInvoice_number() {
		return invoice_number;
	}

	public void setInvoice_number(String invoice_number) {
		this.invoice_number = invoice_number;
	}

	public String getRandom_code() {
		return random_code;
	}

	public void setRandom_code(String random_code) {
		this.random_code = random_code;
	}

}
