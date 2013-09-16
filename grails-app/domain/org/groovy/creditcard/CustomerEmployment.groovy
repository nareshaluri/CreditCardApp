package org.groovy.creditcard

class CustomerEmployment {
	
	int id
	int customer_id
	long annualIncome
	long otherAnnualIncome
	String residentialStatus
	int monthlyMortgageRent
	String workPhone
	int score
    static mapping = {
		table 'customer_employment_history'
		version false
		id generator:'increment', column:'id'
		annualIncome column:'annualIncome'
		otherAnnualIncome column:'otherAnnualIncome'
		residentialStatus column:'residentialStatus'
		monthlyMortgageRent column:'monthlyMortgageRent'
		workPhone column:'workPhone'
		score column:'score'
    }
	
	static constraints = {
		annualIncome(blank:false)
		customer_id(display:false)
		residentialStatus inList:['Rent','Own'],blank:false
		monthlyMortgageRent nullable:true
	}
}
