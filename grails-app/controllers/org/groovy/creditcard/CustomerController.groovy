package org.groovy.creditcard

import org.java.creditcard.DroolsClass
import org.springframework.dao.DataIntegrityViolationException
import grails.converters.*
import org.codehaus.groovy.grails.web.json.*;
import groovy.sql.*;

class CustomerController {

	static allowedMethods = [save: "POST", update: "POST", delete: "POST"]
	
	static scaffolding = true

	def index() {
		redirect(action: "list", params: params)
	}

	def list(Integer max) {
		params.max = Math.min(max ?: 10, 100)
		[customerInstanceList: Customer.list(params), customerInstanceTotal: Customer.count()]
	}

	def create() {
		print 'params list..'
		[customerInstance: new Customer(params)]
	}

	def save() {
		def customerInstance = new Customer(params)
		System.out.println(params.customerInstance);
		customerInstance['username'] = session.username
		getCreditRatingWS(customerInstance, customerInstance.getSsn())
		if (!customerInstance.save(flush: true)) {
			render(view: "create", model: [customerInstance: customerInstance])
			return
		}
		flash.message = message(code: 'default.created.message', args: [message(code: 'customer.label', default: 'Customer'), customerInstance.id])
		redirect(action: "create", controller:"CustomerBank", id: customerInstance.id)
		
	}
	
	def getCreditRatingWS(Customer apprec, inputSSN){
		withRest(uri: "http://192.168.1.24:8080") {
			   def html = get(path : '/SSNWS-0.1/getSSNRating/'+inputSSN)
			   def responseMap = JSON.parse(html.getData().toString());
			   apprec.creditRating = responseMap["creditRating"]
			   apprec.crime = responseMap["crime"]
			   apprec.dl = responseMap["dl"]
			   System.out.println(responseMap["creditRating"]);
		}
	}

	def show(Long id) {
		println(id)
		def customerInstance = Customer.get(id)
		if (!customerInstance) {
			flash.message = message(code: 'default.not.found.message', args: [message(code: 'customer.label', default: 'Customer'), id])
			redirect(action: "list")
			return
		}

		[customerInstance: customerInstance]
	}

	def edit(Long id) {
		def customerInstance = Customer.get(id)
		if (!customerInstance) {
			flash.message = message(code: 'default.not.found.message', args: [message(code: 'customer.label', default: 'Customer'), id])
			redirect(action: "list")
			return
		}

		[customerInstance: customerInstance]
	}

	def update(Long id, Long version) {
		def customerInstance = Customer.get(id)
		if (!customerInstance) {
			flash.message = message(code: 'default.not.found.message', args: [message(code: 'customer.label', default: 'Customer'), id])
			redirect(action: "list")
			return
		}

		if (version != null) {
			if (customerInstance.version > version) {
				customerInstance.errors.rejectValue("version", "default.optimistic.locking.failure",
						  [message(code: 'customer.label', default: 'Customer')] as Object[],
						  "Another user has updated this Customer while you were editing")
				render(view: "edit", model: [customerInstance: customerInstance])
				return
			}
		}

		customerInstance.properties = params

		if (!customerInstance.save(flush: true)) {
			render(view: "edit", model: [customerInstance: customerInstance])
			return
		}

		flash.message = message(code: 'default.updated.message', args: [message(code: 'customer.label', default: 'Customer'), customerInstance.id])
		redirect(action: "show", id: customerInstance.id)
	}

	def delete(Long id) {
		def customerInstance = Customer.get(id)
		if (!customerInstance) {
			flash.message = message(code: 'default.not.found.message', args: [message(code: 'customer.label', default: 'Customer'), id])
			redirect(action: "list")
			return
		}

		try {
			customerInstance.delete(flush: true)
			flash.message = message(code: 'default.deleted.message', args: [message(code: 'customer.label', default: 'Customer'), id])
			redirect(action: "list")
		}
		catch (DataIntegrityViolationException e) {
			flash.message = message(code: 'default.not.deleted.message', args: [message(code: 'customer.label', default: 'Customer'), id])
			redirect(action: "show", id: id)
		}
	}
	
	def apply = {
		DroolsClass d1 = new DroolsClass();
		System.out.println("Entering Drools Logic" +params)
		def appID = params.int('id')
		
		//def creditRating = getCreditRatingWS(params["ssn"])
		System.out.println("Submitting - "+appID)
		def appRecord = Customer.get(appID)
		
		def empMap = [:]
		empMap.put("customer_id",appID)
		def appEmpRecord = CustomerEmployment.findWhere(empMap)
		System.out.println("comments - "+appRecord.getComments())
		if(appRecord.getComments()==null){
			System.out.println(appRecord.ssn);
			
			d1.droolsInit(appRecord)
			d1.droolsInitEmp(appEmpRecord)
			
			
			def totScore = appRecord.getScore() + appEmpRecord.getScore();
			def resultRecord = new Results();
			resultRecord.setApplicationID(appID);
			resultRecord.setApproverID(111);
			
			resultRecord.setCustomerID(appID);
			
			resultRecord.setManualcomments("Processed by system");
			resultRecord.setManualOverRide("N");
			Date d34 = new Date();
			Date d35 = new Date(2014,12,12);
			Date d36 = new Date(2016,12,12);
			System.out.println("totScore-"+totScore)
			if(totScore < 5){
				resultRecord.setCreditLimit(0);
				resultRecord.setIsAccept("N");
				resultRecord.setCardTypeID(0);
				resultRecord.setValidity(d34);
				resultRecord.setSystemcomments(" Application Rejected ")
			}
			else if(totScore > 5 && totScore < 100){
				resultRecord.setCreditLimit(1000);
				resultRecord.setIsAccept("Y");
				resultRecord.setCardTypeID(1);
				resultRecord.setValidity(d35);
				resultRecord.setSystemcomments(" Application Approved ")
			}
			else if(totScore > 100){
				resultRecord.setCreditLimit(2000);
				resultRecord.setIsAccept("Y");
				resultRecord.setCardTypeID(3);
				resultRecord.setValidity(d36);
				resultRecord.setSystemcomments(" Application Approved ")
			}
			
			
			
			def newCard = new CustomerCard();
			Random random = new Random();
			newCard.setApproverID(111);
			newCard.setCardNumber(random.nextInt(100));
			newCard.setCustomer_id(appID);
			newCard.setIsActive(1);
			System.out.println("resultRecord.getCreditLimit()::"+resultRecord.getCreditLimit())
			newCard.setCreditLimit(resultRecord.getCreditLimit())
			
			System.out.println(newCard.getProperties().toMapString())
			
			resultRecord.save()
			newCard.save()
			
//			def sql = Sql.newInstance("jdbc:mysql://localhost:3306/creditcard","root", "root", "com.mysql.jdbc.Driver")
//			
//			
//			
//			def params = [newCard.getApproverID(),newCard.getCardNumber(),newCard.getCustomer_id(),newCard.getIsActive(),newCard.getLimit()]
//			sql.execute 'insert into customer_card (approverID,cardNumber, customer_id,isActive,limit ) values (?, ?, ?, ?, ?)', params
			
			appRecord.setComments("Processed")
			
			//def cc = new CustomerCard();
			//cc.setCustomer_id(appID);
			//cc.setCardNumber()
		}
		else{
			flash.message = "Cannot submit again"
		}
		
		redirect(uri:'/customer/show/'+appID)
	}
	
	def customerhome = {
		if(session.usertype == 'banker')
		{
			//def cb = Results.countByCardTypeID()
			def criteria = Results.createCriteria()
			def criteria1 = Results.createCriteria()
			def criteria2 = Results.createCriteria()
			def criteria3 = Results.createCriteria()
			
			def distinctCardType = criteria.list {
				projections {
					distinct 'cardTypeID'
					cache(true)
				}
			}
			
			
			 def distinctCardAcceptance = criteria1.list{
				projections {
						distinct 'isAccept'
						cache(true)
					}
			}
			
			def distinctApprover = criteria2.list{
				projections {
						distinct 'approverID'
						cache(true)
					}
			}
			
			def distinctOverrider = criteria3.list{
				projections {
						distinct 'manualOverRide'
						cache(true)
					}
			}
			//System.out.println("countByOverrider : "+countByCardType)
			def countByCardType = [:]
			for(def item in distinctCardType)
			{
				def cb = Results.countByCardTypeID(item,[cache: true])
				countByCardType.put(item,cb)
			}
			
			def countCardAcceptance = [:]
			for(def item in distinctCardAcceptance)
			{
				def cb = Results.countByIsAccept(item,[cache: true])
				countCardAcceptance.put(item,cb)
			}
			
			def countByApproverID = [:]
			for(def item in distinctApprover)
			{
				def cb = Results.countByApproverID(item,[cache: true])
				countByApproverID.put(item,cb)
			}
			
			def countByOverride = [:]
			for(def item in distinctOverrider)
			{
				def cb = Results.countByManualOverRide(item,[cache: true])
				countByOverride.put(item,cb)
			}
			
			System.out.println("countByApproverID:"+countByApproverID);
			System.out.println("countByCardType:"+countByCardType);
			System.out.println("countByOverride:"+countByOverride);
			System.out.println("countCardAcceptance:"+countCardAcceptance);
			
			["countByApproverID":countByApproverID,"countByCardType":countByCardType,"countByOverride":countByOverride,"countCardAcceptance":countCardAcceptance]
			
		}
		else
		{
			def cardlist = []
			def m = [:]
			m.put("username",session.username)
			def cust = Customer.findWhere(m)
			System.out.println("fetched cust - "+cust)
			for(Customer c : cust)
			{
				def tempMap = [:]
				tempMap.put("customer_id", c.id)
				def cc = CustomerCard.findWhere(tempMap)
				System.out.println("fetched cc - "+cc)
				cardlist.addAll(cc?.list())
			}
			[cardlist : cardlist]
		}
		
	}
	
	def logout = {
		redirect(action:'logout', controller:'Login')
	}
}
